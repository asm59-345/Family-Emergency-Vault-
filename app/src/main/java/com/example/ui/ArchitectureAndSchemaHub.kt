package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun ArchitectureAndSchemaHub(model: VaultViewModel) {
    var activeSubTab by remember { mutableStateOf(0) } // 0: App Flow Lifecycle, 1: Database System Schema, 2: Legal Certificate

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, SlateBorder),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "⚡ SYSTEM INTEGRITY & ARCHITECTURE PORTAL",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = SlatePrimary,
                letterSpacing = 0.5.sp
            )
            Text(
                text = "Technical inspection tools for verifying local schema states, emergency release flowcharts, and legal consent stamps.",
                fontSize = 11.sp,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Inner Tab Group
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(SlateLightBg)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val tabsList = listOf("📌 App Flow", "🗄️ DB Schema", "📜 Legal Stamp")
                tabsList.forEachIndexed { idx, label ->
                    val isSelected = activeSubTab == idx
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) SlatePrimary else Color.Transparent)
                            .clickable { activeSubTab = idx }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White else SlatePrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            when (activeSubTab) {
                0 -> FlowLifecycleDiagram(model)
                1 -> DatabaseSchemaDisplay(model)
                2 -> LegalStampDisplay(model)
            }
        }
    }
}

@Composable
fun FlowLifecycleDiagram(model: VaultViewModel) {
    // Determine active flow state to highlight corresponding stages in flow chart!
    val isReleased = model.isEmergencyAccessReleased
    val isWaiting = model.isWaitingPeriodActive
    val isLocked = model.isAppMpinLocked

    val activeStage = when {
        isReleased -> 4 // Released stage
        isWaiting -> 3  // Cooling down stage
        isLocked -> 0   // Locked stage
        else -> 1       // Normal operational stage
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Emergency Security Lifecycle Workflow",
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = SlatePrimary
        )
        Text(
            text = "Highlighted blocks denote active states reactive to the current simulation status.",
            fontSize = 10.sp,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        // Stage 0: Lock Protection
        FlowBlock(
            stageNumber = 0,
            title = "Stage 0: Secure Decrypt Lock (MPIN)",
            description = "At startup, AES-GCM local shared preference decryption sandbox keys are locked behind a user 4-digit code. Inactivity lockers re-engage lock state after 5m.",
            isActive = activeStage == 0,
            icon = Icons.Default.Lock
        )

        VerticalConnector()

        // Stage 1: Active Heartbeat Standby
        FlowBlock(
            stageNumber = 1,
            title = "Stage 1: Owner Operational Heartbeat",
            description = "Owner records, updates, and accesses parameters freely. A background Dead Man's checks frequency. (Interval countdown: ${model.deadManSwitchDaysLeft} days remaining).",
            isActive = activeStage == 1,
            icon = Icons.Default.Sync
        )

        VerticalConnector()

        // Stage 2: Trigger Dormancy / Claim Filed
        FlowBlock(
            stageNumber = 2,
            title = "Stage 2: Heartbeat Exceeded / Heir Access Appeal",
            description = "If checking periods lapse without interaction, or if spouses/executors invoke a hand-over appeal, an Emergency Request is logged inside the Room SQLite register.",
            isActive = activeStage == 2,
            icon = Icons.Default.Warning
        )

        VerticalConnector()

        // Stage 3: Verification Cooling Down Delay
        FlowBlock(
            stageNumber = 3,
            title = "Stage 3: Verification Waiting Period (Deliberated)",
            description = "A standard safety delay (e.g., 48 hours simulated wait) launches. Notifications are dispatched to Owner's emergency networks. Owner may abort and revoke request at any microsecond.",
            isActive = activeStage == 3,
            icon = Icons.Default.HourglassEmpty
        )

        if (isWaiting) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, TealAccent, RoundedCornerShape(8.dp))
                    .background(TealAccent.copy(alpha = 0.05f))
                    .padding(10.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Active Verification Countdown", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TealAccent)
                        Text("SIMULATING COOLDOWN", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TealAccent)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { model.mockApprovalTimerProgress },
                        color = TealAccent,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        VerticalConnector()

        // Stage 4: Release of Sandbox Decryption Coordinates
        FlowBlock(
            stageNumber = 4,
            title = "Stage 4: Secure Sandbox Handover Executed",
            description = "Upon timer lapse without revocation, decryption authority shifts to the approved nominee. Redundant encryptions dissolve, opening checklists and assets for read-only continuity.",
            isActive = activeStage == 4,
            icon = Icons.Default.DoneAll
        )
    }
}

