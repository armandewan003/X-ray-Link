package com.xraylink.client.ui

import android.content.Context
import android.os.Build
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xraylink.client.data.AnnouncementDto
import com.xraylink.client.data.EncryptedTokenStore
import com.xraylink.client.data.PinLoginRequest
import com.xraylink.client.data.ProfileDto
import com.xraylink.client.data.ServerDto
import com.xraylink.client.data.SessionRecordDto
import com.xraylink.client.data.WebSocketSyncClient
import com.xraylink.client.data.XRayApiService
import com.xraylink.client.vpn.VpnConnectionState
import com.xraylink.client.vpn.XRayVpnService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ClientUiState(
    val isAuthenticated: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val pinCode: String = "",
    val username: String = "",
    val expiresAt: String = "",
    val deviceLimit: Int = 1,
    val servers: List<ServerDto> = emptyList(),
    val profiles: List<ProfileDto> = emptyList(),
    val selectedServer: ServerDto? = null,
    val selectedProfile: ProfileDto? = null,
    val sessions: List<SessionRecordDto> = emptyList(),
    val announcements: List<AnnouncementDto> = emptyList(),
    val durationSeconds: Long = 0L,
    val autoReconnect: Boolean = true,
    val localKillSwitch: Boolean = true
)

@HiltViewModel
class ClientViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val apiService: XRayApiService,
    private val tokenStore: EncryptedTokenStore,
    private val wsSyncClient: WebSocketSyncClient
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ClientUiState(
            isAuthenticated = !tokenStore.getAccessToken().isNullOrBlank(),
            pinCode = tokenStore.getPinCode().orEmpty()
        )
    )
    val uiState: StateFlow<ClientUiState> = _uiState.asStateFlow()

    val vpnState = XRayVpnService.state
    val vpnStats = XRayVpnService.stats
    val vpnError = XRayVpnService.lastError

    init {
        if (!tokenStore.getAccessToken().isNullOrBlank()) {
            wsSyncClient.connect()
            refreshControlPlane()
        }
        viewModelScope.launch {
            wsSyncClient.events.collect { evt ->
                if (evt.type == "config_version_updated" || evt.type == "notification" || evt.type == "session_update") {
                    refreshControlPlane()
                }
            }
        }
        viewModelScope.launch {
            while (true) {
                delay(1000)
                if (vpnState.value == VpnConnectionState.CONNECTED) {
                    _uiState.update { it.copy(durationSeconds = it.durationSeconds + 1L) }
                } else if (vpnState.value == VpnConnectionState.DISCONNECTED) {
                    _uiState.update { it.copy(durationSeconds = 0L) }
                }
            }
        }
    }

    fun authenticatePin(pin: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val androidId = Settings.Secure.getString(appContext.contentResolver, Settings.Secure.ANDROID_ID) ?: "android-device"
            val deviceName = "${Build.MANUFACTURER} ${Build.MODEL}"
            runCatching {
                apiService.pinLogin(
                    PinLoginRequest(
                        pin = pin.trim(),
                        deviceFingerprint = androidId,
                        deviceName = deviceName
                    )
                )
            }.onSuccess { res ->
                tokenStore.saveTokens(res.accessToken, res.refreshToken, res.user.pinCode)
                _uiState.update {
                    it.copy(
                        isAuthenticated = true,
                        isLoading = false,
                        pinCode = res.user.pinCode,
                        username = res.user.username,
                        expiresAt = res.user.expiresAt,
                        deviceLimit = res.user.deviceLimit,
                        errorMessage = null
                    )
                }
                wsSyncClient.connect()
                refreshControlPlane()
            }.onFailure { err ->
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = err.message ?: "PIN authentication failed")
                }
            }
        }
    }

    fun refreshControlPlane() {
        viewModelScope.launch {
            runCatching {
                val me = runCatching { apiService.me() }.getOrNull()
                val srvs = apiService.listServers().servers
                val profs = apiService.listProfiles().profiles
                val ses = apiService.listSessions().sessions
                val notifs = apiService.listNotifications().announcements
                _uiState.update { state ->
                    state.copy(
                        pinCode = me?.pin?.pinCode ?: state.pinCode,
                        username = me?.user?.username ?: state.username,
                        expiresAt = me?.user?.expiresAt ?: state.expiresAt,
                        deviceLimit = me?.user?.deviceLimit ?: state.deviceLimit,
                        servers = srvs,
                        profiles = profs,
                        selectedServer = state.selectedServer?.takeIf { sel -> srvs.any { it.id == sel.id } } ?: srvs.firstOrNull(),
                        selectedProfile = state.selectedProfile?.takeIf { sel -> profs.any { it.id == sel.id } } ?: profs.firstOrNull(),
                        sessions = ses,
                        announcements = notifs,
                        errorMessage = null
                    )
                }
            }.onFailure { err ->
                if (tokenStore.getAccessToken().isNullOrBlank()) {
                    wsSyncClient.disconnect()
                    _uiState.value = ClientUiState(isAuthenticated = false, errorMessage = "Session expired, please sign in again")
                } else {
                    _uiState.update { it.copy(errorMessage = err.message) }
                }
            }
        }
    }

    fun selectServer(serverId: String) {
        _uiState.update { s -> s.copy(selectedServer = s.servers.firstOrNull { it.id == serverId } ?: s.selectedServer) }
    }

    fun selectProfile(profileId: String) {
        _uiState.update { s -> s.copy(selectedProfile = s.profiles.firstOrNull { it.id == profileId } ?: s.selectedProfile) }
    }

    fun setAutoReconnect(enabled: Boolean) {
        _uiState.update { it.copy(autoReconnect = enabled) }
    }

    fun setLocalKillSwitch(enabled: Boolean) {
        _uiState.update { it.copy(localKillSwitch = enabled) }
    }

    fun logout() {
        viewModelScope.launch {
            val rt = tokenStore.getRefreshToken()
            if (!rt.isNullOrBlank()) {
                runCatching { apiService.logout(mapOf("refreshToken" to rt)) }
            }
            wsSyncClient.disconnect()
            tokenStore.clear()
            _uiState.value = ClientUiState(isAuthenticated = false)
        }
    }
}
