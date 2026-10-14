package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.ServerUserRecord
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminAuditDashboardDialog(
    model: VaultViewModel,
    onDismiss: () -> Unit
) {
    val serverUsers by model.serverUsers.collectAsState(initial = emptyList())
    val userCount by model.serverUserCount.collectAsState(initial = serverUsers.size)
    val clusterMetrics by model.clusterMetrics.collectAsState(initial = null)

    var searchQuery by remember { mutableStateOf("") }
    var selectedRoleFilter by remember { mutableStateOf("ALL") } // "ALL", "OWNER", "SPOUSE", "ADMIN"
    var inspectedUser by remember { mutableStateOf<ServerUserRecord?>(null) }
    var showQuickRegisterDialog by remember { mutableStateOf(false) }

    val filteredUsers = remember(serverUsers, searchQuery, selectedRoleFilter) {
        serverUsers.filter { user ->
            val matchesQuery = searchQuery.isBlank() ||
                user.fullName.contains(searchQuery, ignoreCase = true) ||
                user.email.contains(searchQuery, ignoreCase = true) ||
                user.userId.contains(searchQuery, ignoreCase = true) ||
                user.nomineeName.contains(searchQuery, ignoreCase = true)
            val matchesRole = selectedRoleFilter == "ALL" || user.role.equals(selectedRoleFilter, ignoreCase = true)
            matchesQuery && matchesRole
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(SlatePrimary),
            color = SlatePrimary
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                // Top App Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (VaultTheme.isDark) Color(0xFF0F172A) else Color(0xFF0B192C))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(TealAccent.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AdminPanelSettings,
                                contentDescription = "Admin Panel",
                                tint = TealAccent,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "SERVER AUDIT & REGISTRY DASHBOARD",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFF10B981).copy(alpha = 0.2f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("LIVE CLUSTER", color = Color(0xFF10B981), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Text(
                                text = "Real-time user persistence, token tracking & load balancer metrics",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 10.sp
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { showQuickRegisterDialog = true },
                            modifier = Modifier.testTag("btn_admin_add_user")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PersonAdd,
                                contentDescription = "Add User",
                                tint = TealAccent
                            )
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.testTag("btn_admin_close")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.White
                            )
                        }
                    }
                }

                // Main Content Body
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(VaultTheme.background)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Item 1: Real-Time Cluster Health & Scaling Topology
                    item {
                        ClusterHealthAndScalingCard(metrics = clusterMetrics)
                    }

                    // Item 2: KPI Metrics Cards Row
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            AdminKpiMetricCard(
                                title = "TOTAL REGISTERED",
                                value = userCount.toString(),
                                subtitle = "Persisted on Server DB",
                                icon = Icons.Default.Group,
                                accentColor = TealAccent,
                                modifier = Modifier.weight(1f)
                            )
                            AdminKpiMetricCard(
                                title = "ACTIVE SESSIONS",
                                value = serverUsers.count { it.status == "ACTIVE_VERIFIED" }.toString(),
                                subtitle = "Valid JWT Tokens",
                                icon = Icons.Default.VerifiedUser,
                                accentColor = Color(0xFF10B981),
                                modifier = Modifier.weight(1f)
                            )
                            AdminKpiMetricCard(
                                title = "AVG LATENCY",
                                value = "${clusterMetrics?.avgLatencyMs ?: 14}ms",
                                subtitle = "Async Queue Dispatch",
                                icon = Icons.Default.Speed,
                                accentColor = Color(0xFF0284C7),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Item 3: Query & Filter Section
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = VaultTheme.cardBg),
                            border = BorderStroke(1.dp, VaultTheme.cardBorder),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "USER REGISTRY QUERY & FILTER ENGINE",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TealAccent,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("admin_search_query_input"),
                                    placeholder = { Text("Search by email, name, user ID, or nominee...", fontSize = 12.sp) },
                                    leadingIcon = {
                                        Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = VaultTheme.textSecondary)
                                    },
                                    trailingIcon = {
                                        if (searchQuery.isNotEmpty()) {
                                            IconButton(onClick = { searchQuery = "" }) {
                                                Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear", tint = VaultTheme.textSecondary)
                                            }
                                        }
                                    },
                                    singleLine = true,
                                    colors = vaultTextFieldColors()
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                // Role Filter Chips
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    val roles = listOf("ALL", "OWNER", "SPOUSE", "ADMIN")
                                    roles.forEach { role ->
                                        val isSelected = selectedRoleFilter == role
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(if (isSelected) TealAccent else VaultTheme.subCardBg)
                                                .border(1.dp, if (isSelected) TealAccent else VaultTheme.cardBorder, RoundedCornerShape(6.dp))
                                                .clickable { selectedRoleFilter = role }
                                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                text = if (role == "ALL") "All Users ($userCount)" else "$role (${serverUsers.count { it.role.equals(role, ignoreCase = true) }})",
                                                color = if (isSelected) Color.White else VaultTheme.textPrimary,
                                                fontSize = 11.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Item 4: Registered Users List Header
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "REGISTERED SERVER ACCOUNTS (${filteredUsers.size})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = VaultTheme.textPrimary
                            )
                            Text(
                                text = "Sorted by Latest Registration",
                                fontSize = 11.sp,
                                color = VaultTheme.textSecondary
                            )
                        }
                    }

                    // Items: User Record Cards
                    if (filteredUsers.isEmpty()) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = VaultTheme.cardBg),
                                border = BorderStroke(1.dp, VaultTheme.cardBorder),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(imageVector = Icons.Default.SearchOff, contentDescription = "None", tint = VaultTheme.textSecondary, modifier = Modifier.size(36.dp))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("No matching user records found", fontWeight = FontWeight.Bold, color = VaultTheme.textPrimary, fontSize = 13.sp)
                                    Text("Try adjusting your query or filter parameters", color = VaultTheme.textSecondary, fontSize = 11.sp)
                                }
                            }
                        }
                    } else {
                        items(filteredUsers, key = { it.userId }) { user ->
                            ServerUserAuditCard(
                                user = user,
                                onInspect = { inspectedUser = user },
                                onDelete = { model.deleteServerUserRecord(user) }
                            )
                        }
                    }
                }
            }
        }
    }

    // Modal: User Details & Security Payload Inspector
    inspectedUser?.let { user ->
        ServerUserInspectorDialog(
            user = user,
            onDismiss = { inspectedUser = null }
        )
    }

    // Modal: Quick Register Test User
    if (showQuickRegisterDialog) {
        AdminQuickRegisterUserDialog(
            model = model,
            onDismiss = { showQuickRegisterDialog = false }
        )
    }
}

