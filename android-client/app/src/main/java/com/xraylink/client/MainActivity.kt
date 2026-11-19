package com.xraylink.client

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.xraylink.client.ui.*
import com.xraylink.client.vpn.VpnConnectionState
import com.xraylink.client.vpn.XRayVpnService
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    background = Color(0xFF060B12),
                    surface = Color(0xFF0E1726),
                    primary = Color(0xFF22D3EE)
                )
            ) {
                XRayClientAppRoot(
                    onStartVpn = { serverId, profileId -> startVpnService(serverId, profileId) },
                    onStopVpn = { stopVpnService() }
                )
            }
        }
    }

    private fun startVpnService(serverId: String, profileId: String) {
        val intent = Intent(this, XRayVpnService::class.java).apply {
            action = XRayVpnService.ACTION_CONNECT
            putExtra("server_id", serverId)
            putExtra("profile_id", profileId)
        }
        ContextCompat.startForegroundService(this, intent)
    }

    private fun stopVpnService() {
        val intent = Intent(this, XRayVpnService::class.java).apply {
            action = XRayVpnService.ACTION_DISCONNECT
        }
        startService(intent)
    }
}

@Composable
private fun XRayClientAppRoot(
    onStartVpn: (String, String) -> Unit,
    onStopVpn: () -> Unit,
    vm: ClientViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val state by vm.uiState.collectAsState()
    val vpnState by vm.vpnState.collectAsState()
    val vpnStats by vm.vpnStats.collectAsState()
    val vpnError by vm.vpnError.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    var pendingServerId by remember { mutableStateOf<String?>(null) }
    var pendingProfileId by remember { mutableStateOf<String?>(null) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    val vpnPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val sid = pendingServerId
            val pid = pendingProfileId
            if (!sid.isNullOrBlank() && !pid.isNullOrBlank()) {
                onStartVpn(sid, pid)
            }
        }
    }

    fun toggleVpn() {
        if (vpnState == VpnConnectionState.CONNECTED || vpnState == VpnConnectionState.CONNECTING) {
            onStopVpn()
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        val sid = state.selectedServer?.id ?: return
        val pid = state.selectedProfile?.id ?: return
        val prepareIntent = VpnService.prepare(context)
        if (prepareIntent != null) {
            pendingServerId = sid
            pendingProfileId = pid
            vpnPermissionLauncher.launch(prepareIntent)
        } else {
            onStartVpn(sid, pid)
        }
    }

    Scaffold(
        containerColor = Color(0xFF060B12),
        bottomBar = {
            if (state.isAuthenticated) {
                NavigationBar(containerColor = Color(0xFF080E17)) {
                    NavigationBarItem(
                        selected = false,
                        onClick = { navController.navigate("dashboard") { launchSingleTop = true } },
                        icon = { Icon(Icons.Default.Shield, contentDescription = "VPN") },
                        label = { Text("VPN") }
                    )
                    NavigationBarItem(
                        selected = false,
                        onClick = { navController.navigate("history") },
                        icon = { Icon(Icons.Default.History, contentDescription = "History") },
                        label = { Text("History") }
                    )
                    NavigationBarItem(
                        selected = false,
                        onClick = { navController.navigate("notifications") },
                        icon = { Icon(Icons.Default.Notifications, contentDescription = "Alerts") },
                        label = { Text("Alerts") }
                    )
                    NavigationBarItem(
                        selected = false,
                        onClick = { navController.navigate("account") },
                        icon = { Icon(Icons.Default.Person, contentDescription = "Account") },
                        label = { Text("Account") }
                    )
                    NavigationBarItem(
                        selected = false,
                        onClick = { navController.navigate("settings") },
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                        label = { Text("Settings") }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "splash",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("splash") {
                SplashScreen(
                    onReady = {
                        navController.navigate(if (state.isAuthenticated) "dashboard" else "login") {
                            popUpTo("splash") { inclusive = true }
                        }
                    }
                )
            }
            composable("login") {
                LaunchedEffect(state.isAuthenticated) {
                    if (state.isAuthenticated) {
                        navController.navigate("dashboard") {
                            popUpTo("login") { inclusive = true }
                        }
                    }
                }
                PinLoginScreen(
                    isLoading = state.isLoading,
                    errorMessage = state.errorMessage,
                    onSubmitPin = { vm.authenticatePin(it) }
                )
            }
            composable("dashboard") {
                Column {
                    val activeError = vpnError ?: state.errorMessage
                    if (!activeError.isNullOrBlank()) {
                        Surface(
                            color = Color(0xFF3F1219),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = activeError,
                                color = Color(0xFFFCA5A5),
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                    DashboardScreen(
                        pinCode = state.pinCode.ifBlank { "XL-SUBSCRIBER" },
                        expiresAt = state.expiresAt.ifBlank { "—" },
                        selectedServerName = state.selectedServer?.let { "${it.countryCode} · ${it.name}" } ?: "Select Server",
                        selectedProfileName = state.selectedProfile?.name ?: "Select Profile",
                        latencyMs = state.selectedServer?.latencyMs ?: 0,
                        connectionState = vpnState,
                        downloadFormatted = formatBytes(vpnStats.rxBytes),
                        uploadFormatted = formatBytes(vpnStats.txBytes),
                        durationFormatted = formatDuration(state.durationSeconds),
                        onSelectServerClick = { navController.navigate("servers") },
                        onSelectProfileClick = { navController.navigate("profiles") },
                        onToggleConnect = { toggleVpn() }
                    )
                }
            }
            composable("servers") {
                ServerSelectionScreen(
                    servers = state.servers.map {
                        ServerUiItem(it.id, it.name, it.location, it.protocol, it.latencyMs)
                    },
                    selectedId = state.selectedServer?.id.orEmpty(),
                    onSelect = {
                        vm.selectServer(it.id)
                        navController.popBackStack()
                    }
                )
            }
            composable("profiles") {
                ProfileSelectionScreen(
                    profiles = state.profiles.map {
                        ProfileUiItem(it.id, it.name, it.protocol, it.description.orEmpty())
                    },
                    selectedId = state.selectedProfile?.id.orEmpty(),
                    onSelect = {
                        vm.selectProfile(it.id)
                        navController.popBackStack()
                    }
                )
            }
            composable("details") {
                ConnectionDetailsScreen(
                    serverName = state.selectedServer?.name ?: "None",
                    endpoint = state.selectedServer?.let { "${it.endpoint}:${it.port}" } ?: "—",
                    profileName = state.selectedProfile?.name ?: "None",
                    protocol = state.selectedProfile?.protocol ?: "—",
                    status = vpnState.name,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("account") {
                AccountScreen(
                    pinCode = state.pinCode,
                    username = state.username,
                    expiresAt = state.expiresAt,
                    deviceLimit = state.deviceLimit,
                    onLogout = {
                        onStopVpn()
                        vm.logout()
                        navController.navigate("login") { popUpTo(0) { inclusive = true } }
                    },
                    onBack = { navController.popBackStack() }
                )
            }
            composable("settings") {
                ClientSettingsScreen(
                    autoReconnect = state.autoReconnect,
                    killSwitch = state.localKillSwitch,
                    onAutoReconnectChange = vm::setAutoReconnect,
                    onKillSwitchChange = vm::setLocalKillSwitch,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("notifications") {
                NotificationsScreen(announcements = state.announcements, onBack = { navController.popBackStack() })
            }
            composable("history") {
                ConnectionHistoryScreen(sessions = state.sessions, onBack = { navController.popBackStack() })
            }
        }
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 B"
    val kb = bytes / 1024.0
    if (kb < 1024.0) return String.format(Locale.US, "%.1f KB", kb)
    val mb = kb / 1024.0
    if (mb < 1024.0) return String.format(Locale.US, "%.2f MB", mb)
    return String.format(Locale.US, "%.2f GB", mb / 1024.0)
}

private fun formatDuration(sec: Long): String {
    val h = sec / 3600
    val m = (sec % 3600) / 60
    val s = sec % 60
    return String.format(Locale.US, "%02d:%02d:%02d", h, m, s)
}
