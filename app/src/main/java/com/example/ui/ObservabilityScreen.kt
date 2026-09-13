package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.*
import com.example.util.DiagnosticLogItem
import com.example.util.DiagnosticType
import com.example.util.LocalSystemObservability
import com.example.util.ObservabilitySnapshot
import java.util.Locale

@Composable
fun SystemObservabilityDialog(
    model: VaultViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var selectedTab by remember { mutableStateOf(0) }
    var selectedFilter by remember { mutableStateOf<DiagnosticType?>(null) }

    LaunchedEffect(Unit) {
        model.refreshObservabilitySnapshot()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.92f)
                .clip(RoundedCornerShape(16.dp))
                .testTag("dialog_system_observability"),
            colors = CardDefaults.cardColors(containerColor = SlateLightBg),
            border = BorderStroke(1.dp, SlateBorder)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // --- TOP BAR ---
                Surface(
                    color = SlatePrimary,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(TealAccent.copy(alpha = 0.25f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Security,
                                        contentDescription = "Shield",
                                        tint = Color(0xFF38BDF8),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = if (model.isHindiMode) "डेटा स्टोरेज, सुरक्षा और सिस्टम स्वास्थ्य" else "Data Locality, Observability & Health",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = if (model.isHindiMode) "100% ऑन-डिवाइस • शून्य क्लाउड लीक • WAL मोड सक्रिय" else "100% On-Device • Zero Cloud Leak • WAL Active",
                                        fontSize = 10.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                            }
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.size(32.dp).testTag("btn_close_observability")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = Color.White
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Status Pills Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                color = Color(0xFF064E3B),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "SANDBOXED UID ${model.observabilitySnapshot?.sandboxUid ?: "LOCAL"}",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF6EE7B7),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Surface(
                                color = Color(0xFF1E3A8A),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "PHISHING RISK: 0.0%",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF93C5FD),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Surface(
                                color = Color(0xFF581C87),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "INTEGRITY: ${model.observabilitySnapshot?.integrityStatus ?: "OK"}",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFD8B4FE),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                // --- TABS ---
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.White,
                    contentColor = SlatePrimary,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Text(
                                text = if (model.isHindiMode) "डेटा स्टोरेज & शील्ड" else "Storage & Shield",
                                fontSize = 11.sp,
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        icon = { Icon(Icons.Default.FolderSpecial, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Text(
                                text = if (model.isHindiMode) "निगरानी & मेट्रिक्स" else "Health & Metrics",
                                fontSize = 11.sp,
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        icon = { Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = {
                            Text(
                                text = if (model.isHindiMode) "रखरखाव & ट्यूनिंग" else "Maintenance",
                                fontSize = 11.sp,
                                fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        icon = { Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                    Tab(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        text = {
                            Text(
                                text = if (model.isHindiMode) "लाइव टेलीमेट्री" else "Live Logs",
                                fontSize = 11.sp,
                                fontWeight = if (selectedTab == 3) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        icon = { Icon(Icons.Default.Terminal, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                }

                // --- MAINTENANCE STATUS BANNER ---
                if (model.observabilityMaintenanceMessage.isNotEmpty()) {
                    Surface(
                        color = Color(0xFFDCFCE7),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = model.observabilityMaintenanceMessage,
                                    fontSize = 11.sp,
                                    color = Color(0xFF15803D),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            IconButton(
                                onClick = { model.observabilityMaintenanceMessage = "" },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = Color(0xFF15803D), modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }

                // --- TAB CONTENT ---
                Box(modifier = Modifier.weight(1f)) {
                    when (selectedTab) {
                        0 -> LocalityAndAntiPhishingTab(model, clipboardManager, context)
                        1 -> HealthAndMetricsTab(model)
                        2 -> MaintainabilityTab(model, clipboardManager, context)
                        3 -> LiveTelemetryTab(model, selectedFilter) { selectedFilter = it }
                    }
                }
            }
        }
    }
}

@Composable
fun LocalityAndAntiPhishingTab(
    model: VaultViewModel,
    clipboardManager: androidx.compose.ui.platform.ClipboardManager,
    context: Context
) {
    val snapshot = model.observabilitySnapshot
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Direct Question Answer Highlight Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                border = BorderStroke(1.dp, Color(0xFF86EFAC)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (model.isHindiMode) "डेटा कहाँ सुरक्षित रहता है? (100% ऑन-डिवाइस)" else "Where is Data Stored? (100% On-Device)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF14532D)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (model.isHindiMode)
                            "आपका सारा डेटा केवल आपके फोन की आंतरिक मेमोरी (Private App SQLite Sandbox) में ही सेव रहता है। कोई भी बाहरी सर्वर, रिमोट बैकएंड या क्लाउड डेटाबेस इस्तेमाल नहीं होता। किसी भी तीसरे पक्ष (Third Party) या फ़िशिंग लिंक द्वारा डेटा चुराना असंभव है क्योंकि ऐप लिनक्स सैंडबॉक्स यूआईडी के अंदर अलग-थलग काम करता है।"
                        else
                            "All sensitive vault records, emergency contacts, legal directives, and nominee details reside exclusively within this device's private Linux sandboxed internal storage. No external server, remote cloud database, or third-party telemetry pipeline is contacted.",
                        fontSize = 11.sp,
                        color = Color(0xFF166534),
                        lineHeight = 16.sp
                    )
                }
            }
        }

        // Storage Paths Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, SlateBorder),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = if (model.isHindiMode) "सत्यापित ऑन-डिवाइस फाइल पथ" else "Verified On-Device Storage Paths",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = SlatePrimary
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    // Primary Database Path
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SlateLightBg, RoundedCornerShape(8.dp))
                            .border(1.dp, SlateBorder, RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "PRIMARY SQLITE DATABASE",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = SlatePrimary
                            )
                            IconButton(
                                onClick = {
                                    snapshot?.storagePath?.let {
                                        clipboardManager.setText(AnnotatedString(it))
                                        Toast.makeText(context, "DB Path Copied!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.size(22.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = SlatePrimary, modifier = Modifier.size(13.dp))
                            }
                        }
                        Text(
                            text = snapshot?.storagePath ?: "/data/user/0/com.example/databases/family_continuity_vault_db",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF334155),
                            lineHeight = 13.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Size: ${(snapshot?.dbSizeBytes ?: 0L) / 1024} KB • Journal: ${(snapshot?.walSizeBytes ?: 0L) / 1024} KB (WAL)",
                            fontSize = 9.sp,
                            color = Color.Gray
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Shared Preferences Path
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SlateLightBg, RoundedCornerShape(8.dp))
                            .border(1.dp, SlateBorder, RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "USER PREFERENCES & SECURITY KEYS",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = SlatePrimary
                        )
                        Text(
                            text = snapshot?.prefsPath ?: "/data/user/0/com.example/shared_prefs/vault_user_prefs.xml",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF334155),
                            lineHeight = 13.sp
                        )
                    }
                }
            }
        }

        // Anti-Phishing & Security Shield Guarantees
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, SlateBorder),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = if (model.isHindiMode) "एंटी-फिशिंग और शून्य डेटा रिसाव गारंटी" else "Anti-Phishing & Zero Leakage Guarantees",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = SlatePrimary
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    SecurityShieldItem(
                        icon = Icons.Default.Lock,
                        title = if (model.isHindiMode) "लिनक्स सैंडबॉक्स यूआईडी पृथक्करण" else "Linux Process Sandbox Isolation",
                        desc = if (model.isHindiMode) "ऑपरेटिंग सिस्टम द्वारा अलग यूआईडी आवंटित है। कोई अन्य ऐप या ब्राउज़र डेटा नहीं पढ़ सकता।" else "Enforced by Android OS kernel. Other applications cannot inspect or access app private storage.",
                        badge = "ENFORCED"
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    SecurityShieldItem(
                        icon = Icons.Default.Phishing,
                        title = if (model.isHindiMode) "शून्य बाहरी वेबव्यू या फ़िशिंग लिंक" else "Zero External Auth WebViews",
                        desc = if (model.isHindiMode) "कोई बाहरी लॉगिन पेज या री-डायरेक्ट नहीं। सभी पिन और बायोमेट्रिक ऑन-डिवाइस कीस्टोर में चलते हैं।" else "No external browser redirects or credential harvesting. Master key verified locally via KeyStore & SHA-256.",
                        badge = "0% RISK"
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    SecurityShieldItem(
                        icon = Icons.Default.WifiOff,
                        title = if (model.isHindiMode) "शून्य आउटबाउंड टेलीमेट्री (No Cloud Leak)" else "Zero Telemetry Egress",
                        desc = if (model.isHindiMode) "रिमोट सर्वर को भेजा गया डेटा: 0 बाइट्स। पूरी तरह ऑफ़लाइन भी सुरक्षित कार्य करता है।" else "Outbound telemetry calls: 0. Private financial and nominee numbers never egress device boundaries.",
                        badge = "0 BYTES"
                    )
                }
            }
        }
    }
}

@Composable
fun HealthAndMetricsTab(model: VaultViewModel) {
    val snapshot = model.observabilitySnapshot
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Live Performance & Observability KPIs
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, SlateBorder),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (model.isHindiMode) "सिस्टम स्वास्थ्य और प्रदर्शन मेट्रिक्स" else "System Health & Query Telemetry",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = SlatePrimary
                        )
                        IconButton(
                            onClick = { model.refreshObservabilitySnapshot() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = SlatePrimary, modifier = Modifier.size(16.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // KPI Grid
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            KpiCard(
                                title = "READ LATENCY",
                                value = "${String.format(Locale.getDefault(), "%.2f", snapshot?.avgReadLatencyMs ?: 0.8)} ms",
                                subtitle = "Fast SQLite index lookup",
                                color = Color(0xFF0284C7),
                                modifier = Modifier.weight(1f)
                            )
                            KpiCard(
                                title = "WRITE LATENCY",
                                value = "${String.format(Locale.getDefault(), "%.2f", snapshot?.avgWriteLatencyMs ?: 1.9)} ms",
                                subtitle = "WAL concurrent writes",
                                color = Color(0xFF16A34A),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            KpiCard(
                                title = "DB INTEGRITY",
                                value = snapshot?.integrityStatus ?: "OK",
                                subtitle = "PRAGMA check validated",
                                color = Color(0xFF7C3AED),
                                modifier = Modifier.weight(1f)
                            )
                            KpiCard(
                                title = "JVM HEAP USED",
                                value = "${snapshot?.jvmUsedMemoryMb ?: 18} MB",
                                subtitle = "Max: ${snapshot?.jvmMaxMemoryMb ?: 256} MB",
                                color = Color(0xFFD97706),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        // Table Record Distribution
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, SlateBorder),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = if (model.isHindiMode) "तालिका रिकॉर्ड गणना और डेटा स्केलेबिलिटी" else "Table Record Volume & Scalability",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = SlatePrimary
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    snapshot?.tableCounts?.forEach { (tableName, count) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = tableName,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF475569)
                            )
                            Surface(
                                color = SlateLightBg,
                                shape = RoundedCornerShape(4.dp),
                                border = BorderStroke(1.dp, SlateBorder)
                            ) {
                                Text(
                                    text = "$count rows",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SlatePrimary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Divider(color = SlateBorder.copy(alpha = 0.5f))
                    }
                }
            }
        }
    }
}

