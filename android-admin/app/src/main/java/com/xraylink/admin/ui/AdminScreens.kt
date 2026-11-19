package com.xraylink.admin.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AdminLoginScreen(
    isLoading: Boolean,
    error: String?,
    onSignIn: (String, String, Boolean) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var ownerMode by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF060B12))
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("X-RAY LINK ADMIN", color = Color(0xFF22D3EE), fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text("Central Operations Control Plane", color = Color(0xFF94A3B8), fontSize = 13.sp)
        Spacer(modifier = Modifier.height(20.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = ownerMode,
                onClick = { ownerMode = true },
                label = { Text("Owner Portal") }
            )
            FilterChip(
                selected = !ownerMode,
                onClick = { ownerMode = false },
                label = { Text("Delegated Admin") }
            )
        }
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Admin Email") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        if (!error.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(error, color = Color(0xFFFCA5A5), fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.height(18.dp))
        Button(
            onClick = { onSignIn(email, password, ownerMode) },
            enabled = !isLoading,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0E2A47)),
            border = BorderStroke(1.dp, Color(0xFF22D3EE)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text(if (isLoading) "Authenticating..." else "Sign In", color = Color.White, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun AdminUsersScreen(
    users: List<AdminUserItem>,
    onCreateUser: (String, Int, Int) -> Unit,
    onRotatePin: (String) -> Unit,
    onToggleStatus: (AdminUserItem) -> Unit
) {
    var username by remember { mutableStateOf("") }
    var validityDays by remember { mutableStateOf("30") }
    var deviceLimit by remember { mutableStateOf("2") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF060B12))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Users & PIN Codes", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        item {
            Surface(
                color = Color(0xFF0E1726),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Color(0xFF1E293B))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Provision Subscriber", color = Color(0xFF22D3EE), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Username") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = validityDays,
                            onValueChange = { validityDays = it },
                            label = { Text("Days") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = deviceLimit,
                            onValueChange = { deviceLimit = it },
                            label = { Text("Devices") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Button(
                        onClick = {
                            onCreateUser(username, validityDays.toIntOrNull() ?: 30, deviceLimit.toIntOrNull() ?: 2)
                            username = ""
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0E2A47)),
                        border = BorderStroke(1.dp, Color(0xFF22D3EE)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Create User & PIN", color = Color.White)
                    }
                }
            }
        }

        items(users, key = { it.id }) { u ->
            Surface(
                color = Color(0xFF0E1726),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Color(0xFF1E293B))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(u.username, color = Color.White, fontWeight = FontWeight.SemiBold)
                        Text(
                            u.status,
                            color = if (u.status == "ACTIVE") Color(0xFF34D399) else Color(0xFFFCA5A5),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        "PIN: ${u.pinCode ?: "None"} (${u.pinStatus ?: "N/A"}) · Devices: ${u.deviceLimit}",
                        color = Color(0xFF22D3EE),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    )
                    Text("Expires: ${u.expiresAt.take(10)}", color = Color(0xFF94A3B8), fontSize = 11.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { onRotatePin(u.id) }) {
                            Text("Rotate PIN", fontSize = 12.sp)
                        }
                        OutlinedButton(onClick = { onToggleStatus(u) }) {
                            Text(if (u.status == "ACTIVE") "Suspend" else "Activate", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminResellersScreen(
    resellers: List<AdminResellerItem>,
    onAdjustCredits: (String, String, Int) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF060B12))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Resellers & Credit Wallets", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        items(resellers, key = { it.id }) { r ->
            Surface(
                color = Color(0xFF0E1726),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Color(0xFF1E293B))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(r.name, color = Color.White, fontWeight = FontWeight.SemiBold)
                        Text("${r.balanceCredits} cr", color = Color(0xFF22D3EE), fontWeight = FontWeight.Bold)
                    }
                    Text(r.email, color = Color(0xFF94A3B8), fontSize = 12.sp)
                    Text("Users: ${r.userCount}/${r.maxUsers} · Limit: ${r.creditLimit} · ${r.status}", color = Color(0xFFCBD5E1), fontSize = 11.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { onAdjustCredits(r.id, "CREDIT_ADD", 50) }) {
                            Text("+50 Credits", fontSize = 12.sp)
                        }
                        OutlinedButton(onClick = { onAdjustCredits(r.id, "CREDIT_DEDUCT", 10) }) {
                            Text("-10 Credits", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminServersScreen(
    servers: List<AdminServerItem>,
    onToggleServer: (AdminServerItem) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF060B12))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("VPN Servers", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        items(servers, key = { it.id }) { s ->
            Surface(
                color = Color(0xFF0E1726),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Color(0xFF1E293B))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("[${s.countryCode}] ${s.name}", color = Color.White, fontWeight = FontWeight.SemiBold)
                        Text("${s.protocol} · ${s.endpoint}:${s.port}", color = Color(0xFF22D3EE), fontSize = 12.sp)
                        Text("${s.location} · ${s.healthStatus} · ${s.activeSessionsCount} active", color = Color(0xFF94A3B8), fontSize = 11.sp)
                    }
                    Switch(checked = s.isEnabled, onCheckedChange = { onToggleServer(s) })
                }
            }
        }
    }
}

@Composable
fun AdminSessionsScreen(
    sessions: List<AdminSessionItem>,
    onRevokeSession: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF060B12))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("VPN Sessions", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        items(sessions, key = { it.id }) { ses ->
            val live = ses.status == "CONNECTED" || ses.status == "CONNECTING" || ses.status == "RECONNECTING"
            Surface(
                color = Color(0xFF0E1726),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Color(0xFF1E293B))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("${ses.username} -> ${ses.serverName}", color = Color.White, fontWeight = FontWeight.SemiBold)
                        Text("${ses.protocol} · ${ses.clientTunnelIp}", color = Color(0xFF94A3B8), fontSize = 12.sp)
                        Text("Status: ${ses.status} · ↑${ses.bytesUp / 1024}KB ↓${ses.bytesDown / 1024}KB", color = Color(0xFF22D3EE), fontSize = 11.sp)
                    }
                    if (live) {
                        OutlinedButton(onClick = { onRevokeSession(ses.id) }) {
                            Text("Revoke", color = Color(0xFFFCA5A5), fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminAuditLogsScreen(
    logs: List<AdminAuditLogItem>
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF060B12))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text("Security Audit Trail", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        items(logs, key = { it.id }) { log ->
            Surface(
                color = Color(0xFF0E1726),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFF1E293B))
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(log.action, color = Color(0xFF22D3EE), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text(log.actorRole, color = Color(0xFF94A3B8), fontSize = 11.sp)
                    }
                    Text("${log.actorEmail} · ${log.resourceType}/${log.resourceId}", color = Color.White, fontSize = 12.sp)
                    Text(log.createdAt, color = Color(0xFF64748B), fontSize = 10.sp)
                }
            }
        }
    }
}