@Composable
fun ClusterHealthAndScalingCard(metrics: com.example.util.ClusterMetrics?) {
    Card(
        colors = CardDefaults.cardColors(containerColor = VaultTheme.cardBg),
        border = BorderStroke(1.dp, VaultTheme.cardBorder),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "CLUSTER TOPOLOGY & ELASTIC SCALING",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = VaultTheme.textPrimary,
                        letterSpacing = 0.5.sp
                    )
                }
                Text(
                    text = "ELB • NGINX Active",
                    fontSize = 10.sp,
                    color = Color(0xFF10B981),
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Kubernetes Pods status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val nodes = metrics?.activeNodes ?: listOf(
                    com.example.util.ClusterNodeStatus("node-01-auth", "Auth Microservice", "Auth & JWT", "HEALTHY", 24, 14),
                    com.example.util.ClusterNodeStatus("node-02-vault", "Vault Core", "AES-256 Storage", "HEALTHY", 31, 18),
                    com.example.util.ClusterNodeStatus("node-03-audit", "Audit & Observer", "Compliance Ledger", "HEALTHY", 18, 12)
                )
                nodes.forEach { node ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = VaultTheme.subCardBg),
                        border = BorderStroke(1.dp, VaultTheme.cardBorder),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(node.name.take(12), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = VaultTheme.textPrimary)
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF10B981))
                                )
                            }
                            Text(node.role, fontSize = 8.sp, color = VaultTheme.textSecondary, maxLines = 1)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Load: ${node.loadPercent}%", fontSize = 9.sp, color = TealAccent, fontWeight = FontWeight.Bold)
                                Text("${node.latencyMs}ms", fontSize = 9.sp, color = VaultTheme.textSecondary)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Asynchronous Queue and Rate Limiter Telemetry
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(VaultTheme.subCardBg)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Queue, contentDescription = "Queue", tint = TealAccent, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Async Queue Depth: ${metrics?.activeQueueDepth ?: 0} payloads",
                        fontSize = 10.sp,
                        color = VaultTheme.textPrimary
                    )
                }
                Text(
                    text = "Total Handled: ${metrics?.totalRequestsProcessed ?: 0} reqs",
                    fontSize = 10.sp,
                    color = VaultTheme.textSecondary
                )
            }
        }
    }
}

