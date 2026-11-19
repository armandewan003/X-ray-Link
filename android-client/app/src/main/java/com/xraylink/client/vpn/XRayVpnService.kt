package com.xraylink.client.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.VpnService
import android.os.Build
import com.wireguard.crypto.KeyPair
import com.xraylink.client.data.XRayApiService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class XRayVpnService : VpnService() {

    @Inject lateinit var apiService: XRayApiService

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var heartbeatJob: Job? = null
    private var connectJob: Job? = null
    private var activeAdapter: TunnelTransportAdapter? = null
    private var activeSessionId: String? = null
    private var prevRx: Long = 0L
    private var prevTx: Long = 0L

    companion object {
        const val ACTION_CONNECT = "com.xraylink.client.ACTION_CONNECT"
        const val ACTION_DISCONNECT = "com.xraylink.client.ACTION_DISCONNECT"
        private const val CHANNEL_ID = "xraylink_vpn_channel"
        private const val NOTIFICATION_ID = 101

        private val _state = MutableStateFlow(VpnConnectionState.DISCONNECTED)
        val state: StateFlow<VpnConnectionState> = _state

        private val _stats = MutableStateFlow(TunnelTrafficSnapshot(0L, 0L))
        val stats: StateFlow<TunnelTrafficSnapshot> = _stats

        private val _lastError = MutableStateFlow<String?>(null)
        val lastError: StateFlow<String?> = _lastError
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_DISCONNECT -> {
                connectJob?.cancel()
                serviceScope.launch { disconnectTunnel("User requested disconnect") }
            }
            ACTION_CONNECT -> {
                val serverId = intent.getStringExtra("server_id") ?: return START_NOT_STICKY
                val profileId = intent.getStringExtra("profile_id") ?: return START_NOT_STICKY
                startForegroundCompat(buildNotification("Establishing X-RAY LINK tunnel..."))
                if (connectJob?.isActive == true) {
                    return START_NOT_STICKY
                }
                connectJob = serviceScope.launch { connectTunnel(serverId, profileId) }
            }
            else -> {
                stopSelfResult(startId)
            }
        }
        return START_NOT_STICKY
    }

    override fun onRevoke() {
        connectJob?.cancel()
        serviceScope.launch { disconnectTunnel("VPN permission revoked by system") }
        super.onRevoke()
    }

    private fun startForegroundCompat(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun updateNotification(text: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager?.notify(NOTIFICATION_ID, buildNotification(text))
    }

    private suspend fun connectTunnel(serverId: String, profileId: String) {
        _lastError.value = null
        _state.value = VpnConnectionState.CONNECTING
        val keyPair = KeyPair()

        val initResp = runCatching {
            apiService.initiateSession(
                mapOf(
                    "serverId" to serverId,
                    "profileId" to profileId,
                    "clientPublicKey" to keyPair.publicKey.toBase64()
                )
            )
        }.getOrElse { err ->
            _lastError.value = err.message ?: "Failed to initiate VPN session"
            _state.value = VpnConnectionState.FAILED
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return
        }

        val sessionId = initResp.sessionId
        activeSessionId = sessionId
        val cfg = initResp.tunnelProvisioning

        val adapter: TunnelTransportAdapter = when (cfg.transport) {
            "WIREGUARD" -> WireGuardTunnelAdapter()
            else -> ModularProxyTunnelAdapter(cfg.transport)
        }
        activeAdapter = adapter

        val result = adapter.establish(this, this, cfg, keyPair)
        if (result.isFailure) {
            runCatching { apiService.confirmSession(sessionId, mapOf("tunnelEstablished" to false)) }
            _lastError.value = result.exceptionOrNull()?.message ?: "Tunnel establishment failed"
            activeAdapter = null
            activeSessionId = null
            _state.value = VpnConnectionState.FAILED
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return
        }

        // Only mark CONNECTED after real tunnel establishment succeeds
        runCatching { apiService.confirmSession(sessionId, mapOf("tunnelEstablished" to true)) }
        prevRx = 0L
        prevTx = 0L
        _stats.value = TunnelTrafficSnapshot(0L, 0L)
        _state.value = VpnConnectionState.CONNECTED
        updateNotification("Connected · ${cfg.transport} (${cfg.serverEndpoint})")
        startHeartbeatLoop(sessionId, adapter)
    }

    private fun startHeartbeatLoop(sessionId: String, adapter: TunnelTransportAdapter) {
        heartbeatJob?.cancel()
        heartbeatJob = serviceScope.launch {
            while (_state.value == VpnConnectionState.CONNECTED) {
                delay(5000)
                val snap = adapter.readStats()
                _stats.value = snap
                val deltaDown = (snap.rxBytes - prevRx).coerceAtLeast(0L)
                val deltaUp = (snap.txBytes - prevTx).coerceAtLeast(0L)
                prevRx = snap.rxBytes
                prevTx = snap.txBytes

                val hb = runCatching {
                    apiService.sendHeartbeat(
                        sessionId,
                        mapOf(
                            "bytesUpDelta" to deltaUp,
                            "bytesDownDelta" to deltaDown,
                            "uploadSpeedBps" to (deltaUp / 5L),
                            "downloadSpeedBps" to (deltaDown / 5L)
                        )
                    )
                }.getOrNull()

                if (hb?.action == "DISCONNECT") {
                    _lastError.value = "Session disconnected by control plane"
                    disconnectTunnel("Revoked by central control plane")
                    break
                }
            }
        }
    }

    private suspend fun disconnectTunnel(reason: String) {
        heartbeatJob?.cancel()
        activeAdapter?.teardown()
        activeSessionId?.let { id ->
            runCatching { apiService.disconnectSession(id, mapOf("reason" to reason)) }
        }
        activeAdapter = null
        activeSessionId = null
        prevRx = 0L
        prevTx = 0L
        _stats.value = TunnelTrafficSnapshot(0L, 0L)
        _state.value = VpnConnectionState.DISCONNECTED
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun buildNotification(text: String): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(CHANNEL_ID, "X-RAY LINK VPN", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(ch)
        }
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("X-RAY LINK")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        heartbeatJob?.cancel()
        connectJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }
}