@Composable
fun FlowBlock(
    stageNumber: Int,
    title: String,
    description: String,
    isActive: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) SlatePrimary else SlateLightBg
        ),
        border = BorderStroke(
            width = if (isActive) 2.dp else 1.dp,
            color = if (isActive) TealAccent else SlateBorder
        ),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (isActive) TealAccent else Color.White)
                    .border(1.dp, if (isActive) TealAccent else SlateBorder, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = if (isActive) SlatePrimary else Color.Gray,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = if (isActive) Color.White else SlatePrimary
                )
                Text(
                    text = description,
                    fontSize = 10.sp,
                    color = if (isActive) Color.White.copy(alpha = 0.9f) else Color.DarkGray,
                    lineHeight = 13.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            if (isActive) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(TealAccent)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text("ACTIVE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = SlatePrimary)
                }
            }
        }
    }
}

@Composable
fun VerticalConnector() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(14.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(2.dp)
                .fillMaxHeight()
                .background(Color.LightGray)
        )
    }
}

@Composable
fun DatabaseSchemaDisplay(model: VaultViewModel) {
    // Dynamic record count indicators
    val dependentsCount = model.familyDependents.collectAsState(initial = emptyList()).value.size
    val contactsCount = model.importantContacts.collectAsState(initial = emptyList()).value.size
    val vaultItemsCount = model.vaultItems.collectAsState(initial = emptyList()).value.size
    val checklistsCount = model.emergencyActionItems.collectAsState(initial = emptyList()).value.size
    val claimsCount = model.claimRecords.collectAsState(initial = emptyList()).value.size
    val requestsCount = model.emergencyAccessRequests.collectAsState(initial = emptyList()).value.size
    val logsCount = model.auditLogs.collectAsState(initial = emptyList()).value.size
    val localSecureCount = model.localSecureContacts.size

    var expandedTableId by remember { mutableStateOf<String?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Active SQLite Database Room Schematics",
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = SlatePrimary
        )
        Text(
            text = "Tap on any table card below to inspect its schema constraints, SQLite column datatypes, primary key definitions, and active record count.",
            fontSize = 10.sp,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        // Table 1: family_dependents
        SchemaTableItem(
            tableName = "family_dependents",
            description = "Tracks dependent profiles, relations, blood types, and physical guardian files.",
            recordCount = dependentsCount,
            isExpanded = expandedTableId == "family_dependents",
            onToggle = { expandedTableId = if (expandedTableId == "family_dependents") null else "family_dependents" },
            columns = listOf(
                "id" to "INTEGER PRIMARY KEY (Auto)",
                "fullName" to "TEXT (NOT NULL)",
                "relation" to "TEXT",
                "dob" to "TEXT",
                "bloodGroup" to "TEXT",
                "mobile" to "TEXT",
                "email" to "TEXT",
                "address" to "TEXT",
                "dependentStatus" to "TEXT (Guardian/Dependent)",
                "guardianDetails" to "TEXT",
                "notes" to "TEXT"
            )
        )

        // Table 2: important_contacts
        SchemaTableItem(
            tableName = "important_contacts",
            description = "Contains trusted support coordinates (advisors, lawyers, doctors, CA ledger networks).",
            recordCount = contactsCount,
            isExpanded = expandedTableId == "important_contacts",
            onToggle = { expandedTableId = if (expandedTableId == "important_contacts") null else "important_contacts" },
            columns = listOf(
                "id" to "INTEGER PRIMARY KEY (Auto)",
                "contactName" to "TEXT (NOT NULL)",
                "category" to "TEXT (Advisor/Lawyer/HR)",
                "phone" to "TEXT",
                "email" to "TEXT",
                "address" to "TEXT",
                "priority" to "TEXT (P1 / P2)",
                "remarks" to "TEXT"
            )
        )

        // Table 3: vault_items
        SchemaTableItem(
            tableName = "vault_items",
            description = "Primary secure credential vault. Houses institutional details, accounts, masked records, and physical/digital coordinates.",
            recordCount = vaultItemsCount,
            isExpanded = expandedTableId == "vault_items",
            onToggle = { expandedTableId = if (expandedTableId == "vault_items") null else "vault_items" },
            columns = listOf(
                "id" to "INTEGER PRIMARY KEY (Auto)",
                "category" to "TEXT (BANK/INSURANCE/PROP)",
                "title" to "TEXT (Holder Label)",
                "ownerName" to "TEXT (Holder full)",
                "institution" to "TEXT (SBI/LIC/EPFO)",
                "numberOrId" to "TEXT (Masked parameter)",
                "nomineeName" to "TEXT",
                "nomineeRelation" to "TEXT",
                "nomineeVerified" to "INTEGER (Boolean flag)",
                "physicalLocation" to "TEXT (Locker Binder)",
                "digitalLocation" to "TEXT (Secure URL/Drive)",
                "status" to "TEXT (Active/Inactive)",
                "isMasked" to "INTEGER (Boolean filter)",
                "remarks" to "TEXT",
                "detailsString" to "TEXT (JSON Custom payload)",
                "lastUpdated" to "INTEGER (Epoch timestamp)"
            )
        )

        // Table 4: emergency_action_items
        SchemaTableItem(
            tableName = "emergency_action_items",
            description = "Chronological checklist phases mapping critical tasks to 24 Hours, 7 Days, and 30 Days segments.",
            recordCount = checklistsCount,
            isExpanded = expandedTableId == "emergency_action_items",
            onToggle = { expandedTableId = if (expandedTableId == "emergency_action_items") null else "emergency_action_items" },
            columns = listOf(
                "id" to "INTEGER PRIMARY KEY (Auto)",
                "phase" to "TEXT (T+24H / T+7D / T+30D)",
                "taskName" to "TEXT",
                "instructions" to "TEXT (Rich documentation)",
                "completed" to "INTEGER (Boolean state)",
                "updatedBy" to "TEXT (Operator ID)",
                "lastUpdated" to "INTEGER (Epoch)"
            )
        )

        // Table 5: claim_records
        SchemaTableItem(
            tableName = "claim_records",
            description = "Tracks inheritance status, assigned executors, required certificate attachments, and timelines.",
            recordCount = claimsCount,
            isExpanded = expandedTableId == "claim_records",
            onToggle = { expandedTableId = if (expandedTableId == "claim_records") null else "claim_records" },
            columns = listOf(
                "id" to "INTEGER PRIMARY KEY (Auto)",
                "itemType" to "TEXT (Bank transfer/Insurance)",
                "institution" to "TEXT",
                "status" to "TEXT (Review/Completed)",
                "assignedPerson" to "TEXT",
                "pendingDocuments" to "TEXT (Claim Form 12A)",
                "expectedTimeline" to "TEXT",
                "notes" to "TEXT",
                "lastUpdated" to "INTEGER (Epoch)"
            )
        )

        // Table 6: emergency_requests
        SchemaTableItem(
            tableName = "emergency_requests",
            description = "Audit trail for access appeals, waiting delay approvals, and release approvals.",
            recordCount = requestsCount,
            isExpanded = expandedTableId == "emergency_requests",
            onToggle = { expandedTableId = if (expandedTableId == "emergency_requests") null else "emergency_requests" },
            columns = listOf(
                "id" to "INTEGER PRIMARY KEY (Auto)",
                "userName" to "TEXT",
                "relation" to "TEXT",
                "requestReason" to "TEXT",
                "requestTime" to "INTEGER (Epoch stamp)",
                "delayHours" to "INTEGER (Default 48h)",
                "status" to "TEXT (Pending/Active/Rejected)",
                "approvedTime" to "INTEGER"
            )
        )

        // Table 7: audit_logs
        SchemaTableItem(
            tableName = "audit_logs",
            description = "Strict security compliance ledger recording logins, manual unlocks, card views, edits, and exports.",
            recordCount = logsCount,
            isExpanded = expandedTableId == "audit_logs",
            onToggle = { expandedTableId = if (expandedTableId == "audit_logs") null else "audit_logs" },
            columns = listOf(
                "id" to "INTEGER PRIMARY KEY (Auto GP)",
                "timestamp" to "INTEGER (Epoch NOT NULL)",
                "action" to "TEXT (Vault View / Decrypt)",
                "details" to "TEXT (Details payload)",
                "userRole" to "TEXT (Operating Persona)"
            )
        )

        // Table 8: local_secure_contacts (AES-GCM Sandbox)
        SchemaTableItem(
            tableName = "contacts_list (AES-GCM PREFERENCE SANDBOX)",
            description = "Completely isolated local ledger decoupled from SQLite databases. Keys are generated dynamically. Fully cipher-encoded block.",
            recordCount = localSecureCount,
            isExpanded = expandedTableId == "contacts_list_encrypted",
            onToggle = { expandedTableId = if (expandedTableId == "contacts_list_encrypted") null else "contacts_list_encrypted" },
            columns = listOf(
                "id" to "TEXT UUID (Non-predictable)",
                "name" to "CIPHER TEXT (AES-GCM-128)",
                "relationship" to "CIPHER TEXT (AES-GCM-128)",
                "phone" to "CIPHER TEXT (AES-GCM-128)",
                "altPhone" to "CIPHER TEXT (AES-GCM-128)",
                "notes" to "CIPHER TEXT (AES-GCM-128)"
            ),
            isEncryptedTable = true
        )
    }
}