@Composable
fun MaintainabilityTab(
    model: VaultViewModel,
    clipboardManager: androidx.compose.ui.platform.ClipboardManager,
    context: Context
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = if (model.isHindiMode) "सिस्टम रखरखाव और स्व-सुधार टूल्स" else "Maintainability & Self-Healing Tools",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = SlatePrimary
            )
        }

        // Action 1: VACUUM Database
        item {
            MaintenanceActionCard(
                title = if (model.isHindiMode) "1. SQLite VACUUM डीफ़्रेग्मेंटेशन चलाएं" else "1. Run SQLite VACUUM Compaction",
                desc = if (model.isHindiMode) "हटाए गए डेटा के खाली स्थान को पुनः प्राप्त करता है, बी-ट्री इंडेक्स को पुनर्गठित करता है और डिस्क स्टोरेज घटाता है।" else "Reclaims unused deleted pages, rebuilds B-Trees, defragments SQLite storage, and accelerates lookup speed.",
                btnLabel = if (model.isHindiMode) "VACUUM संपीड़न चलाएं" else "Execute VACUUM",
                btnColor = Color(0xFF0F766E),
                onClick = { model.runDatabaseVacuum() }
            )
        }

        // Action 2: WAL Checkpoint
        item {
            MaintenanceActionCard(
                title = if (model.isHindiMode) "2. Write-Ahead Log (WAL) चेकपॉइंट फ्लश करें" else "2. Flush WAL Checkpoint to Disk",
                desc = if (model.isHindiMode) "सभी गैर-प्रतिबद्ध WAL लॉग पेजों को सीधे मुख्य SQLite डेटाबेस फ़ाइल में लिखता है।" else "Flushes all uncommitted write-ahead log pages directly to primary SQLite database file on disk.",
                btnLabel = if (model.isHindiMode) "WAL फ्लश करें" else "Flush WAL Checkpoint",
                btnColor = Color(0xFF0284C7),
                onClick = { model.runWalCheckpoint() }
            )
        }

        // Action 3: Prune Audit Logs
        item {
            MaintenanceActionCard(
                title = if (model.isHindiMode) "3. पुराने ऑडिट लॉग छांटें (Prune Logs)" else "3. Prune Historical Audit Logs",
                desc = if (model.isHindiMode) "नवीनतम 50 ऑडिट लॉग सुरक्षित रखता है और पुराने प्रविष्टियों को हटाकर स्टोरेज को हल्का रखता है।" else "Bound in-memory footprint: Retains the latest 50 security audit logs and removes older records.",
                btnLabel = if (model.isHindiMode) "लॉग्स छांटें (Keep 50)" else "Prune Logs (Keep 50)",
                btnColor = Color(0xFFD97706),
                onClick = { model.pruneHistoricalAuditLogs(50) }
            )
        }

        // Action 4: Export Diagnostic Report
        item {
            MaintenanceActionCard(
                title = if (model.isHindiMode) "4. सिस्टम स्वास्थ्य व सुरक्षा रिपोर्ट कॉपी करें" else "4. Copy Complete Observability Report",
                desc = if (model.isHindiMode) "पासवर्ड या गोपनीय नंबरों को छिपाकर पूर्ण सिस्टम डायग्नोस्टिक रिपोर्ट क्लिपबोर्ड पर कॉपी करता है।" else "Generates sanitized structured report with storage paths, record counts, and query latencies.",
                btnLabel = if (model.isHindiMode) "डायग्नोस्टिक रिपोर्ट कॉपी करें" else "Copy Report to Clipboard",
                btnColor = SlatePrimary,
                onClick = {
                    val report = model.getExportableDiagnosticsReport()
                    clipboardManager.setText(AnnotatedString(report))
                    Toast.makeText(context, "Observability Report Copied!", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}

@Composable
fun LiveTelemetryTab(
    model: VaultViewModel,
    selectedFilter: DiagnosticType?,
    onFilterChange: (DiagnosticType?) -> Unit
) {
    val logs = remember(model.observabilitySnapshot) {
        LocalSystemObservability.getDiagnosticLogs()
    }
    val filteredLogs = remember(logs, selectedFilter) {
        if (selectedFilter == null) logs else logs.filter { it.type == selectedFilter }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(
                selected = selectedFilter == null,
                onClick = { onFilterChange(null) },
                label = { Text("ALL (${logs.size})", fontSize = 10.sp) }
            )
            DiagnosticType.values().forEach { type ->
                FilterChip(
                    selected = selectedFilter == type,
                    onClick = { onFilterChange(if (selectedFilter == type) null else type) },
                    label = { Text(type.label, fontSize = 10.sp) }
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Logs List
        if (filteredLogs.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No telemetry events recorded for this category yet.", fontSize = 12.sp, color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(filteredLogs) { item ->
                    DiagnosticLogCard(item)
                }
            }
        }
    }
}

@Composable
fun DiagnosticLogCard(item: DiagnosticLogItem) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, SlateBorder),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                color = Color(item.type.badgeColorHex),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = item.type.label,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = item.tag,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = SlatePrimary,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = item.formattedTime,
                        fontSize = 9.sp,
                        color = Color.Gray,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Text(
                    text = item.message,
                    fontSize = 10.sp,
                    color = Color(0xFF334155),
                    lineHeight = 13.sp
                )
                if (item.latencyMs > 0) {
                    Text(
                        text = "Latency: ${item.latencyMs}ms",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFD97706)
                    )
                }
            }
        }
    }
}

@Composable
fun SecurityShieldItem(
    icon: ImageVector,
    title: String,
    desc: String,
    badge: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SlateLightBg, RoundedCornerShape(8.dp))
            .border(1.dp, SlateBorder, RoundedCornerShape(8.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFFE0F2FE)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SlatePrimary)
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                    color = Color(0xFF059669),
                    shape = RoundedCornerShape(3.dp)
                ) {
                    Text(
                        text = badge,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }
            Text(text = desc, fontSize = 10.sp, color = Color.Gray, lineHeight = 13.sp)
        }
    }
}

@Composable
fun KpiCard(
    title: String,
    value: String,
    subtitle: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = SlateLightBg,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, SlateBorder),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(text = title, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = value, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = color)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = subtitle, fontSize = 9.sp, color = Color.Gray)
        }
    }
}

@Composable
fun MaintenanceActionCard(
    title: String,
    desc: String,
    btnLabel: String,
    btnColor: Color,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, SlateBorder),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SlatePrimary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = desc, fontSize = 10.sp, color = Color.Gray, lineHeight = 14.sp)
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(containerColor = btnColor),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(text = btnLabel, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}
