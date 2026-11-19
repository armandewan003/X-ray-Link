package com.xraylink.client.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xraylink.client.vpn.VpnConnectionState

private val ObsidianBg = Color(0xFF060B12)
private val CardNavy = Color(0xFF0E1726)
private val CyanGlow = Color(0xFF22D3EE)
private val TealAccent = Color(0xFF14B8A6)
private val NeonConnected = Color(0xFF22C55E)

@Composable
fun DashboardScreen(
    pinCode: String,
    expiresAt: String,
    selectedServerName: String,
    selectedProfileName: String,
    latencyMs: Int,
    connectionState: VpnConnectionState,
    downloadFormatted: String,
    uploadFormatted: String,
    durationFormatted: String,
    onSelectServerClick: () -> Unit,
    onSelectProfileClick: () -> Unit,
    onToggleConnect: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Circular Cybersecurity X-RAY LINK Brand Header
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .background(
                        brush = Brush.radialGradient(listOf(CyanGlow.copy(alpha = 0.24f), Color.Transparent)),
                        shape = CircleShape
                    )
                    .border(1.5.dp, CyanGlow.copy(alpha = 0.7f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = "X-RAY LINK Emblem",
                    tint = CyanGlow,
                    modifier = Modifier.size(40.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text("X-RAY LINK", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Text("ENCRYPTED TUNNEL CLIENT", color = Color(0xFF94A3B8), fontSize = 11.sp, letterSpacing = 1.5.sp)
        }

        // Account & Live Telemetry Card
        Surface(
            color = CardNavy,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, CyanGlow.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("PIN ACCOUNT", color = Color(0xFF94A3B8), fontSize = 11.sp)
                        Text(pinCode, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("EXPIRES", color = Color(0xFF94A3B8), fontSize = 11.sp)
                        Text(expiresAt.take(10), color = CyanGlow, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    }
                }
                HorizontalDivider(color = Color(0xFF1E293B))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("DOWNLOAD", color = Color(0xFF94A3B8), fontSize = 11.sp)
                        Text(downloadFormatted, color = TealAccent, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                    Column {
                        Text("UPLOAD", color = Color(0xFF94A3B8), fontSize = 11.sp)
                        Text(uploadFormatted, color = CyanGlow, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("DURATION", color = Color(0xFF94A3B8), fontSize = 11.sp)
                        Text(durationFormatted, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }
        }

        // Server & Profile Selectors
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedCard(
                onClick = onSelectServerClick,
                colors = CardDefaults.outlinedCardColors(containerColor = CardNavy),
                border = BorderStroke(1.dp, Color(0xFF1E293B)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("SERVER NODE", color = Color(0xFF94A3B8), fontSize = 10.sp)
                        Text(selectedServerName, color = Color.White, fontWeight = FontWeight.Medium)
                    }
                    Text("${latencyMs} ms", color = NeonConnected, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            OutlinedCard(
                onClick = onSelectProfileClick,
                colors = CardDefaults.outlinedCardColors(containerColor = CardNavy),
                border = BorderStroke(1.dp, Color(0xFF1E293B)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("CONNECTION PROFILE", color = Color(0xFF94A3B8), fontSize = 10.sp)
                        Text(selectedProfileName, color = Color.White, fontWeight = FontWeight.Medium)
                    }
                    Icon(Icons.Default.Lock, contentDescription = null, tint = CyanGlow, modifier = Modifier.size(18.dp))
                }
            }
        }

        // Status & Primary Connect Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Status", color = Color(0xFF94A3B8), fontSize = 12.sp)
                val statusColor = when (connectionState) {
                    VpnConnectionState.CONNECTED -> NeonConnected
                    VpnConnectionState.CONNECTING, VpnConnectionState.RECONNECTING -> CyanGlow
                    VpnConnectionState.FAILED, VpnConnectionState.EXPIRED -> Color(0xFFEF4444)
                    VpnConnectionState.DISCONNECTED -> Color(0xFF7DD3FC)
                }
                Text(connectionState.name, color = statusColor, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }

            Button(
                onClick = onToggleConnect,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (connectionState == VpnConnectionState.CONNECTED) Color(0xFF14532D) else Color(0xFF0E2A47)
                ),
                border = BorderStroke(
                    1.5.dp,
                    if (connectionState == VpnConnectionState.CONNECTED) NeonConnected else CyanGlow
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.height(52.dp).width(176.dp)
            ) {
                Text(
                    text = if (connectionState == VpnConnectionState.CONNECTED) "DISCONNECT" else "CONNECT",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}