@Composable
fun AdminKpiMetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = VaultTheme.cardBg),
        border = BorderStroke(1.dp, VaultTheme.cardBorder),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = VaultTheme.textSecondary, letterSpacing = 0.5.sp)
                Icon(imageVector = icon, contentDescription = title, tint = accentColor, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = VaultTheme.textPrimary)
            Text(subtitle, fontSize = 9.sp, color = VaultTheme.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun ServerUserAuditCard(
    user: ServerUserRecord,
    onInspect: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()) }
    val formattedCreated = remember(user.createdAt) { dateFormat.format(Date(user.createdAt)) }
    val formattedLastLogin = remember(user.lastLoginAt) { dateFormat.format(Date(user.lastLoginAt)) }

    Card(
        colors = CardDefaults.cardColors(containerColor = VaultTheme.cardBg),
        border = BorderStroke(1.dp, VaultTheme.cardBorder),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: User identity & role badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                when (user.role.uppercase()) {
                                    "ADMIN" -> Color(0xFF7C3AED)
                                    "SPOUSE" -> TealAccent
                                    else -> SlatePrimary
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = user.fullName.take(1).uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(user.fullName, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = VaultTheme.textPrimary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        when (user.role.uppercase()) {
                                            "ADMIN" -> Color(0xFF7C3AED).copy(alpha = 0.15f)
                                            "SPOUSE" -> TealAccent.copy(alpha = 0.15f)
                                            else -> Color(0xFF0284C7).copy(alpha = 0.15f)
                                        }
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = user.role.uppercase(),
                                    color = when (user.role.uppercase()) {
                                        "ADMIN" -> Color(0xFF7C3AED)
                                        "SPOUSE" -> TealAccent
                                        else -> Color(0xFF0284C7)
                                    },
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Text(user.email, fontSize = 11.sp, color = VaultTheme.textSecondary)
                    }
                }

                // Status Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF10B981).copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF10B981)))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(user.status, color = Color(0xFF10B981), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Divider(color = VaultTheme.cardBorder, thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(8.dp))

            // Details Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("User ID", fontSize = 9.sp, color = VaultTheme.textSecondary)
                    Text(user.userId, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = VaultTheme.textPrimary, fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Phone", fontSize = 9.sp, color = VaultTheme.textSecondary)
                    Text(user.phone, fontSize = 10.sp, color = VaultTheme.textPrimary)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Nominee Binding", fontSize = 9.sp, color = VaultTheme.textSecondary)
                    Text("${user.nomineeName} (${user.nomineeRelation})", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TealAccent)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Registered On", fontSize = 9.sp, color = VaultTheme.textSecondary)
                    Text(formattedCreated, fontSize = 10.sp, color = VaultTheme.textPrimary)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Certificate & JWT info pill
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(VaultTheme.subCardBg)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Key, contentDescription = "Cert", tint = TealAccent, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Cert: ${user.digitalCertificateId.take(18)}...", fontSize = 9.sp, color = VaultTheme.textSecondary, fontFamily = FontFamily.Monospace)
                }
                Text("Last Login: $formattedLastLogin", fontSize = 9.sp, color = VaultTheme.textSecondary)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onInspect,
                    modifier = Modifier.weight(1f).height(36.dp),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, TealAccent)
                ) {
                    Icon(imageVector = Icons.Default.Security, contentDescription = "Inspect", tint = TealAccent, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Inspect Security Payload", fontSize = 11.sp, color = TealAccent, fontWeight = FontWeight.Bold)
                }

                if (user.role != "OWNER" && user.email != "rahul@gmail.com") {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = "Archive", tint = Color(0xFFEF4444))
                    }
                }
            }
        }
    }
}

