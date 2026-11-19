package com.xraylink.client.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Obsidian = Color(0xFF060B12)
private val SurfaceNavy = Color(0xFF0E1726)
private val Cyan = Color(0xFF22D3EE)

@Composable
fun SplashScreen(onReady: () -> Unit) {
    LaunchedEffect(Unit) { onReady() }
    Box(
        modifier = Modifier.fillMaxSize().background(Obsidian),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("X-RAY LINK", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Text("ENCRYPTED VPN ECOSYSTEM", color = Cyan, fontSize = 11.sp, letterSpacing = 2.sp)
        }
    }
}

@Composable
fun PinLoginScreen(
    isLoading: Boolean,
    errorMessage: String?,
    onSubmitPin: (String) -> Unit
) {
    var pin by remember { mutableStateOf("") }
    Column(
        modifier = Modifier.fillMaxSize().background(Obsidian).padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("X-RAY LINK ACCESS", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Enter your subscriber PIN code", color = Color(0xFF94A3B8), fontSize = 13.sp)
        Spacer(modifier = Modifier.height(20.dp))
        OutlinedTextField(
            value = pin,
            onValueChange = { pin = it.uppercase() },
            label = { Text("PIN Code") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        if (!errorMessage.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(errorMessage, color = Color(0xFFF87171), fontSize = 12.sp)
        }
        Spacer(modifier = Modifier.height(18.dp))
        Button(
            onClick = { onSubmitPin(pin.trim()) },
            enabled = !isLoading && pin.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text(if (isLoading) "VERIFYING..." else "AUTHENTICATE PIN")
        }
    }
}

data class ServerUiItem(val id: String, val name: String, val location: String, val protocol: String, val latencyMs: Int)
data class ProfileUiItem(val id: String, val name: String, val protocol: String, val description: String)

@Composable
fun ServerSelectionScreen(servers: List<ServerUiItem>, selectedId: String, onSelect: (ServerUiItem) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Obsidian).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(servers) { srv ->
            Surface(
                color = SurfaceNavy,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, if (srv.id == selectedId) Cyan else Color(0xFF1E293B)),
                modifier = Modifier.fillMaxWidth().clickable { onSelect(srv) }
            ) {
                Row(modifier = Modifier.padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text(srv.name, color = Color.White, fontWeight = FontWeight.SemiBold)
                        Text("${srv.location} · ${srv.protocol}", color = Color(0xFF94A3B8), fontSize = 12.sp)
                    }
                    Text("${srv.latencyMs} ms", color = Color(0xFF22C55E), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ProfileSelectionScreen(profiles: List<ProfileUiItem>, selectedId: String, onSelect: (ProfileUiItem) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Obsidian).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(profiles) { prof ->
            Surface(
                color = SurfaceNavy,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, if (prof.id == selectedId) Cyan else Color(0xFF1E293B)),
                modifier = Modifier.fillMaxWidth().clickable { onSelect(prof) }
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(prof.name, color = Color.White, fontWeight = FontWeight.SemiBold)
                    Text(prof.protocol, color = Cyan, fontSize = 11.sp)
                    Text(prof.description, color = Color(0xFF94A3B8), fontSize = 12.sp)
                }
            }
        }
    }
}
