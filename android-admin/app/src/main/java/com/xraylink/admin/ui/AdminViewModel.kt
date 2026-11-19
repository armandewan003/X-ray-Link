package com.xraylink.admin.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xraylink.admin.data.AdminApiService
import com.xraylink.admin.data.AdminTokenStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AdminUserItem(
    val id: String,
    val username: String,
    val displayName: String,
    val status: String,
    val pinCode: String?,
    val pinStatus: String?,
    val deviceLimit: Int,
    val expiresAt: String
)

data class AdminResellerItem(
    val id: String,
    val name: String,
    val email: String,
    val status: String,
    val balanceCredits: Int,
    val creditLimit: Int,
    val maxUsers: Int,
    val userCount: Int
)

data class AdminServerItem(
    val id: String,
    val name: String,
    val countryCode: String,
    val location: String,
    val endpoint: String,
    val port: Int,
    val protocol: String,
    val healthStatus: String,
    val isEnabled: Boolean,
    val activeSessionsCount: Int
)

data class AdminSessionItem(
    val id: String,
    val username: String,
    val serverName: String,
    val protocol: String,
    val status: String,
    val clientTunnelIp: String,
    val bytesUp: Long,
    val bytesDown: Long
)

data class AdminAuditLogItem(
    val id: String,
    val actorRole: String,
    val actorEmail: String,
    val action: String,
    val resourceType: String,
    val resourceId: String,
    val createdAt: String
)

data class AdminUiState(
    val isAuthenticated: Boolean = false,
    val role: String = "",
    val email: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val statusMessage: String? = null,
    val overview: AdminOverviewUiState = AdminOverviewUiState(),
    val users: List<AdminUserItem> = emptyList(),
    val resellers: List<AdminResellerItem> = emptyList(),
    val servers: List<AdminServerItem> = emptyList(),
    val sessions: List<AdminSessionItem> = emptyList(),
    val auditLogs: List<AdminAuditLogItem> = emptyList()
)

