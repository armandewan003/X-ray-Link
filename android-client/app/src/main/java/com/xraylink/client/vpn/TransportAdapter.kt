package com.xraylink.client.vpn

import android.content.Context
import android.net.VpnService
import com.wireguard.android.backend.GoBackend
import com.wireguard.android.backend.Tunnel
import com.wireguard.config.Config
import com.wireguard.config.InetEndpoint
import com.wireguard.config.InetNetwork
import com.wireguard.config.Interface
import com.wireguard.config.Peer
import com.wireguard.crypto.KeyPair
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.InetAddress

enum class VpnConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    RECONNECTING,
    FAILED,
    EXPIRED
}

data class TunnelProvisioningConfig(
    val sessionId: String = "",
    val transport: String = "WIREGUARD",
    val serverEndpoint: String = "",
    val serverPort: Int = 51820,
    val serverPublicKey: String = "",
    val clientTunnelIp: String = "",
    val dnsServers: List<String> = emptyList(),
    val allowedIps: List<String> = emptyList(),
    val authorizedHostname: String = "",
    val sniHost: String = "",
    val wsPath: String = "",
    val wsHeaders: Map<String, String> = emptyMap()
)

data class TunnelTrafficSnapshot(
    val rxBytes: Long,
    val txBytes: Long
)

interface TunnelTransportAdapter {
    val protocolName: String
    suspend fun establish(
        context: Context,
        vpnService: VpnService,
        config: TunnelProvisioningConfig,
        clientKeyPair: KeyPair
    ): Result<Unit>

    suspend fun teardown(): Result<Unit>
    suspend fun readStats(): TunnelTrafficSnapshot
}

class WireGuardTunnelAdapter : TunnelTransportAdapter {
    override val protocolName: String = "WIREGUARD"

    private var backend: GoBackend? = null
    private var activeTunnel: Tunnel? = null

    override suspend fun establish(
        context: Context,
        vpnService: VpnService,
        config: TunnelProvisioningConfig,
        clientKeyPair: KeyPair
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            require(config.serverEndpoint.isNotBlank() && config.serverPort > 0) {
                "Server endpoint or port missing from provisioning payload"
            }
            require(config.serverPublicKey.isNotBlank()) {
                "Server WireGuard public key is missing"
            }
            require(config.clientTunnelIp.isNotBlank()) {
                "Client tunnel IP is missing"
            }

            val goBackend = backend ?: GoBackend(context.applicationContext).also { backend = it }

            val ifaceBuilder = Interface.Builder()
                .parsePrivateKey(clientKeyPair.privateKey.toBase64())
                .addAddress(InetNetwork.parse(config.clientTunnelIp))
                // Exclude the client app itself so control-plane heartbeats & WebSocket sync bypass the tunnel
                .excludeApplication(context.packageName)

            config.dnsServers.filter { it.isNotBlank() }.forEach { dns ->
                ifaceBuilder.addDnsServer(InetAddress.getByName(dns.trim()))
            }

            val peerBuilder = Peer.Builder()
                .parsePublicKey(config.serverPublicKey.trim())
                .setEndpoint(InetEndpoint.parse("${config.serverEndpoint.trim()}:${config.serverPort}"))
                .setPersistentKeepalive(25)

            val routes = config.allowedIps.filter { it.isNotBlank() }.ifEmpty { listOf("0.0.0.0/0") }
            routes.forEach { cidr ->
                peerBuilder.addAllowedIp(InetNetwork.parse(cidr.trim()))
            }

            val wgConfig = Config.Builder()
                .setInterface(ifaceBuilder.build())
                .addPeer(peerBuilder.build())
                .build()

            val tunnel = object : Tunnel {
                override fun getName(): String = "xraylink-wg0"
                override fun onStateChange(newState: Tunnel.State) {
                    // Propagated by XRayVpnService state observer
                }
            }
            activeTunnel = tunnel

            val resultingState = goBackend.setState(tunnel, Tunnel.State.UP, wgConfig)
            if (resultingState != Tunnel.State.UP) {
                error("WireGuard tunnel failed to reach UP state: $resultingState")
            }
        }
    }

    override suspend fun teardown(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val tunnel = activeTunnel
            val goBackend = backend
            if (tunnel != null && goBackend != null) {
                goBackend.setState(tunnel, Tunnel.State.DOWN, null)
            }
            activeTunnel = null
        }
    }

    override suspend fun readStats(): TunnelTrafficSnapshot = withContext(Dispatchers.IO) {
        val tunnel = activeTunnel ?: return@withContext TunnelTrafficSnapshot(0L, 0L)
        val goBackend = backend ?: return@withContext TunnelTrafficSnapshot(0L, 0L)
        val stats = goBackend.getStatistics(tunnel)
        TunnelTrafficSnapshot(rxBytes = stats.totalRx(), txBytes = stats.totalTx())
    }
}

class ModularProxyTunnelAdapter(
    override val protocolName: String
) : TunnelTransportAdapter {

    override suspend fun establish(
        context: Context,
        vpnService: VpnService,
        config: TunnelProvisioningConfig,
        clientKeyPair: KeyPair
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val nativeDir = File(context.applicationInfo.nativeLibraryDir)
            val hasXrayCore = File(nativeDir, "libxray.so").exists() || File(nativeDir, "libtun2socks.so").exists()
            if (!hasXrayCore) {
                error(
                    "$protocolName requires native xray-core/tun2socks binaries (libxray.so) in app/src/main/jniLibs. " +
                        "Select a WIREGUARD server & profile for GoBackend kernel/userspace tunneling."
                )
            }
            error("$protocolName native runner invocation is not configured for ${config.serverEndpoint}:${config.serverPort}")
        }
    }

    override suspend fun teardown(): Result<Unit> = Result.success(Unit)

    override suspend fun readStats(): TunnelTrafficSnapshot = TunnelTrafficSnapshot(0L, 0L)
}