@Composable
fun SchemaTableItem(
    tableName: String,
    description: String,
    recordCount: Int,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    columns: List<Pair<String, String>>,
    isEncryptedTable: Boolean = false
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SlateLightBg),
        border = BorderStroke(1.dp, SlateBorder),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isEncryptedTable) Icons.Default.EnhancedEncryption else Icons.Default.TableView,
                    contentDescription = "Database icon",
                    tint = if (isEncryptedTable) TealAccent else SlatePrimary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = tableName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    color = SlatePrimary,
                    modifier = Modifier.weight(1f)
                )

                // Record bubble
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isEncryptedTable) TealAccent else SlatePrimary)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "$recordCount rows",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isEncryptedTable) SlatePrimary else Color.White
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = "Expand toggle",
                    tint = Color.Gray,
                    modifier = Modifier.size(16.dp)
                )
            }

            Text(
                text = description,
                fontSize = 10.sp,
                color = Color.DarkGray,
                modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
            )

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.White)
                        .border(1.dp, SlateBorder, RoundedCornerShape(6.dp))
                        .padding(8.dp)
                ) {
                    Text(
                        text = "COLUMN MAP & SCHEMA ATTRIBUTES:",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    columns.forEach { (colName, colType) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "•  $colName",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = SlatePrimary
                            )
                            Text(
                                text = colType,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = if (colType.contains("PRIMARY")) TealAccent else Color.DarkGray
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LegalStampDisplay(model: VaultViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Active Decryption Consent stamp Certificate",
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = SlatePrimary
        )

        if (model.isTermsAccepted) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, TealAccent, RoundedCornerShape(8.dp))
                    .background(TealAccent.copy(alpha = 0.05f))
                    .padding(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = "Verified Icon",
                            tint = TealAccent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "OFFLINE COMPLIANCE STAMP VERIFIED",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TealAccent
                        )
                    }

                    Divider(color = TealAccent.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 4.dp))

                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Certified Signatory:", fontSize = 11.sp, color = Color.Gray)
                        Text(model.consentName, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SlatePrimary)
                    }

                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Certified Owner Email:", fontSize = 11.sp, color = Color.Gray)
                        Text(model.consentEmail, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SlatePrimary)
                    }

                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Consent Reference ID:", fontSize = 11.sp, color = Color.Gray)
                        Text(
                            text = model.consentSignature,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = SlatePrimary
                        )
                    }

                    Divider(color = TealAccent.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 4.dp))

                    Text(
                        text = "This legal stamp is generated dynamically using localized application clock records and is cryptographically tied to seed signatures. To clear stored preferences and revoke system consent, press below.",
                        fontSize = 10.sp,
                        color = Color.DarkGray,
                        lineHeight = 13.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    OutlinedButton(
                        onClick = { model.revokeTerms() },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = RedAlert),
                        border = BorderStroke(1.dp, RedAlert.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.RemoveCircleOutline, contentDescription = "Revoke Icon", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Revoke Acceptance Stamp & Format Sandbox", fontSize = 11.sp)
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, Color.Gray.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .background(Color.White)
                    .padding(14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Legal stamp has been cleared or revoked. Register via onboarding to decrypt the portal.",
                    color = Color.DarkGray,
                    textAlign = TextAlign.Center,
                    fontSize = 11.sp
                )
            }
        }
    }
}
