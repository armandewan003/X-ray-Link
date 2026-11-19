package com.xraylink.admin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.xraylink.admin.ui.*
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFF22D3EE),
                    background = Color(0xFF060B12),
                    surface = Color(0xFF0E1726),
                    onBackground = Color.White,
                    onSurface = Color.White
                )
            ) {
                XRayAdminAppRoot()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun XRayAdminAppRoot(viewModel: AdminViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    val navController = rememberNavController()
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

    if (!state.isAuthenticated) {
        AdminLoginScreen(
            isLoading = state.isLoading,
            error = state.error,
            onSignIn = { email, password, ownerMode -> viewModel.signIn(email, password, ownerMode) }
        )
        return
    }

    Scaffold(
        containerColor = Color(0xFF060B12),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("X-RAY LINK", color = Color.White)
                        Text("${state.role} · ${state.email}", color = Color(0xFF94A3B8), style = MaterialTheme.typography.labelSmall)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0A111C)),
                actions = {
                    IconButton(onClick = { viewModel.refreshAll() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Sync", tint = Color(0xFF22D3EE))
                    }
                    IconButton(onClick = { viewModel.logout() }) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Sign out", tint = Color(0xFFFCA5A5))
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(containerColor = Color(0xFF0A111C)) {
                val tabs = listOf(
                    Triple("overview", "Home", Icons.Default.Dashboard),
                    Triple("users", "Users", Icons.Default.People),
                    Triple("resellers", "Resellers", Icons.Default.Store),
                    Triple("servers", "Servers", Icons.Default.Dns),
                    Triple("sessions", "Sessions", Icons.Default.Cable),
                    Triple("audit", "Audit", Icons.Default.History)
                )
                tabs.forEach { (route, label, icon) ->
                    NavigationBarItem(
                        selected = currentRoute == route,
                        onClick = {
                            navController.navigate(route) {
                                popUpTo("overview") { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(icon, contentDescription = label) },
                        label = { Text(label, maxLines = 1, softWrap = false, fontSize = 10.sp) }
                    )
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().background(Color(0xFF060B12)).padding(padding)) {
            if (!state.statusMessage.isNullOrBlank()) {
                Surface(color = Color(0xFF0E2A47), modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
                    Row(modifier = Modifier.padding(10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(state.statusMessage!!, color = Color(0xFF22D3EE))
                        TextButton(onClick = { viewModel.clearFeedback() }) { Text("OK") }
                    }
                }
            }
            if (!state.error.isNullOrBlank()) {
                Surface(color = Color(0xFF3F1219), modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
                    Row(modifier = Modifier.padding(10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(state.error!!, color = Color(0xFFFCA5A5))
                        TextButton(onClick = { viewModel.clearFeedback() }) { Text("Dismiss") }
                    }
                }
            }

            NavHost(navController = navController, startDestination = "overview") {
                composable("overview") {
                    AdminDashboardScreen(
                        state = state.overview,
                        onGeneratePinClick = { viewModel.generateQuickPin() },
                        onToggleMaintenance = { viewModel.toggleMaintenance() },
                        onToggleKillSwitch = { viewModel.toggleKillSwitch() }
                    )
                }
                composable("users") {
                    AdminUsersScreen(
                        users = state.users,
                        onCreateUser = { u, d, l -> viewModel.createUser(u, d, l) },
                        onRotatePin = { uid -> viewModel.rotateUserPin(uid) },
                        onToggleStatus = { user -> viewModel.toggleUserStatus(user) }
                    )
                }
                composable("resellers") {
                    AdminResellersScreen(
                        resellers = state.resellers,
                        onAdjustCredits = { id, type, amt -> viewModel.adjustResellerCredits(id, type, amt) }
                    )
                }
                composable("servers") {
                    AdminServersScreen(
                        servers = state.servers,
                        onToggleServer = { srv -> viewModel.toggleServer(srv) }
                    )
                }
                composable("sessions") {
                    AdminSessionsScreen(
                        sessions = state.sessions,
                        onRevokeSession = { sid -> viewModel.revokeSession(sid) }
                    )
                }
                composable("audit") {
                    AdminAuditLogsScreen(logs = state.auditLogs)
                }
            }
        }
    }
}