@Composable
fun ServerUserInspectorDialog(
    user: ServerUserRecord,
    onDismiss: () -> Unit
) {
    AlertDialog(
        containerColor = VaultTheme.dialogBg,
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.Lock, contentDescription = "Security", tint = TealAccent)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "SECURITY PAYLOAD INSPECTOR",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = VaultTheme.textPrimary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Server Database Record for ${user.fullName} (${user.email}):",
                    fontSize = 11.sp,
                    color = VaultTheme.textSecondary
                )

                InspectorField(label = "User UUID", value = user.userId, isMonospace = true)
                InspectorField(label = "Salted Password Hash (SHA-256)", value = user.passwordHash, isMonospace = true)
                InspectorField(label = "Cryptographic Salt", value = user.passwordSalt, isMonospace = true)
                InspectorField(label = "Master MPIN Code", value = "•••• (4-digit verified)", isMonospace = true)
                InspectorField(label = "Digital Certificate ID", value = user.digitalCertificateId, isMonospace = true)
                InspectorField(label = "Device Fingerprint", value = user.deviceFingerprint, isMonospace = true)
                InspectorField(label = "JWT Token Signature", value = if (user.tokenJwt.isNotEmpty()) user.tokenJwt.take(36) + "..." else "Generated on Auth", isMonospace = true)
                InspectorField(label = "Security Question Safeguard", value = "${user.securityQuestion}\n(Answer hash verified)")
                InspectorField(label = "Biometric Enclave Enrolled", value = if (user.isBiometricEnrolled) "YES (Android BiometricPrompt)" else "NO (PIN / Password fallback)")
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = SlatePrimary)
            ) {
                Text("Close Audit Inspector", color = Color.White)
            }
        }
    )
}

@Composable
private fun InspectorField(label: String, value: String, isMonospace: Boolean = false) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(VaultTheme.subCardBg)
            .padding(8.dp)
    ) {
        Text(label, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TealAccent)
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            fontSize = 10.sp,
            color = VaultTheme.textPrimary,
            fontFamily = if (isMonospace) FontFamily.Monospace else FontFamily.Default
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminQuickRegisterUserDialog(
    model: VaultViewModel,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var nominee by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }

    AlertDialog(
        containerColor = VaultTheme.dialogBg,
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "PROVISION NEW SERVER USER",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = VaultTheme.textPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Provision a new enterprise user directly to the server database through the high-concurrency request queue.", fontSize = 11.sp, color = VaultTheme.textSecondary)

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = vaultTextFieldColors()
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = vaultTextFieldColors()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = vaultTextFieldColors()
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = vaultTextFieldColors()
                )
                OutlinedTextField(
                    value = nominee,
                    onValueChange = { nominee = it },
                    label = { Text("Emergency Nominee", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = vaultTextFieldColors()
                )

                if (errorMsg.isNotEmpty()) {
                    Text(errorMsg, color = Color(0xFFEF4444), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank() || email.isBlank() || password.isBlank()) {
                        errorMsg = "Please fill in all required fields."
                    } else {
                        isSaving = true
                        model.registerEnterpriseUser(
                            fullName = name.trim(),
                            email = email.trim(),
                            phone = phone.ifBlank { "+91 99887 76655" },
                            password = password.trim(),
                            mpin = "4321",
                            nomineeName = nominee.ifBlank { "Primary Nominee" },
                            nomineeRelation = "Family",
                            securityQuestion = "What was your first school name?",
                            securityAnswer = "Greenwood",
                            enableBiometrics = false,
                            onResult = { success, msg ->
                                isSaving = false
                                if (success) {
                                    onDismiss()
                                } else {
                                    errorMsg = msg
                                }
                            }
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = TealAccent)
            ) {
                Text(if (isSaving) "Queuing..." else "Create Account", color = SlatePrimary, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
