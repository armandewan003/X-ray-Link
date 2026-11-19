package com.xraylink.client.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xraylink.client.data.AnnouncementDto
import com.xraylink.client.data.SessionRecordDto

private val Obsidian = Color(0xFF060B12)
private val CardNavy = Color(0xFF0E1726)
private val Cyan = Color(0xFF22D3EE)

@Composable
fun ConnectionDetailsScreen(
    serverName: String,
    endpoint: String,
    profileName: String,
    protocol: String,
    status: String,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().background(Obsidian).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Tunnel Details", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            TextButton(onClick = onBack) { Text("Back", color = Cyan) }
        }
        Surface(color = CardNavy, shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, Color(0xFF1E293B))) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Node: $serverName", color = Color.White, fontSize = 13.sp)
                Text("Endpoint: $endpoint", color = Cyan, fontSize = 13.sp)
                Text("Profile: $profileName ($protocol)", color = Color(0xFFCBD5E1), fontSize = 13.sp)
                Text("State: $status", color = Color(0xFF22C55E), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text("DNS: 1.1.1.1, 9.9.9.9", color = Color(0xFF94A3B8), fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun AccountScreen(
    pinCode: String,
    username: String,
    expiresAt: String,
    deviceLimit: Int,
    onLogout: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().background(Obsidian).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Subscriber Account", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            TextButton(onClick = onBack) { Text("Back", color = Cyan) }
        }
        Surface(color = CardNavy, shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, Color(0xFF1E293B))) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("PIN Code: $pinCode", color = Cyan, fontWeight = FontWeight.Bold)
                Text("Username: ${username.ifBlank { "Active Subscriber" }}", color = Color.White, fontSize = 13.sp)
                Text("Expires: ${expiresAt.take(10).ifBlank { "Active" }}", color = Color(0xFFCBD5E1), fontSize = 13.sp)
                Text("Device Limit: $deviceLimit", color = Color(0xFF94A3B8), fontSize = 13.sp)
            }
        }
        Button(
            onClick = onLogout,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7F1D1D)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Sign Out", color = Color.White)
        }
    }
}

@Composable
fun ClientSettingsScreen(
    autoReconnect: Boolean,
    killSwitch: Boolean,
    onAutoReconnectChange: (Boolean) -> Unit,
    onKillSwitchChange: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().background(Obsidian).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("VPN Client Settings", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            TextButton(onClick = onBack) { Text("Back", color = Cyan) }
        }
        Surface(color = CardNavy, shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, Color(0xFF1E293B))) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Auto-reconnect on network shift", color = Color.White, fontSize = 13.sp)
                    Switch(checked = autoReconnect, onCheckedChange = onAutoReconnectChange)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Android VPN Kill-Switch", color = Color.White, fontSize = 13.sp)
                    Switch(checked = killSwitch, onCheckedChange = onKillSwitchChange)
                }
            }
        }
    }
}

@Composable
fun NotificationsScreen(announcements: List<AnnouncementDto>, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(Obsidian).padding(20.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Announcements", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            TextButton(onClick = onBack) { Text("Back", color = Cyan) }
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(announcements) { ann ->
                Surface(color = CardNavy, shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, Color(0xFF1E293B))) {
                    Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                        Text(ann.title, color = Color.White, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(ann.body, color = Color(0xFF94A3B8), fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun ConnectionHistoryScreen(sessions: List<SessionRecordDto>, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(Obsidian).padding(20.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Connection History", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            TextButton(onClick = onBack) { Text("Back", color = Cyan) }
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(sessions) { ses ->
                Surface(color = CardNavy, shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, Color(0xFF1E293B))) {
                    Row(modifier = Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(ses.serverName, color = Color.White, fontWeight = FontWeight.Medium)
                            Text("${ses.profileName} · ${ses.status}", color = Color(0xFF94A3B8), fontSize = 11.sp)
                        }
                        Text("${ses.bytesDown / 1024} KB", color = Cyan, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