@HiltViewModel
class AdminViewModel @Inject constructor(
    private val api: AdminApiService,
    private val tokenStore: AdminTokenStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        AdminUiState(
            isAuthenticated = !tokenStore.getAccessToken().isNullOrBlank(),
            role = tokenStore.getRole().orEmpty(),
            email = tokenStore.getEmail().orEmpty()
        )
    )
    val uiState: StateFlow<AdminUiState> = _uiState.asStateFlow()

    init {
        if (!tokenStore.getAccessToken().isNullOrBlank()) {
            refreshAll()
        }
    }

    fun clearFeedback() {
        _uiState.update { it.copy(error = null, statusMessage = null) }
    }

    fun signIn(email: String, password: String, ownerMode: Boolean) {
        val trimmedEmail = email.trim()
        if (trimmedEmail.isEmpty() || password.isEmpty()) {
            _uiState.update { it.copy(error = "Email and password are required") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, statusMessage = null) }
            try {
                val payload = mapOf("email" to trimmedEmail, "password" to password)
                val res = if (ownerMode) api.ownerLogin(payload) else api.login(payload)
                val accessToken = res["accessToken"] as? String
                val refreshToken = res["refreshToken"] as? String ?: ""
                @Suppress("UNCHECKED_CAST")
                val principal = res["principal"] as? Map<String, Any>
                val role = (principal?.get("role") as? String) ?: ""
                val resolvedEmail = (principal?.get("email") as? String) ?: trimmedEmail

                if (accessToken.isNullOrBlank() || (role != "OWNER" && role != "ADMIN")) {
                    tokenStore.clear()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Administrator or Owner credentials required for Admin app"
                        )
                    }
                    return@launch
                }

                tokenStore.saveSession(accessToken, refreshToken, role, resolvedEmail)
                _uiState.update {
                    it.copy(
                        isAuthenticated = true,
                        role = role,
                        email = resolvedEmail,
                        isLoading = false,
                        statusMessage = "Signed in as $role"
                    )
                }
                refreshAll()
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Authentication failed") }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            val refresh = tokenStore.getRefreshToken().orEmpty()
            try {
                api.logout(mapOf("refreshToken" to refresh))
            } catch (_: Exception) {
                // Proceed with local token clear even if network unreachable
            } finally {
                tokenStore.clear()
                _uiState.value = AdminUiState(isAuthenticated = false)
            }
        }
    }

    fun refreshAll() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                loadOverviewAndConfig()
                runCatching { loadUsers() }
                runCatching { loadResellers() }
                runCatching { loadServers() }
                runCatching { loadSessions() }
                runCatching { loadAuditLogs() }
                _uiState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Failed to sync admin telemetry") }
            }
        }
    }

    private suspend fun loadOverviewAndConfig() {
        val overviewRes = api.getOverview()
        @Suppress("UNCHECKED_CAST")
        val stats = overviewRes["stats"] as? Map<String, Any> ?: emptyMap()

        val configRes = api.getConfig()
        @Suppress("UNCHECKED_CAST")
        val settings = configRes["settings"] as? Map<String, Any> ?: emptyMap()
        @Suppress("UNCHECKED_CAST")
        val maintenance = settings["maintenance_mode"] as? Map<String, Any> ?: emptyMap()
        @Suppress("UNCHECKED_CAST")
        val killSwitch = settings["emergency_kill_switch"] as? Map<String, Any> ?: emptyMap()

        _uiState.update {
            it.copy(
                overview = AdminOverviewUiState(
                    totalUsers = (stats["totalUsers"] as? Number)?.toInt() ?: 0,
                    activeConnections = (stats["activeConnections"] as? Number)?.toInt() ?: 0,
                    onlineServers = (stats["onlineServers"] as? Number)?.toInt() ?: 0,
                    totalResellers = (stats["totalResellers"] as? Number)?.toInt() ?: 0,
                    maintenanceEnabled = (maintenance["enabled"] as? Boolean) ?: false,
                    emergencyKillSwitch = (killSwitch["enabled"] as? Boolean) ?: false
                )
            )
        }
    }

    private suspend fun loadUsers() {
        val res = api.listUsers()
        @Suppress("UNCHECKED_CAST")
        val list = res["users"] as? List<Map<String, Any>> ?: emptyList()
        val mapped = list.map { row ->
            AdminUserItem(
                id = row["id"] as? String ?: "",
                username = row["username"] as? String ?: "",
                displayName = row["display_name"] as? String ?: "",
                status = row["status"] as? String ?: "ACTIVE",
                pinCode = row["pin_code"] as? String,
                pinStatus = row["pin_status"] as? String,
                deviceLimit = (row["device_limit"] as? Number)?.toInt() ?: 2,
                expiresAt = row["expires_at"] as? String ?: ""
            )
        }
        _uiState.update { it.copy(users = mapped) }
    }

    private suspend fun loadResellers() {
        val res = api.listResellers()
        @Suppress("UNCHECKED_CAST")
        val list = res["resellers"] as? List<Map<String, Any>> ?: emptyList()
        val mapped = list.map { row ->
            AdminResellerItem(
                id = row["id"] as? String ?: "",
                name = row["name"] as? String ?: "",
                email = row["email"] as? String ?: "",
                status = row["status"] as? String ?: "ACTIVE",
                balanceCredits = (row["balance_credits"] as? Number)?.toInt() ?: 0,
                creditLimit = (row["credit_limit"] as? Number)?.toInt() ?: 0,
                maxUsers = (row["max_users"] as? Number)?.toInt() ?: 0,
                userCount = (row["user_count"] as? Number)?.toInt() ?: 0
            )
        }
        _uiState.update { it.copy(resellers = mapped) }
    }

    private suspend fun loadServers() {
        val res = api.listServers()
        @Suppress("UNCHECKED_CAST")
        val list = res["servers"] as? List<Map<String, Any>> ?: emptyList()
        val mapped = list.map { row ->
            val enabledBool = (row["isEnabled"] as? Boolean) ?: (((row["is_enabled"] as? Number)?.toInt() ?: 0) == 1)
            AdminServerItem(
                id = row["id"] as? String ?: "",
                name = row["name"] as? String ?: "",
                countryCode = row["country_code"] as? String ?: "",
                location = row["location"] as? String ?: "",
                endpoint = row["endpoint"] as? String ?: "",
                port = (row["port"] as? Number)?.toInt() ?: 0,
                protocol = row["protocol"] as? String ?: "",
                healthStatus = row["health_status"] as? String ?: "ONLINE",
                isEnabled = enabledBool,
                activeSessionsCount = (row["active_sessions_count"] as? Number)?.toInt() ?: 0
            )
        }
        _uiState.update { it.copy(servers = mapped) }
    }

    private suspend fun loadSessions() {
        val res = api.listSessions()
        @Suppress("UNCHECKED_CAST")
        val list = res["sessions"] as? List<Map<String, Any>> ?: emptyList()
        val mapped = list.map { row ->
            AdminSessionItem(
                id = row["id"] as? String ?: "",
                username = row["username"] as? String ?: "",
                serverName = row["server_name"] as? String ?: "",
                protocol = row["protocol"] as? String ?: "",
                status = row["status"] as? String ?: "",
                clientTunnelIp = row["client_tunnel_ip"] as? String ?: "",
                bytesUp = (row["bytes_up"] as? Number)?.toLong() ?: 0L,
                bytesDown = (row["bytes_down"] as? Number)?.toLong() ?: 0L
            )
        }
        _uiState.update { it.copy(sessions = mapped) }
    }

    private suspend fun loadAuditLogs() {
        val res = api.getAuditLogs()
        @Suppress("UNCHECKED_CAST")
        val list = res["logs"] as? List<Map<String, Any>> ?: emptyList()
        val mapped = list.map { row ->
            AdminAuditLogItem(
                id = row["id"] as? String ?: "",
                actorRole = row["actor_role"] as? String ?: "",
                actorEmail = row["actor_email"] as? String ?: "",
                action = row["action"] as? String ?: "",
                resourceType = row["resource_type"] as? String ?: "",
                resourceId = row["resource_id"] as? String ?: "",
                createdAt = row["created_at"] as? String ?: ""
            )
        }
        _uiState.update { it.copy(auditLogs = mapped) }
    }

    fun generateQuickPin() {
        viewModelScope.launch {
            _uiState.update { it.copy(error = null, statusMessage = null) }
            try {
                val res = api.generatePins(
                    mapOf("count" to 1, "prefix" to "mobpin", "validityDays" to 30, "deviceLimit" to 2)
                )
                @Suppress("UNCHECKED_CAST")
                val generated = res["generated"] as? List<Map<String, Any>> ?: emptyList()
                val pinCode = generated.firstOrNull()?.get("pinCode") as? String
                _uiState.update { it.copy(statusMessage = if (pinCode != null) "Generated PIN: $pinCode" else "PIN generated") }
                loadUsers()
                loadOverviewAndConfig()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Failed to generate PIN") }
            }
        }
    }

    fun createUser(username: String, validityDays: Int, deviceLimit: Int) {
        val clean = username.trim()
        if (clean.isEmpty()) {
            _uiState.update { it.copy(error = "Username is required") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(error = null, statusMessage = null) }
            try {
                val res = api.createUser(
                    mapOf(
                        "username" to clean,
                        "displayName" to clean,
                        "validityDays" to validityDays,
                        "deviceLimit" to deviceLimit
                    )
                )
                @Suppress("UNCHECKED_CAST")
                val pin = res["pin"] as? Map<String, Any>
                val pinCode = pin?.get("pinCode") as? String ?: ""
                _uiState.update { it.copy(statusMessage = "User $clean created · PIN $pinCode") }
                loadUsers()
                loadOverviewAndConfig()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Failed to create user") }
            }
        }
    }

    fun rotateUserPin(userId: String) {
        viewModelScope.launch {
            try {
                val res = api.generatePins(mapOf("userId" to userId, "revokeExisting" to true))
                @Suppress("UNCHECKED_CAST")
                val pin = res["pin"] as? Map<String, Any>
                val pinCode = pin?.get("pinCode") as? String ?: ""
                _uiState.update { it.copy(statusMessage = "Rotated PIN: $pinCode") }
                loadUsers()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Failed to rotate PIN") }
            }
        }
    }

    fun toggleUserStatus(user: AdminUserItem) {
        val next = if (user.status == "ACTIVE") "SUSPENDED" else "ACTIVE"
        viewModelScope.launch {
            try {
                api.updateUser(user.id, mapOf("status" to next))
                _uiState.update { it.copy(statusMessage = "${user.username} -> $next") }
                loadUsers()
                loadSessions()
                loadOverviewAndConfig()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Failed to update user") }
            }
        }
    }

    fun adjustResellerCredits(resellerId: String, type: String, amount: Int) {
        viewModelScope.launch {
            try {
                api.adjustCredits(
                    mapOf(
                        "resellerId" to resellerId,
                        "type" to type,
                        "amount" to amount,
                        "description" to "Mobile admin $type"
                    )
                )
                _uiState.update { it.copy(statusMessage = "$type ($amount) applied") }
                loadResellers()
                loadOverviewAndConfig()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Credit adjustment failed") }
            }
        }
    }

    fun toggleServer(server: AdminServerItem) {
        viewModelScope.launch {
            try {
                api.updateServer(server.id, mapOf("isEnabled" to !server.isEnabled))
                _uiState.update { it.copy(statusMessage = "${server.name} ${if (!server.isEnabled) "enabled" else "disabled"}") }
                loadServers()
                loadOverviewAndConfig()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Server update failed") }
            }
        }
    }

    fun revokeSession(sessionId: String) {
        viewModelScope.launch {
            try {
                api.revokeSession(sessionId)
                _uiState.update { it.copy(statusMessage = "Session $sessionId revoked") }
                loadSessions()
                loadOverviewAndConfig()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Session revoke failed") }
            }
        }
    }

    fun toggleMaintenance() {
        val next = !_uiState.value.overview.maintenanceEnabled
        viewModelScope.launch {
            try {
                api.updateSetting(
                    "maintenance_mode",
                    mapOf("enabled" to next, "message" to "Scheduled X-RAY LINK infrastructure maintenance")
                )
                _uiState.update { it.copy(statusMessage = "Maintenance mode ${if (next) "ENABLED" else "DISABLED"}") }
                loadOverviewAndConfig()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Failed to toggle maintenance") }
            }
        }
    }

    fun toggleKillSwitch() {
        val next = !_uiState.value.overview.emergencyKillSwitch
        viewModelScope.launch {
            try {
                api.updateSetting(
                    "emergency_kill_switch",
                    mapOf("enabled" to next, "reason" to if (next) "Emergency kill switch triggered from Admin app" else "")
                )
                _uiState.update { it.copy(statusMessage = "Kill switch ${if (next) "ACTIVATED" else "CLEARED"}") }
                loadOverviewAndConfig()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Only Owner can toggle emergency kill switch") }
            }
        }
    }
}
