package com.xraylink.admin.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class AdminOverviewUiState(
    val totalUsers: Int = 0,
    val activeConnections: Int = 0,
    val onlineServers: Int = 0,
    val totalResellers: Int = 0,
    val maintenanceEnabled: Boolean = false,
    val emergencyKillSwitch: Boolean = false
)

@Composable
fun AdminDashboardScreen(
    state: AdminOverviewUiState,
    onGeneratePinClick: () -> Unit,
    onToggleMaintenance: () -> Unit,
    onToggleKillSwitch: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF060B12))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("X-RAY LINK ADMIN", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("Synchronized Central Control Plane", color = Color(0xFF94A3B8), fontSize = 12.sp)
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricTile("USERS", state.totalUsers.toString(), Modifier.weight(1f))
                MetricTile("ACTIVE VPN", state.activeConnections.toString(), Modifier.weight(1f))
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricTile("ONLINE NODES", state.onlineServers.toString(), Modifier.weight(1f))
                MetricTile("RESELLERS", state.totalResellers.toString(), Modifier.weight(1f))
            }
        }

        item {
            Button(
                onClick = onGeneratePinClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0E2A47)),
                border = BorderStroke(1.dp, Color(0xFF22D3EE)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text("Generate User & PIN", color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        }

        item {
            Surface(
                color = Color(0xFF0E1726),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Color(0xFF1E293B))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Maintenance Mode", color = Color.White)
                        Switch(checked = state.maintenanceEnabled, onCheckedChange = { onToggleMaintenance() })
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Emergency Kill Switch", color = Color(0xFFFCA5A5))
                        Switch(checked = state.emergencyKillSwitch, onCheckedChange = { onToggleKillSwitch() })
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricTile(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        color = Color(0xFF0E1726),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Color(0xFF1E293B)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(label, color = Color(0xFF94A3B8), fontSize = 11.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, color = Color(0xFF22D3EE), fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
    }
}
