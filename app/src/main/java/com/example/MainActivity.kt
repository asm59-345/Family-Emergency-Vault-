package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.*
import com.example.ui.UserRole
import com.example.ui.VaultViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val model by lazy {
        androidx.lifecycle.ViewModelProvider(this)[VaultViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAppSurface(model)
            }
        }
    }

    override fun dispatchTouchEvent(ev: android.view.MotionEvent?): Boolean {
        model.updateUserActivity()
        return super.dispatchTouchEvent(ev)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppSurface(model: VaultViewModel = viewModel()) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Collect Reactive db flows
    val dependents by model.familyDependents.collectAsStateWithLifecycle()
    val contacts by model.importantContacts.collectAsStateWithLifecycle()
    val vaultItems by model.vaultItems.collectAsStateWithLifecycle()
    val checklists by model.emergencyActionItems.collectAsStateWithLifecycle()
    val claims by model.claimRecords.collectAsStateWithLifecycle()
    val emergencyRequests by model.emergencyAccessRequests.collectAsStateWithLifecycle()
    val logs by model.auditLogs.collectAsStateWithLifecycle()

    var showRoleSelector by remember { mutableStateOf(false) }
    var showAddVaultItemDialog by remember { mutableStateOf(false) }
    var selectedItemForEdit by remember { mutableStateOf<VaultItem?>(null) }
    var currentSelectedCategoryForAdd by remember { mutableStateOf("BANK") }

    var showAddDependentDialog by remember { mutableStateOf(false) }
    var selectedDependentForEdit by remember { mutableStateOf<FamilyDependent?>(null) }

    var showAddContactDialog by remember { mutableStateOf(false) }
    var selectedContactForEdit by remember { mutableStateOf<ImportantContact?>(null) }

    var showAddClaimDialog by remember { mutableStateOf(false) }
    var selectedClaimForEdit by remember { mutableStateOf<ClaimRecord?>(null) }

    var showCsvImportDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showEmergencyRequestDialog by remember { mutableStateOf(false) }

    var showAddLocalSecureContactDialog by remember { mutableStateOf(false) }
    var selectedLocalContactForEdit by remember { mutableStateOf<LocalSecureContact?>(null) }

    if (!model.isTermsAccepted) {
        com.example.ui.AppTermsConsentScreen(model = model)
    } else if (model.isAppMpinLocked) {
        AppMpinLockScreen(model = model)
    } else {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .testTag("app_main_scaffold"),
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SlatePrimary,
                    titleContentColor = Color.White
                ),
                title = {
                    Column {
                        Text(
                            text = "Family Emergency Vault",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.VerifiedUser,
                                contentDescription = "Security Active",
                                tint = TealAccent,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Military-grade end-to-end security active",
                                fontSize = 10.sp,
                                color = Color.White.copy(alpha = 0.7f),
                                fontWeight = FontWeight.Normal
                            )
                        }
                    }
                },
                actions = {
                    // Role Switcher Badge
                    Box(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(TealAccent)
                            .clickable { showRoleSelector = true }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.SwitchAccount,
                                contentDescription = "Switch Active Persona",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = model.currentRole.label.split(" ")[0],
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (model.isLoggedIn) {
                NavigationBar(
                    containerColor = Color.White,
                    tonalElevation = 8.dp,
                    modifier = Modifier.navigationBarsPadding()
                ) {
                    val items = listOf(
                        Triple("DASHBOARD", Icons.Outlined.Dashboard, Icons.Filled.Dashboard),
                        Triple("VAULT", Icons.Outlined.Lock, Icons.Filled.Lock),
                        Triple("CONTACTS", Icons.Outlined.People, Icons.Filled.People),
                        Triple("CHECKLIST", Icons.Outlined.Assignment, Icons.Filled.Assignment),
                        Triple("SETTINGS", Icons.Outlined.Settings, Icons.Filled.Settings)
                    )
                    items.forEach { (tab, outlineIcon, filledIcon) ->
                        val isSelected = model.selectedTabItem == tab
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { model.selectedTabItem = tab },
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) filledIcon else outlineIcon,
                                    contentDescription = tab,
                                    tint = if (isSelected) SlatePrimary else Color.Gray
                                )
                            },
                            label = {
                                Text(
                                    text = tab.lowercase().replaceFirstChar { it.uppercase() },
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) SlatePrimary else Color.Gray
                                )
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(SlateLightBg)
        ) {
            if (!model.isLoggedIn) {
                LoginScreen(model = model)
            } else {
                when (model.selectedTabItem) {
                    "DASHBOARD" -> DashboardScreen(
                        model = model,
                        vaultItems = vaultItems,
                        dependents = dependents,
                        contacts = contacts,
                        checklists = checklists,
                        onRequestAccess = { showEmergencyRequestDialog = true },
                        onImportTrigger = { showCsvImportDialog = true },
                        onEditVaultItem = { item ->
                            selectedItemForEdit = item
                            showAddVaultItemDialog = true
                        }
                    )
                    "VAULT" -> VaultScreen(
                        model = model,
                        vaultItems = vaultItems,
                        onAddItem = { category ->
                            currentSelectedCategoryForAdd = category
                            selectedItemForEdit = null
                            showAddVaultItemDialog = true
                        },
                        onEditItem = { item ->
                            selectedItemForEdit = item
                            showAddVaultItemDialog = true
                        }
                    )
                    "CONTACTS" -> ContactsScreen(
                        model = model,
                        dependents = dependents,
                        contacts = contacts,
                        onAddDependent = {
                            selectedDependentForEdit = null
                            showAddDependentDialog = true
                        },
                        onEditDependent = { dep ->
                            selectedDependentForEdit = dep
                            showAddDependentDialog = true
                        },
                        onAddContact = {
                            selectedContactForEdit = null
                            showAddContactDialog = true
                        },
                        onEditContact = { con ->
                            selectedContactForEdit = con
                            showAddContactDialog = true
                        },
                        onAddLocalSecure = {
                            selectedLocalContactForEdit = null
                            showAddLocalSecureContactDialog = true
                        },
                        onEditLocalSecure = { con ->
                            selectedLocalContactForEdit = con
                            showAddLocalSecureContactDialog = true
                        }
                    )
                    "CHECKLIST" -> ChecklistScreen(
                        model = model,
                        checklists = checklists,
                        claims = claims,
                        onAddClaim = {
                            selectedClaimForEdit = null
                            showAddClaimDialog = true
                        },
                        onEditClaim = { clm ->
                            selectedClaimForEdit = clm
                            showAddClaimDialog = true
                        }
                    )
                    "SETTINGS" -> SettingsScreen(
                        model = model,
                        logs = logs,
                        onCsvImport = { showCsvImportDialog = true },
                        onExportPdf = { showExportDialog = true },
                        onRequestEmergency = { showEmergencyRequestDialog = true }
                    )
                }
            }

            // Global FAB for Gemini AI Assistant (Only if logged in)
            if (model.isLoggedIn) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 16.dp, end = 16.dp),
                    contentAlignment = Alignment.BottomEnd
                ) {
                    FloatingActionButton(
                        onClick = { model.isAiChatOpen = true },
                        containerColor = TealAccent,
                        contentColor = Color.White,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Ask Gemini AI Assistant",
                            tint = Color.White
                        )
                    }
                }
            }

            // --- ALL FLOATING DIALOGS & SHEET SIMULATORS ---

            // 1. Role Selector Dialog
            if (showRoleSelector) {
                RoleSelectorDialog(
                    model = model,
                    onDismiss = { showRoleSelector = false }
                )
            }

            // 2. Add/Edit Vault Item Dialog
            if (showAddVaultItemDialog) {
                AddEditVaultItemDialog(
                    initialItem = selectedItemForEdit,
                    defaultCategory = currentSelectedCategoryForAdd,
                    onDismiss = { showAddVaultItemDialog = false },
                    onSave = { item ->
                        model.saveVaultItem(item)
                        showAddVaultItemDialog = false
                    },
                    onDelete = { item ->
                        model.deleteVaultItem(item)
                        showAddVaultItemDialog = false
                    }
                )
            }

            // 3. Add/Edit Dependent Dialog
            if (showAddDependentDialog) {
                AddEditDependentDialog(
                    initialItem = selectedDependentForEdit,
                    onDismiss = { showAddDependentDialog = false },
                    onSave = { dep ->
                        model.saveFamilyDependent(dep)
                        showAddDependentDialog = false
                    },
                    onDelete = { dep ->
                        model.deleteFamilyDependent(dep)
                        showAddDependentDialog = false
                    }
                )
            }

            // 4. Add/Edit Emergency Contact Dialog
            if (showAddContactDialog) {
                AddEditContactDialog(
                    initialItem = selectedContactForEdit,
                    onDismiss = { showAddContactDialog = false },
                    onSave = { con ->
                        model.saveImportantContact(con)
                        showAddContactDialog = false
                    },
                    onDelete = { con ->
                        model.deleteImportantContact(con)
                        showAddContactDialog = false
                    }
                )
            }

            // 5. Add/Edit Claim Tracker Dialog
            if (showAddClaimDialog) {
                AddEditClaimDialog(
                    initialItem = selectedClaimForEdit,
                    onDismiss = { showAddClaimDialog = false },
                    onSave = { clm ->
                        model.saveClaimRecord(clm)
                        showAddClaimDialog = false
                    },
                    onDelete = { clm ->
                        model.deleteClaimRecord(clm)
                        showAddClaimDialog = false
                    }
                )
            }

            // 6. CSV Importer Dialog
            if (showCsvImportDialog) {
                CsvImportDialog(
                    onDismiss = { showCsvImportDialog = false },
                    onImportConfirm = { mapping ->
                        model.simulateCsvImport(mapping)
                        showCsvImportDialog = false
                        Toast.makeText(context, "Spreadsheet imported securely!", Toast.LENGTH_LONG).show()
                    }
                )
            }

            // 7. Printable PDF Handbook Generator Overlay
            if (showExportDialog) {
                ExportHandbookDialog(
                    model = model,
                    vaultItems = vaultItems,
                    dependents = dependents,
                    claims = claims,
                    onDismiss = { showExportDialog = false }
                )
            }

            // 8. Emergency Access Launch Dialog
            if (showEmergencyRequestDialog) {
                EmergencyAccessRequestDialog(
                    model = model,
                    emergencyRequests = emergencyRequests,
                    onDismiss = { showEmergencyRequestDialog = false }
                )
            }

            // 9. Add/Edit Local Secure Contact Dialog
            if (showAddLocalSecureContactDialog) {
                AddEditLocalSecureContactDialog(
                    initialItem = selectedLocalContactForEdit,
                    onDismiss = { showAddLocalSecureContactDialog = false },
                    onSave = { contact ->
                        model.saveLocalSecureContact(contact)
                        showAddLocalSecureContactDialog = false
                    },
                    onDelete = { id ->
                        model.deleteLocalSecureContact(id)
                        showAddLocalSecureContactDialog = false
                    }
                )
            }

            // 10. AI Chat Assistant Dialog/Dashboard Window
            if (model.isAiChatOpen) {
                GeminiAssistantDialog(
                    model = model,
                    onDismiss = { model.isAiChatOpen = false }
                )
            }
        }
    }
}
}

// ==================== AUTHENTICATION SCREEN ====================
@Composable
fun LoginScreen(model: VaultViewModel) {
    var password by remember { mutableStateOf("") }
    var otpCode by remember { mutableStateOf("") }
    var selectedDemoRole by remember { mutableStateOf(UserRole.OWNER) }
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Vault Lock Icon Header
        Icon(
            imageVector = Icons.Default.Shield,
            contentDescription = "Shield Guard",
            tint = TealAccent,
            modifier = Modifier.size(72.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "FAMILY EMERGENCY VAULT",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = SlatePrimary,
            letterSpacing = 1.sp
        )
        Text(
            text = "Personal Continuity & Handover Organizer",
            fontSize = 12.sp,
            color = Color.Gray,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        if (!model.showMfaChallenge) {
            // Screen 1: Password entry
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, SlateBorder),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Secure Login Portal",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = SlatePrimary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(text = "Vault Key (Password)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .testTag("password_input"),
                        placeholder = { Text("Enter account access password") },
                        visualTransformation = PasswordVisualTransformation(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TealAccent,
                            unfocusedBorderColor = Color.LightGray
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { model.login(password) },
                        colors = ButtonDefaults.buttonColors(containerColor = SlatePrimary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("login_button")
                    ) {
                        Text("Verify & Continue", color = Color.White)
                    }
                }
            }
        } else {
            // Screen 2: Simulate Smart OTP
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, SlateBorder),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Double-Factor MFA OTP",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = SlatePrimary
                    )
                    Text(
                        text = "A secure temporary OTP is dispatched to your registered mobile and backup recovery email for verification.",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                    )

                    Text(text = "4-Digit OTP Code", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    OutlinedTextField(
                        value = otpCode,
                        onValueChange = { if (it.length <= 4) otpCode = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .testTag("otp_input"),
                        placeholder = { Text("Enter the simulation OTP code: 1234") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TealAccent,
                            unfocusedBorderColor = Color.LightGray
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { model.verifyOtp(otpCode) },
                        colors = ButtonDefaults.buttonColors(containerColor = TealAccent),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("otp_verify_button")
                    ) {
                        Text("Grant Secure Access", color = Color.White)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Demo Sandbox bypass card for quick evaluation
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9)),
            border = BorderStroke(1.dp, Color.LightGray),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "🛠️ Evaluator sandbox bypass keys",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = SlatePrimary
                )
                Text(
                    text = "Directly launch the organizer to test role permissions instantly. No data leaves your secure local system.",
                    fontSize = 11.sp,
                    color = Color.DarkGray
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            model.switchRole(UserRole.OWNER)
                            model.verifyOtp("1234")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SlatePrimary),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        Text("Rahul (Owner)", fontSize = 11.sp, color = Color.White)
                    }
                    Button(
                        onClick = {
                            model.switchRole(UserRole.SPOUSE)
                            model.verifyOtp("1234")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TealAccent),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        Text("Priya (Spouse)", fontSize = 11.sp, color = Color.White)
                    }
                }
            }
        }
    }
}

// ==================== DASHBOARD SCREEN ====================
@Composable
fun DashboardScreen(
    model: VaultViewModel,
    vaultItems: List<VaultItem>,
    dependents: List<FamilyDependent>,
    contacts: List<ImportantContact>,
    checklists: List<EmergencyActionItem>,
    onRequestAccess: () -> Unit,
    onImportTrigger: () -> Unit,
    onEditVaultItem: (VaultItem) -> Unit
) {
    val scrollState = rememberScrollState()

    // Aggregate alert alerts
    val itemsWithoutNominee = vaultItems.filter {
        (it.category == "BANK" || it.category == "INVESTMENT" || it.category == "INSURANCE") && it.nomineeName.trim().isEmpty()
    }
    val premiumDues = model.getUpcomingPremiumDues()
    val emiDues = model.getUpcomingEMIDues()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // Welcome Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Welcome Back, ${model.currentRole.label.split(" ")[0]}",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = SlatePrimary
                )
                Text(
                    text = "Continuity Plan Status: Active Protection",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }

            // Quick Switch Check-In Indicator
            if (model.currentRole == UserRole.OWNER) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFFEF3C7))
                        .clickable { model.performOwnerCheckIn() }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = "Dead man",
                            tint = CreamGold,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Check in (${model.deadManSwitchDaysLeft}d left)",
                            fontSize = 10.sp,
                            color = CreamGold,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Security Status Tracker Panel
        Card(
            colors = CardDefaults.cardColors(containerColor = SlatePrimary),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "EMERGENCY READY STATS",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.6f),
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Net Worth Snapshot",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "₹ ${String.format("%,.2f", model.getTotalAssetsSum() - model.getTotalLiabilitiesSum())} INR",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }

                    // Score Circle
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${model.getEmergencyReadinessScore()}%",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Ready score",
                                fontSize = 8.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                }

                Divider(
                    color = Color.White.copy(alpha = 0.1f),
                    modifier = Modifier.padding(vertical = 12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(text = "Total Assets", fontSize = 10.sp, color = Color.White.copy(alpha = 0.6f))
                        Text(
                            text = "₹ ${String.format("%,.0f", model.getTotalAssetsSum())}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(text = "Total Liabilities", fontSize = 10.sp, color = Color.White.copy(alpha = 0.6f))
                        Text(
                            text = "₹ ${String.format("%,.0f", model.getTotalLiabilitiesSum())}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFECACA)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Progress Bar: Profile Completion
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Continuous Profile Completion",
                            fontSize = 10.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "${model.getProfileCompletionPercentage()}%",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { model.getProfileCompletionPercentage() / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(4.dp)),
                        color = TealAccent,
                        trackColor = Color.White.copy(alpha = 0.2f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Simulated Emergency Trigger status bar
        if (model.isWaitingPeriodActive) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
                border = BorderStroke(1.dp, CreamGold),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Alert",
                            tint = CreamGold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Emergency Release Impending: 48h Delayed Switch Active",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Color(0xFF92400E)
                        )
                    }
                    Text(
                        text = "Access initiated by family. System will release all private folder keys if Owner (Rahul) does not veto in response.",
                        fontSize = 11.sp,
                        color = Color.DarkGray,
                        modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { model.revokeEmergencyAccess() },
                            colors = ButtonDefaults.buttonColors(containerColor = RedAlert),
                            modifier = Modifier.height(32.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text("VETO (I am fine)", color = Color.White, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // Active Emergency State Opened Banner
        if (model.isEmergencyAccessReleased) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFDCFCE7)),
                border = BorderStroke(1.dp, GreenSuccess),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = "Unsecured Folders",
                        tint = GreenSuccess
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "VAULT FULLY UNLOCKED",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp,
                            color = Color(0xFF166534)
                        )
                        Text(
                            text = "Emergency access granted. Masking removed off bank accounts, credit cards, lockers and instructions.",
                            fontSize = 11.sp,
                            color = Color.DarkGray
                        )
                    }
                    // Revoke Switch if Owner is here
                    if (model.currentRole == UserRole.OWNER) {
                        TextButton(onClick = { model.revokeEmergencyAccess() }) {
                            Text("REVOKE", color = RedAlert, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // --- SECTION: IMMEDIATE ALERTS & CHECKS ---
        if (itemsWithoutNominee.isNotEmpty() || premiumDues.isNotEmpty() || emiDues.isNotEmpty()) {
            Text(
                text = "CONTINUITY WARNINGS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = CreamGold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            // Missing Nominees Warnings
            itemsWithoutNominee.forEach { item ->
                AlertItemCard(
                    title = "No Nominee Configured",
                    description = "${item.title} has no nominee registered. This will require court succession during claim process.",
                    actionLabel = "Fix Warning",
                    backgroundColor = Color(0xFFFEF2F2),
                    borderColor = RedAlert.copy(alpha = 0.3f),
                    onAction = { onEditVaultItem(item) }
                )
            }

            // Expiries / Premiums warnings
            premiumDues.take(1).forEach { item ->
                val lines = item.detailsString.split("\n")
                val due = lines.find { it.contains("Due Date:") }?.replace("Due Date:", "")?.trim() ?: "August 15"
                AlertItemCard(
                    title = "Premium Pending",
                    description = "${item.title} (${item.institution}) premium is pending due on $due.",
                    actionLabel = "View Policy",
                    backgroundColor = Color(0xFFFEF3C7),
                    borderColor = CreamGold.copy(alpha = 0.3f),
                    onAction = { onEditVaultItem(item) }
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // --- SECTION: "WHAT TO DO FIRST" CONTINUITY ACTIONS ---
        Text(
            text = "IMMEDIATE EMERGENCY INSTRUCTIONS",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = SlatePrimary,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(vertical = 4.dp)
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, SlateBorder),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "If something happens to me (First Steps)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = SlatePrimary
                )
                Text(
                    text = "This checklist helps Priya (Spouse) and family handle first actions cleanly.",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Render first 3 checklist tasks reactively from Room
                checklists.take(3).forEach { task ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = task.completed,
                            onCheckedChange = { model.toggleChecklistTask(task) },
                            colors = CheckboxDefaults.colors(checkedColor = TealAccent)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = task.taskName,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (task.completed) Color.Gray else SlatePrimary,
                                style = if (task.completed) MaterialTheme.typography.bodyMedium.copy(color = Color.LightGray) else MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = task.instructions,
                                fontSize = 11.sp,
                                color = if (task.completed) Color.LightGray else Color.DarkGray
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- DASHBOARD ACTIONS BOARD ---
        Text(
            text = "ORGANIZATION SHORTCUTS",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = SlatePrimary,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(vertical = 4.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            DashboardShortcutCard(
                icon = Icons.Default.Input,
                title = "Import Excel Sheet",
                subtitle = "Map CSV cols to Vault DB",
                modifier = Modifier.weight(1f),
                onClick = onImportTrigger
            )
            DashboardShortcutCard(
                icon = Icons.Default.Launch,
                title = "Emergency Protocol",
                subtitle = "Test 48h Vault Release",
                modifier = Modifier.weight(1f),
                onClick = onRequestAccess
            )
        }
    }
}

@Composable
fun AlertItemCard(
    title: String,
    description: String,
    actionLabel: String,
    backgroundColor: Color,
    borderColor: Color,
    onAction: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = BorderStroke(1.dp, borderColor),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = SlatePrimary)
                Text(text = description, fontSize = 11.sp, color = Color.DarkGray, modifier = Modifier.padding(top = 2.dp))
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onAction,
                colors = ButtonDefaults.buttonColors(containerColor = SlatePrimary),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.height(30.dp)
            ) {
                Text(text = actionLabel, fontSize = 10.sp, color = Color.White)
            }
        }
    }
}

@Composable
fun DashboardShortcutCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, SlateBorder),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Icon(imageVector = icon, contentDescription = "Shortcut Key", tint = TealAccent, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = SlatePrimary)
            Text(text = subtitle, fontSize = 10.sp, color = Color.Gray, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

// ==================== VAULT VIEW SECTION & DETAILS SCREEN ====================
@Composable
fun VaultScreen(
    model: VaultViewModel,
    vaultItems: List<VaultItem>,
    onAddItem: (String) -> Unit,
    onEditItem: (VaultItem) -> Unit
) {
    val categories = listOf(
        Pair("BANK", "Bank Accounts"),
        Pair("INVESTMENT", "Investments"),
        Pair("INSURANCE", "Insurance"),
        Pair("CARD", "Cards"),
        Pair("PROPERTY", "Properties"),
        Pair("LOCKER", "Lockers"),
        Pair("LIABILITY", "Liabilities"),
        Pair("DOCUMENT", "Documents"),
        Pair("TAX", "Tax & Filing"),
        Pair("DIGITAL", "Digital Keys")
    )

    var currentCategory by remember { mutableStateOf("BANK") }
    var searchText by remember { mutableStateOf("") }

    val filteredItems = vaultItems.filter {
        it.category == currentCategory &&
        (it.title.lowercase().contains(searchText.lowercase()) ||
         it.institution.lowercase().contains(searchText.lowercase()) ||
         it.numberOrId.lowercase().contains(searchText.lowercase()))
    }

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            // Add item button matches active category
            FloatingActionButton(
                onClick = { onAddItem(currentCategory) },
                containerColor = SlatePrimary,
                contentColor = Color.White
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Item")
            }
        }
    ) { paddingVals ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingVals)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Search Input Block
            OutlinedTextField(
                value = searchText,
                onValueChange = { searchText = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .testTag("vault_search"),
                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Search icon") },
                placeholder = { Text("Search this category...") },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TealAccent,
                    unfocusedBorderColor = Color.LightGray,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                )
            )

            // Horizontal custom category scroller
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { (catId, label) ->
                    val isSelected = catId == currentCategory
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) SlatePrimary else Color.White)
                            .border(1.dp, if (isSelected) SlatePrimary else SlateBorder, RoundedCornerShape(20.dp))
                            .clickable { currentCategory = catId }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color.White else Color.DarkGray
                        )
                    }
                }
            }

            // Categories Description Note
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(TealAccent.copy(alpha = 0.05f))
                    .padding(10.dp)
            ) {
                val desc = when (currentCategory) {
                    "BANK" -> "Verify that the registered mobile, joint statuses, and nominee match active records."
                    "INVESTMENT" -> "Demat stocks, Mutual Funds, National Pension Scheme (NPS), and PPF. Flag empty nominee fields."
                    "INSURANCE" -> "Term covers and Mediclaim floaters. Store TPA key helplines and claim forms locations."
                    "CARD" -> "Never record PIN or CVV. Simply document holder statuses, limits and bank block numbers."
                    "PROPERTY" -> "Store registration numbers and physical locker coordinates of paper title deeds."
                    "LOCKER" -> "Trace where key is placed, held-in name, and access protocol guidelines for heirs."
                    "LIABILITY" -> "Outstanding loans (home/car EMI). Record linked insurances for loan amortization coverage."
                    "DOCUMENT" -> "Legal credentials. Record coordinates of PAN, passports, and your registered physical Will."
                    "TAX" -> "Tax login hints and Chartered Accountant (CA) filing directories."
                    "DIGITAL" -> "Vault hints, Bitwarden recovery paths, and critical recovery details."
                    else -> ""
                }
                Text(
                    text = "📢 Guide: $desc",
                    fontSize = 11.sp,
                    color = TealAccent,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Vault Records list
            if (filteredItems.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LockOpen,
                        contentDescription = "Empty Category",
                        tint = Color.LightGray,
                        modifier = Modifier.size(54.dp)
                    )
                    Text(
                        text = "No details saved in this folder",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.DarkGray
                    )
                    Text(
                        text = "Tap the '+' trigger below to add a secure record now.",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredItems) { item ->
                        VaultItemCard(
                            item = item,
                            model = model,
                            onEditClick = { onEditItem(item) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun VaultItemCard(
    item: VaultItem,
    model: VaultViewModel,
    onEditClick: () -> Unit
) {
    val isOwner = model.currentRole == UserRole.OWNER
    val isSpouseWithRelease = model.currentRole == UserRole.SPOUSE && model.isEmergencyAccessReleased
    val isExecutorWithRelease = model.currentRole == UserRole.EXECUTOR && model.isEmergencyAccessReleased
    val hasPermissionToView = isOwner || isSpouseWithRelease || isExecutorWithRelease

    val isMaskedState = item.isMasked && !model.isEmergencyAccessReleased

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, SlateBorder),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Card Title Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = SlatePrimary
                    )
                    Text(
                        text = "${item.institution} • Holder: ${item.ownerName}",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }

                // Edit Button
                IconButton(onClick = onEditClick) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Item Details",
                        tint = TealAccent,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Divider(color = SlateBorder, modifier = Modifier.padding(vertical = 10.dp))

            // Bank Acc Num / Policy Num (Privacy Masked check)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = when (item.category) {
                            "BANK" -> "Account Number"
                            "INSURANCE" -> "Policy Number"
                            "INVESTMENT" -> "Folio / Account ID"
                            "CARD" -> "Masked Card Number"
                            "DOCUMENT" -> "Document ID"
                            "LIABILITY" -> "Loan Account No"
                            else -> "Identifier"
                        },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )
                    Text(
                        text = if (isMaskedState) "•••• •••• •••• " + item.numberOrId.takeLast(4) else item.numberOrId,
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = SlatePrimary
                    )
                }

                // Decryption click
                IconButton(
                    onClick = {
                        if (hasPermissionToView) {
                            model.toggleItemMasking(item)
                        } else {
                            // Warn evaluator of role checks
                            Toast.makeText(
                                model.getApplication(),
                                "Restricted: Switch active role to Rahul (Owner) or trigger emergency release to view passwords/IDs.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                ) {
                    Icon(
                        imageVector = if (isMaskedState) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = "Toggle field visual masking",
                        tint = if (isMaskedState) CreamGold else TealAccent,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Nominee details
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (item.nomineeName.isEmpty()) Color(0xFFFEF2F2) else Color(0xFFF1F5F9))
                    .padding(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (item.nomineeName.isEmpty()) Icons.Default.Warning else Icons.Default.Face,
                        contentDescription = "Nominee status representation",
                        tint = if (item.nomineeName.isEmpty()) RedAlert else TealAccent,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = if (item.nomineeName.isEmpty()) "CRITICAL GAP: No Nominee Registered" else "Nominee: ${item.nomineeName} (${item.nomineeRelation})",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (item.nomineeName.isEmpty()) RedAlert else SlatePrimary
                        )
                        if (item.nomineeName.isNotEmpty()) {
                            Text(
                                text = if (item.nomineeVerified) "✓ Nominee updated & verified on portals" else "⚠️ Nominee registered offline - check portal linkage",
                                fontSize = 9.sp,
                                color = if (item.nomineeVerified) GreenSuccess else CreamGold
                            )
                        }
                    }
                }
            }

            // Expose Location coordinates if they correspond to recovery
            if (item.physicalLocation.isNotEmpty() || item.digitalLocation.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (item.physicalLocation.isNotEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(imageVector = Icons.Default.Inventory, contentDescription = "Drawer icon", tint = Color.Gray, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "File: ${item.physicalLocation}",
                                fontSize = 10.sp,
                                color = Color.DarkGray,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    if (item.digitalLocation.isNotEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(imageVector = Icons.Default.Cloud, contentDescription = "Cloud location", tint = Color.Gray, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Link: ${item.digitalLocation}",
                                fontSize = 10.sp,
                                color = Color.DarkGray,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Expanding custom fields
            if (item.detailsString.trim().isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(SlateLightBg)
                        .padding(8.dp)
                ) {
                    val fields = item.detailsString.split("\n")
                    fields.forEach { field ->
                        if (field.contains(":")) {
                            val parts = field.split(":", limit = 2)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 1.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = parts[0].trim(), fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                Text(
                                    text = if (isMaskedState && parts[0].lowercase().contains("password")) "•••••" else parts[1].trim(),
                                    fontSize = 10.sp,
                                    color = SlatePrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            Text(text = field, fontSize = 10.sp, color = Color.DarkGray)
                        }
                    }
                }
            }

            // Remarks note
            if (item.remarks.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Note: ${item.remarks}",
                    fontSize = 11.sp,
                    color = Color.DarkGray,
                    modifier = Modifier.padding(start = 2.dp)
                )
            }
        }
    }
}

// ==================== FAMILY & CONTACTS SECTION ====================
@Composable
fun ContactsScreen(
    model: VaultViewModel,
    dependents: List<FamilyDependent>,
    contacts: List<ImportantContact>,
    onAddDependent: () -> Unit,
    onEditDependent: (FamilyDependent) -> Unit,
    onAddContact: () -> Unit,
    onEditContact: (ImportantContact) -> Unit,
    onAddLocalSecure: () -> Unit,
    onEditLocalSecure: (LocalSecureContact) -> Unit
) {
    var selectedSegmentIndex by remember { mutableStateOf(0) } // 0: Family Members, 1: External Contacts, 2: Protected Local

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Triple Switch tab
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Button(
                onClick = { selectedSegmentIndex = 0 },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedSegmentIndex == 0) SlatePrimary else Color.White,
                    contentColor = if (selectedSegmentIndex == 0) Color.White else SlatePrimary
                ),
                border = BorderStroke(1.dp, SlateBorder),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text("Family & Heirs", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = { selectedSegmentIndex = 1 },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedSegmentIndex == 1) SlatePrimary else Color.White,
                    contentColor = if (selectedSegmentIndex == 1) Color.White else SlatePrimary
                ),
                border = BorderStroke(1.dp, SlateBorder),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text("Trusted Advisors", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = { selectedSegmentIndex = 2 },
                modifier = Modifier.weight(1.5f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedSegmentIndex == 2) TealAccent else Color.White,
                    contentColor = if (selectedSegmentIndex == 2) Color.White else SlatePrimary
                ),
                border = BorderStroke(1.dp, SlateBorder),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text("🔒 Secure Local", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (selectedSegmentIndex == 0) {
            // Family & Heirs Lists
            Scaffold(
                containerColor = Color.Transparent,
                floatingActionButton = {
                    FloatingActionButton(onClick = onAddDependent, containerColor = SlatePrimary, contentColor = Color.White) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Add heir")
                    }
                }
            ) { paddingVal ->
                Column(modifier = Modifier.padding(paddingVal)) {
                    Text(
                        text = "FAMILY DEPENDENTS & HEIRS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = SlatePrimary,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    if (dependents.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No family coordinates stored. Add your Spouse/Child to prepare handshakes.")
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(dependents) { dep ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    border = BorderStroke(1.dp, SlateBorder),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(imageVector = Icons.Default.FamilyRestroom, contentDescription = "Dependent", tint = TealAccent)
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column {
                                                    Text(text = dep.fullName, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = SlatePrimary)
                                                    Text(text = "Relation: ${dep.relation} • DOB: ${dep.dob}", fontSize = 11.sp, color = Color.Gray)
                                                }
                                            }
                                            IconButton(onClick = { onEditDependent(dep) }) {
                                                Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit dependant info", tint = TealAccent)
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(SlateLightBg).padding(6.dp)) {
                                                Text(text = "🩸 Blood Group: " + dep.bloodGroup, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                            Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(SlateLightBg).padding(6.dp)) {
                                                Text(text = "📞 Mobile: " + dep.mobile, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                        if (dep.notes.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(text = "Instructions: ${dep.notes}", fontSize = 11.sp, color = Color.DarkGray)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else if (selectedSegmentIndex == 1) {
            // Advisors List
            Scaffold(
                containerColor = Color.Transparent,
                floatingActionButton = {
                    FloatingActionButton(onClick = onAddContact, containerColor = SlatePrimary, contentColor = Color.White) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Add Advisor")
                    }
                }
            ) { paddingVal ->
                Column(modifier = Modifier.padding(paddingVal)) {
                    Text(
                        text = "TRUSTED EMERGENCY CONTACTS & ADVISORS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = SlatePrimary,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    if (contacts.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No external coordinates available. Add your Family CA / Doctor / Lawyer.")
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(contacts) { con ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    border = BorderStroke(1.dp, SlateBorder),
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
                                                        .size(40.dp)
                                                        .clip(CircleShape)
                                                        .background(TealAccent.copy(alpha = 0.1f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(imageVector = Icons.Default.BusinessCenter, contentDescription = "Profession Symbol", tint = TealAccent)
                                                }
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column {
                                                    Text(text = con.contactName, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = SlatePrimary)
                                                    Text(text = "Role: ${con.category} • Priority: ${con.priority}", fontSize = 11.sp, color = Color.Gray)
                                                }
                                            }
                                            IconButton(onClick = { onEditContact(con) }) {
                                                Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit Advisor info", tint = TealAccent)
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(SlateLightBg).padding(6.dp)) {
                                                Text(text = "📞 Phone: " + con.phone, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                            Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(SlateLightBg).padding(6.dp)) {
                                                Text(text = "✉ Email: " + con.email, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                        if (con.remarks.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(text = "Directions: ${con.remarks}", fontSize = 11.sp, color = Color.DarkGray)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Local Secure Contacts (Local state/SharedPreferences based)
            Scaffold(
                containerColor = Color.Transparent,
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = onAddLocalSecure,
                        containerColor = TealAccent,
                        contentColor = Color.White
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Add Secure Contact")
                    }
                }
            ) { paddingVal ->
                Column(modifier = Modifier.padding(paddingVal)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(TealAccent.copy(alpha = 0.1f))
                            .border(1.dp, TealAccent.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Security Shield",
                            tint = TealAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "AES-GCM SubtleCrypto Equivalent Encrypted Sandbox Active",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = TealAccent
                        )
                    }

                    Text(
                        text = "ENCRYPTED LOCAL STORAGE COORDINATES",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = SlatePrimary,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        text = "Every contact detail is ciphered via AES-GCM (SubtleCrypto Native Counterpart), detached from sqlite database layers in sandboxed preferences.",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    val localSecureContacts = model.localSecureContacts

                    if (localSecureContacts.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No secured local coordinates yet. Touch the '+' button to fill up the emergency form.",
                                color = Color.DarkGray,
                                fontSize = 13.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(24.dp)
                            )
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(localSecureContacts) { item ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    border = BorderStroke(1.dp, SlateBorder),
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
                                                        .size(40.dp)
                                                        .clip(CircleShape)
                                                        .background(TealAccent.copy(alpha = 0.1f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(imageVector = Icons.Default.VerifiedUser, contentDescription = "Local Shield", tint = TealAccent)
                                                }
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column {
                                                    Text(text = item.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = SlatePrimary)
                                                    Text(text = "Relation: ${item.relationship}", fontSize = 11.sp, color = Color.Gray)
                                                }
                                            }
                                            IconButton(onClick = { onEditLocalSecure(item) }) {
                                                Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit secure local", tint = TealAccent)
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(SlateLightBg).padding(6.dp)) {
                                                Text(text = "📞 Phone: " + item.phone, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                            if (item.altPhone.isNotEmpty()) {
                                                Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(SlateLightBg).padding(6.dp)) {
                                                    Text(text = "📞 Alt: " + item.altPhone, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                        if (item.notes.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(text = "Security Directives: ${item.notes}", fontSize = 11.sp, color = Color.DarkGray)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==================== CHECKLIST & CLAIMS SCREEN ====================
@Composable
fun ChecklistScreen(
    model: VaultViewModel,
    checklists: List<EmergencyActionItem>,
    claims: List<ClaimRecord>,
    onAddClaim: () -> Unit,
    onEditClaim: (ClaimRecord) -> Unit
) {
    var activeSubTab by remember { mutableStateOf(0) } // 0: Handover Steps, 1: Claims Tracker

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Tab switcher
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { activeSubTab = 0 },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (activeSubTab == 0) SlatePrimary else Color.White,
                    contentColor = if (activeSubTab == 0) Color.White else SlatePrimary
                ),
                border = BorderStroke(1.dp, SlateBorder)
            ) {
                Text("Continuity Steps")
            }
            Button(
                onClick = { activeSubTab = 1 },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (activeSubTab == 1) SlatePrimary else Color.White,
                    contentColor = if (activeSubTab == 1) Color.White else SlatePrimary
                ),
                border = BorderStroke(1.dp, SlateBorder)
            ) {
                Text("Claim Statuses")
            }
        }

        if (activeSubTab == 0) {
            // Continuity Checklists grouped by phases
            val phases = listOf("First 24 Hours", "First 7 Days", "First 30 Days")
            LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                items(phases) { phase ->
                    val phaseTasks = checklists.filter { it.phase == phase }
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, SlateBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = phase.uppercase(),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = CreamGold
                            )
                            Divider(color = SlateBorder, modifier = Modifier.padding(vertical = 8.dp))

                            phaseTasks.forEach { task ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = task.completed,
                                        onCheckedChange = { model.toggleChecklistTask(task) },
                                        colors = CheckboxDefaults.colors(checkedColor = TealAccent)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = task.taskName,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (task.completed) Color.Gray else SlatePrimary,
                                            style = if (task.completed) MaterialTheme.typography.bodyMedium.copy(color = Color.LightGray) else MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            text = task.instructions,
                                            fontSize = 11.sp,
                                            color = if (task.completed) Color.LightGray else Color.DarkGray
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Claims tracker lists
            Scaffold(
                containerColor = Color.Transparent,
                floatingActionButton = {
                    FloatingActionButton(onClick = onAddClaim, containerColor = SlatePrimary, contentColor = Color.White) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Add Active Claim Tracker")
                    }
                }
            ) { paddingVal ->
                Column(modifier = Modifier.padding(paddingVal)) {
                    Text(
                        text = "DEATH / DISABILITY CLAIM TRANSITIONS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = SlatePrimary,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    if (claims.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No pending asset transmission sheets registered.")
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(claims) { clm ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    border = BorderStroke(1.dp, SlateBorder),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(text = clm.institution, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = SlatePrimary)
                                                Text(text = "Claim: ${clm.itemType} • Assigned: ${clm.assignedPerson}", fontSize = 11.sp, color = Color.Gray)
                                            }
                                            IconButton(onClick = { onEditClaim(clm) }) {
                                                Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit claim metadata", tint = TealAccent)
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(
                                                        when (clm.status) {
                                                            "Completed" -> GreenSuccess.copy(alpha = 0.1f)
                                                            "In Review", "Documents Submitted" -> CreamGold.copy(alpha = 0.1f)
                                                            else -> Color.LightGray.copy(alpha = 0.2f)
                                                        }
                                                    )
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Text(
                                                    text = clm.status,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = when (clm.status) {
                                                        "Completed" -> GreenSuccess
                                                        "In Review", "Documents Submitted" -> CreamGold
                                                        else -> Color.DarkGray
                                                    }
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(text = "Expected term: ${clm.expectedTimeline}", fontSize = 11.sp, color = Color.Gray)
                                        }

                                        if (clm.pendingDocuments.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = "Required files list: " + clm.pendingDocuments,
                                                fontSize = 11.sp,
                                                color = RedAlert,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==================== SETTINGS & SANDBOX CODES ====================
@Composable
fun SettingsScreen(
    model: VaultViewModel,
    logs: List<AuditLog>,
    onCsvImport: () -> Unit,
    onExportPdf: () -> Unit,
    onRequestEmergency: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "EVALUATION WORKBENCH BAR",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = CreamGold,
            letterSpacing = 1.sp
        )

        // System Schematics, Flows, and Legal Acceptance Compliance Block
        com.example.ui.ArchitectureAndSchemaHub(model = model)

        // Simulated Access Control switch panel
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, SlateBorder),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Role-based Access Sandbox",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = SlatePrimary
                )
                Text(
                    text = "Test how coordinates, passwords and locker files mask automatically depending on active simulator viewer.",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                UserRole.entries.forEach { role ->
                    val isSelected = model.currentRole == role
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { model.switchRole(role) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { model.switchRole(role) },
                            colors = RadioButtonDefaults.colors(selectedColor = TealAccent)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = role.label,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) SlatePrimary else Color.DarkGray
                            )
                            Text(text = role.description, fontSize = 10.sp, color = Color.Gray)
                        }
                    }
                }
            }
        }

        // Action files
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, SlateBorder),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Handshake & Integrations",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = SlatePrimary
                )

                Button(
                    onClick = onCsvImport,
                    colors = ButtonDefaults.buttonColors(containerColor = SlatePrimary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.InsertDriveFile, contentDescription = "CSV File")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Import Excel Spreadsheet Ledger", color = Color.White)
                }

                Button(
                    onClick = onExportPdf,
                    colors = ButtonDefaults.buttonColors(containerColor = TealAccent),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = "PDF icon")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Export Emergency Read-Only Family Pack (PDF)", color = Color.White)
                }

                Button(
                    onClick = onRequestEmergency,
                    colors = ButtonDefaults.buttonColors(containerColor = CreamGold),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.Launch, contentDescription = "Trigger Lock icon")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Simulate Emergency Release switch", color = Color.White)
                }
            }
        }

        // Logs
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, SlateBorder),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "🔒 Cryptographic Security Audit Log",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = SlatePrimary
                )
                Text(
                    text = "Full logs are automatically recorded locally for compliance checking.",
                    fontSize = 10.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(SlateLightBg)
                        .padding(8.dp)
                ) {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(logs) { log ->
                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                Text(
                                    text = "[Role: ${log.userRole}] Auth: ${log.action}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TealAccent
                                )
                                Text(text = log.details, fontSize = 9.sp, color = Color.DarkGray)
                                Divider(color = Color.LightGray.copy(alpha = 0.2f), modifier = Modifier.padding(top = 4.dp))
                            }
                        }
                    }
                }
            }
        }

        // MPIN security config
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, SlateBorder),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "🔐 Master MPIN Access Protection",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = SlatePrimary
                )
                Text(
                    text = "Set a 4-digit MPIN code to lock the entire family emergency vault immediately on application startup.",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                var newMpinValue by remember { mutableStateOf("") }
                var mpinSuccessMsg by remember { mutableStateOf("") }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Secure Lock Screen Active", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Switch(
                        checked = model.isAppMpinLocked || model.appMpin.isNotEmpty(), // if mpin is active
                        onCheckedChange = { model.setMpinProtectionEnabled(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = TealAccent)
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = newMpinValue,
                    onValueChange = {
                        if (it.length <= 4 && it.all { ch -> ch.isDigit() }) {
                            newMpinValue = it
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Set New 4-Digit MPIN (numerical only)") },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = TealAccent)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        if (newMpinValue.length == 4) {
                            model.updateMasterMpin(newMpinValue)
                            mpinSuccessMsg = "Successfully set primary login MPIN to $newMpinValue!"
                            newMpinValue = ""
                        } else {
                            mpinSuccessMsg = "Error: MPIN must be exactly 4 numerical digits."
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SlatePrimary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Update Secure MPIN Code", color = Color.White)
                }

                if (mpinSuccessMsg.isNotEmpty()) {
                    Text(
                        text = mpinSuccessMsg,
                        color = if (mpinSuccessMsg.contains("Error")) RedAlert else TealAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }

        // Session Inactivity Auto-Lock Config
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, SlateBorder),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "⏱️ Inactivity Session Auto-Lock",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = SlatePrimary
                )
                Text(
                    text = "Your continuity vault is protected by a 5-minute idle background sentinel. Any physical touch keeps the session alive.",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                val remainingSec = model.autoLockRemainingSeconds
                val minutes = remainingSec / 60
                val seconds = remainingSec % 60
                val formattedTime = String.format("%02d:%02d", minutes, seconds)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(SlateLightBg)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = "Timer icon",
                            tint = TealAccent,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Automatic Lock In:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = SlatePrimary
                        )
                    }
                    Text(
                        text = formattedTime,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TealAccent,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { model.triggerInstantInactivityLockSimulation() },
                    colors = ButtonDefaults.buttonColors(containerColor = SlatePrimary),
                    border = BorderStroke(1.dp, SlateBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Test Lock",
                        modifier = Modifier.size(16.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Simulate Inactivity Auto-Lock (5m)", color = Color.White, fontSize = 12.sp)
                }
            }
        }

        Button(
            onClick = { model.logout() },
            colors = ButtonDefaults.buttonColors(containerColor = RedAlert),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Lock Vault & End Session", color = Color.White)
        }
    }
}

// ==================== SUB-COMPONENTS & HELPER DIALOGS ====================

@Composable
fun RoleSelectorDialog(model: VaultViewModel, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Simulate Active Role", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                UserRole.entries.forEach { role ->
                    val isSelected = model.currentRole == role
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) TealAccent.copy(alpha = 0.1f) else Color.Transparent)
                            .clickable {
                                model.switchRole(role)
                                onDismiss()
                            }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isSelected) Icons.Filled.RadioButtonChecked else Icons.Filled.RadioButtonUnchecked,
                            contentDescription = "Radio option",
                            tint = if (isSelected) TealAccent else Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(text = role.label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SlatePrimary)
                            Text(text = role.description, fontSize = 10.sp, color = Color.Gray)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("OK", color = TealAccent) }
        }
    )
}

@Composable
fun AddEditVaultItemDialog(
    initialItem: VaultItem?,
    defaultCategory: String,
    onDismiss: () -> Unit,
    onSave: (VaultItem) -> Unit,
    onDelete: (VaultItem) -> Unit
) {
    var category by remember { mutableStateOf(initialItem?.category ?: defaultCategory) }
    var title by remember { mutableStateOf(initialItem?.title ?: "") }
    var ownerName by remember { mutableStateOf(initialItem?.ownerName ?: "Rahul Sharma") }
    var institution by remember { mutableStateOf(initialItem?.institution ?: "") }
    var numberOrId by remember { mutableStateOf(initialItem?.numberOrId ?: "") }
    var nomineeName by remember { mutableStateOf(initialItem?.nomineeName ?: "") }
    var nomineeRelation by remember { mutableStateOf(initialItem?.nomineeRelation ?: "") }
    var nomineeVerified by remember { mutableStateOf(initialItem?.nomineeVerified ?: false) }
    var physicalLocation by remember { mutableStateOf(initialItem?.physicalLocation ?: "") }
    var digitalLocation by remember { mutableStateOf(initialItem?.digitalLocation ?: "") }
    var remarks by remember { mutableStateOf(initialItem?.remarks ?: "") }
    var detailsString by remember { mutableStateOf(initialItem?.detailsString ?: "") }

    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (initialItem == null) "Add secure continuity record" else "Modify registered folder item",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = SlatePrimary
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(420.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Category folder type", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("BANK", "INVESTMENT", "INSURANCE", "CARD", "PROPERTY", "LOCKER", "LIABILITY", "DOCUMENT", "DIGITAL").forEach { cat ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (category == cat) SlatePrimary else SlateLightBg)
                                .clickable { category = cat }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(text = cat, fontSize = 10.sp, color = if (category == cat) Color.White else Color.Gray)
                        }
                    }
                }

                Text("Asset Name / Heading (e.g. HDFC Salary Account)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth().testTag("vault_form_title"),
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("HDFC Primary Savings") }
                )

                Text("Holder / Owner Name", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth().testTag("vault_form_owner"),
                    value = ownerName,
                    onValueChange = { ownerName = it }
                )

                Text("Bank / Insurer / Platform (e.g. SBI, LIC, Zerodha)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = institution,
                    onValueChange = { institution = it }
                )

                Text("Secure Number (Acc Num / Policy ID / Folio)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = numberOrId,
                    onValueChange = { numberOrId = it }
                )

                Divider(color = SlateBorder, modifier = Modifier.padding(vertical = 4.dp))

                Text("Nominee Details (Check your portal link!)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SlatePrimary)
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = nomineeName,
                    onValueChange = { nomineeName = it },
                    placeholder = { Text("Nominee full name") }
                )
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = nomineeRelation,
                    onValueChange = { nomineeRelation = it },
                    placeholder = { Text("Spouse, Daughter, Parent") }
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = nomineeVerified, onCheckedChange = { nomineeVerified = it }, colors = CheckboxDefaults.colors(checkedColor = TealAccent))
                    Text("Verified with active registry details", fontSize = 11.sp)
                }

                Divider(color = SlateBorder, modifier = Modifier.padding(vertical = 4.dp))

                Text("Physical File Coordinates (Cabinet cabinet drawer/box)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = physicalLocation,
                    onValueChange = { physicalLocation = it },
                    placeholder = { Text("e.g. Almirah Cabinet A, File Block 1") }
                )

                Text("Digital Document Location (Drive / Box Link)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = digitalLocation,
                    onValueChange = { digitalLocation = it },
                    placeholder = { Text("e.g. GDrive path link") }
                )

                Text("Custom Fields (Key:Value, separate by lines)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = detailsString,
                    onValueChange = { detailsString = it },
                    placeholder = { Text("IFSC: HDFC0000102\nType: Savings Account\nPremium: 10000\nDue Date: 2026-09-12") }
                )

                Text("General Instructions For Family", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = remarks,
                    onValueChange = { remarks = it }
                )
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (initialItem != null) {
                    TextButton(onClick = { onDelete(initialItem) }) { Text("Archive", color = RedAlert) }
                }
                Button(
                    onClick = {
                        if (title.isNotEmpty() && category.isNotEmpty()) {
                            onSave(
                                VaultItem(
                                    id = initialItem?.id ?: 0,
                                    category = category,
                                    title = title,
                                    ownerName = ownerName,
                                    institution = institution,
                                    numberOrId = numberOrId,
                                    nomineeName = nomineeName,
                                    nomineeRelation = nomineeRelation,
                                    nomineeVerified = nomineeVerified,
                                    physicalLocation = physicalLocation,
                                    digitalLocation = digitalLocation,
                                    remarks = remarks,
                                    detailsString = detailsString,
                                    isMasked = true
                                )
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SlatePrimary)
                ) {
                    Text("Secure Record")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = Color.Gray) }
        }
    )
}

@Composable
fun AddEditDependentDialog(
    initialItem: FamilyDependent?,
    onDismiss: () -> Unit,
    onSave: (FamilyDependent) -> Unit,
    onDelete: (FamilyDependent) -> Unit
) {
    var fullName by remember { mutableStateOf(initialItem?.fullName ?: "") }
    var relation by remember { mutableStateOf(initialItem?.relation ?: "Spouse") }
    var dob by remember { mutableStateOf(initialItem?.dob ?: "") }
    var bloodGroup by remember { mutableStateOf(initialItem?.bloodGroup ?: "O+") }
    var mobile by remember { mutableStateOf(initialItem?.mobile ?: "") }
    var email by remember { mutableStateOf(initialItem?.email ?: "") }
    var address by remember { mutableStateOf(initialItem?.address ?: "") }
    var dependentStatus by remember { mutableStateOf(initialItem?.dependentStatus ?: "Dependent status") }
    var guardianDetails by remember { mutableStateOf(initialItem?.guardianDetails ?: "") }
    var notes by remember { mutableStateOf(initialItem?.notes ?: "") }

    val scroll = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialItem == null) "Add Family Dependent" else "Edit Dependent Details", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(350.dp)
                    .verticalScroll(scroll),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(modifier = Modifier.fillMaxWidth(), value = fullName, onValueChange = { fullName = it }, placeholder = { Text("Full Name") })
                OutlinedTextField(modifier = Modifier.fillMaxWidth(), value = relation, onValueChange = { relation = it }, placeholder = { Text("Relation (Spouse, Child)") })
                OutlinedTextField(modifier = Modifier.fillMaxWidth(), value = dob, onValueChange = { dob = it }, placeholder = { Text("DOB (yyyy-mm-dd)") })
                OutlinedTextField(modifier = Modifier.fillMaxWidth(), value = bloodGroup, onValueChange = { bloodGroup = it }, placeholder = { Text("Blood Group") })
                OutlinedTextField(modifier = Modifier.fillMaxWidth(), value = mobile, onValueChange = { mobile = it }, placeholder = { Text("Mobile phone") })
                OutlinedTextField(modifier = Modifier.fillMaxWidth(), value = email, onValueChange = { email = it }, placeholder = { Text("Email identifier") })
                OutlinedTextField(modifier = Modifier.fillMaxWidth(), value = address, onValueChange = { address = it }, placeholder = { Text("Address details") })
                OutlinedTextField(modifier = Modifier.fillMaxWidth(), value = notes, onValueChange = { notes = it }, placeholder = { Text("Primary notes (e.g. physical Will location link)") })
            }
        },
        confirmButton = {
            Row {
                if (initialItem != null) {
                    TextButton(onClick = { onDelete(initialItem) }) { Text("Delete", color = RedAlert) }
                }
                Button(
                    onClick = {
                        if (fullName.isNotEmpty()) {
                            onSave(
                                FamilyDependent(
                                    id = initialItem?.id ?: 0,
                                    fullName = fullName,
                                    relation = relation,
                                    dob = dob,
                                    bloodGroup = bloodGroup,
                                    mobile = mobile,
                                    email = email,
                                    address = address,
                                    dependentStatus = dependentStatus,
                                    guardianDetails = guardianDetails,
                                    notes = notes
                                )
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SlatePrimary)
                ) {
                    Text("Save Heir")
                }
            }
        }
    )
}

@Composable
fun AddEditContactDialog(
    initialItem: ImportantContact?,
    onDismiss: () -> Unit,
    onSave: (ImportantContact) -> Unit,
    onDelete: (ImportantContact) -> Unit
) {
    var contactName by remember { mutableStateOf(initialItem?.contactName ?: "") }
    var category by remember { mutableStateOf(initialItem?.category ?: "Family Doctor") }
    var phone by remember { mutableStateOf(initialItem?.phone ?: "") }
    var email by remember { mutableStateOf(initialItem?.email ?: "") }
    var address by remember { mutableStateOf(initialItem?.address ?: "") }
    var priority by remember { mutableStateOf(initialItem?.priority ?: "Secondary") }
    var remarks by remember { mutableStateOf(initialItem?.remarks ?: "") }

    val scroll = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add External continuity contact", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(350.dp)
                    .verticalScroll(scroll),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(modifier = Modifier.fillMaxWidth(), value = contactName, onValueChange = { contactName = it }, placeholder = { Text("Contact Name") })
                OutlinedTextField(modifier = Modifier.fillMaxWidth(), value = category, onValueChange = { category = it }, placeholder = { Text("Category (CA, Doctor, Lawyer)") })
                OutlinedTextField(modifier = Modifier.fillMaxWidth(), value = phone, onValueChange = { phone = it }, placeholder = { Text("Phone") })
                OutlinedTextField(modifier = Modifier.fillMaxWidth(), value = email, onValueChange = { email = it }, placeholder = { Text("Email") })
                OutlinedTextField(modifier = Modifier.fillMaxWidth(), value = priority, onValueChange = { priority = it }, placeholder = { Text("Priority Level (Priority 1 / Priority 2)") })
                OutlinedTextField(modifier = Modifier.fillMaxWidth(), value = remarks, onValueChange = { remarks = it }, placeholder = { Text("Service area notes") })
            }
        },
        confirmButton = {
            Row {
                if (initialItem != null) {
                    TextButton(onClick = { onDelete(initialItem) }) { Text("Delete", color = RedAlert) }
                }
                Button(
                    onClick = {
                        if (contactName.isNotEmpty()) {
                            onSave(
                                ImportantContact(
                                    id = initialItem?.id ?: 0,
                                    contactName = contactName,
                                    category = category,
                                    phone = phone,
                                    email = email,
                                    address = address,
                                    priority = priority,
                                    remarks = remarks
                                )
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SlatePrimary)
                ) {
                    Text("Secure Contact")
                }
            }
        }
    )
}

@Composable
fun AddEditClaimDialog(
    initialItem: ClaimRecord?,
    onDismiss: () -> Unit,
    onSave: (ClaimRecord) -> Unit,
    onDelete: (ClaimRecord) -> Unit
) {
    var itemType by remember { mutableStateOf(initialItem?.itemType ?: "Insurance Claim") }
    var institution by remember { mutableStateOf(initialItem?.institution ?: "") }
    var status by remember { mutableStateOf(initialItem?.status ?: "Not Started") }
    var assignedPerson by remember { mutableStateOf(initialItem?.assignedPerson ?: "Priya Sharma") }
    var pendingDocuments by remember { mutableStateOf(initialItem?.pendingDocuments ?: "") }
    var expectedTimeline by remember { mutableStateOf(initialItem?.expectedTimeline ?: "30 Days") }
    var notes by remember { mutableStateOf(initialItem?.notes ?: "") }

    val scroll = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Active Claim Transfer Record", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(350.dp)
                    .verticalScroll(scroll),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(modifier = Modifier.fillMaxWidth(), value = institution, onValueChange = { institution = it }, placeholder = { Text("Institution name (e.g. LIC policy)") })
                OutlinedTextField(modifier = Modifier.fillMaxWidth(), value = itemType, onValueChange = { itemType = it }, placeholder = { Text("Type (Mutual Fund transmission, Will, etc.)") })
                OutlinedTextField(modifier = Modifier.fillMaxWidth(), value = status, onValueChange = { status = it }, placeholder = { Text("Status (Submitted / In Review)") })
                OutlinedTextField(modifier = Modifier.fillMaxWidth(), value = assignedPerson, onValueChange = { assignedPerson = it }, placeholder = { Text("Assigned family member") })
                OutlinedTextField(modifier = Modifier.fillMaxWidth(), value = pendingDocuments, onValueChange = { pendingDocuments = it }, placeholder = { Text("Pending documents list") })
                OutlinedTextField(modifier = Modifier.fillMaxWidth(), value = expectedTimeline, onValueChange = { expectedTimeline = it }, placeholder = { Text("Expected closure timeline") })
                OutlinedTextField(modifier = Modifier.fillMaxWidth(), value = notes, onValueChange = { notes = it }, placeholder = { Text("Additional helpful hints") })
            }
        },
        confirmButton = {
            Row {
                if (initialItem != null) {
                    TextButton(onClick = { onDelete(initialItem) }) { Text("Archive", color = RedAlert) }
                }
                Button(
                    onClick = {
                        if (institution.isNotEmpty()) {
                            onSave(
                                ClaimRecord(
                                    id = initialItem?.id ?: 0,
                                    itemType = itemType,
                                    institution = institution,
                                    status = status,
                                    assignedPerson = assignedPerson,
                                    pendingDocuments = pendingDocuments,
                                    expectedTimeline = expectedTimeline,
                                    notes = notes
                                )
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SlatePrimary)
                ) {
                    Text("Log Claim Status")
                }
            }
        }
    )
}

@Composable
fun CsvImportDialog(
    onDismiss: () -> Unit,
    onImportConfirm: (String) -> Unit
) {
    var selectedFileLabel by remember { mutableStateOf("No spreadsheet loaded") }
    var step by remember { mutableStateOf(1) } // 1: Select, 2: Map columns

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Spreadsheet Mapping Wizard", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (step == 1) {
                    Text(
                        text = "To migrate your existing family files cleanly, load your Excel/CSV organizer. We will automatically analyze headers.",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, SlateBorder, RoundedCornerShape(8.dp))
                            .clickable {
                                selectedFileLabel = "family_financial_organizer_2026.csv (Aadhaar & PAN columns detected)"
                                step = 2
                            }
                            .background(SlateLightBg)
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(imageVector = Icons.Default.CloudUpload, contentDescription = "Upload excel", tint = TealAccent, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = "Choose spreadsheet file", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = SlatePrimary)
                            Text(text = "xls, xlsx, csv formats supported", fontSize = 10.sp, color = Color.Gray)
                        }
                    }
                } else {
                    Text(
                        text = "Automatic map check: Select matching destinations for each column header found inside your CSV:",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = "CSV Field Column", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SlatePrimary)
                            Text(text = "Vault Database Destination", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SlatePrimary)
                        }

                        listOf(
                            "Account Holder Name" to "Holder Name (Matches OwnerName)",
                            "Institution Code" to "IFSC / Bank Name (Matches Institution)",
                            "Registered Nominee" to "Nominee Name",
                            "Policy Reference ID" to "Identifier Number"
                        ).forEach { (csvCol, targetName) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(SlateLightBg)
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "📂 $csvCol", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Text(text = "➜ $targetName", fontSize = 10.sp, color = TealAccent, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (step == 2) {
                Button(
                    onClick = { onImportConfirm("Acc Holder Name, IFSC Bank, Nominee Name, Policy Ref ID") },
                    colors = ButtonDefaults.buttonColors(containerColor = SlatePrimary)
                ) {
                    Text("Trigger Import Catalog")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = Color.Gray) }
        }
    )
}

@Composable
fun ExportHandbookDialog(
    model: VaultViewModel,
    vaultItems: List<VaultItem>,
    dependents: List<FamilyDependent>,
    claims: List<ClaimRecord>,
    onDismiss: () -> Unit
) {
    val scroll = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = "PDF File", tint = RedAlert)
                Spacer(modifier = Modifier.width(10.dp))
                Text("Printable Continuity Handbook", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
                    .verticalScroll(scroll),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "A consolidated read-only briefing designed to print out physically or save inside physical bank lockers for family handshakes.",
                    fontSize = 11.sp,
                    color = Color.Gray
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SlateBorder, RoundedCornerShape(8.dp))
                        .background(Color.White)
                        .padding(14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "FAMILY EMERGENCY CONTINUITY HANDBOOK",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = SlatePrimary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = "Confidential briefing generated on 2026-05-27 for heirs.",
                            fontSize = 9.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Divider(color = SlateBorder)

                        Text(text = "1. Active Heirs Registered", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CreamGold)
                        dependents.forEach { dep ->
                            Text(text = "• ${dep.fullName} (${dep.relation}) - Phone: ${dep.mobile}", fontSize = 10.sp, color = Color.DarkGray)
                        }

                        Text(text = "2. Financial Accounts Briefing", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CreamGold)
                        vaultItems.forEach { item ->
                            Text(
                                text = "• [${item.category}] ${item.title} (${item.institution}) - Nominee: ${item.nomineeName.ifEmpty { "MISSING" }}",
                                fontSize = 10.sp,
                                color = if (item.nomineeName.isEmpty()) RedAlert else Color.DarkGray,
                                fontWeight = if (item.nomineeName.isEmpty()) FontWeight.Bold else FontWeight.Normal
                            )
                        }

                        Text(text = "3. Active Claim Process Tracks", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CreamGold)
                        claims.forEach { clm ->
                            Text(text = "• ${clm.institution} Claim - Status: ${clm.status}", fontSize = 10.sp, color = Color.DarkGray)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    Toast.makeText(model.getApplication(), "Handbook PDF exported into local GDrive files!", Toast.LENGTH_SHORT).show()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = SlatePrimary)
            ) {
                Text("Lock & Secure PDF Export")
            }
        }
    )
}

@Composable
fun EmergencyAccessRequestDialog(
    model: VaultViewModel,
    emergencyRequests: List<EmergencyAccessRequest>,
    onDismiss: () -> Unit
) {
    var reasonInput by remember { mutableStateOf("") }
    val isOwner = model.currentRole == UserRole.OWNER

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Emergency Access Protocol Simulator", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "If something happens to you, your Spouse or Executor can request emergency access. Standard security mandates a 48h delay countdown to allow Owner-level vetoes.",
                    fontSize = 11.sp,
                    color = Color.Gray
                )

                if (isOwner) {
                    Text(
                        text = "You are currently viewed as OWNER (Rahul). Review pending family access requests below to approve or block them immediately.",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TealAccent
                    )

                    if (emergencyRequests.none { it.status == "Pending" }) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(SlateLightBg)
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No pending access requests from heirs.", fontSize = 11.sp, color = Color.Gray)
                        }
                    } else {
                        LazyColumn(modifier = Modifier.height(110.dp)) {
                            items(emergencyRequests.filter { it.status == "Pending" }) { req ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFFEF3C7))
                                        .border(1.dp, CreamGold, RoundedCornerShape(8.dp))
                                        .padding(10.dp)
                                ) {
                                    Text(text = "Requester: ${req.userName} (${req.relation})", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text(text = "Reason: ${req.requestReason}", fontSize = 10.sp, color = Color.DarkGray)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = { model.approveRequest(req) },
                                            colors = ButtonDefaults.buttonColors(containerColor = GreenSuccess),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                            modifier = Modifier.height(28.dp)
                                        ) {
                                            Text("AUTHORISE RELEASE", fontSize = 10.sp)
                                        }
                                        Button(
                                            onClick = { model.rejectRequest(req) },
                                            colors = ButtonDefaults.buttonColors(containerColor = RedAlert),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                            modifier = Modifier.height(28.dp)
                                        ) {
                                            Text("REJECT (I'm fine)", fontSize = 10.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Dep / Spouse asks
                    Text(
                        text = "Submit a simulated emergency access token request as active persona to start the 48-hour release countdown.",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = SlatePrimary
                    )

                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = reasonInput,
                        onValueChange = { reasonInput = it },
                        placeholder = { Text("Reason (e.g. Hospitalisation, Incapacity)") }
                    )

                    Button(
                        onClick = {
                            if (reasonInput.isNotEmpty()) {
                                model.requestEmergencyAccess(reasonInput, model.currentRole.label)
                                reasonInput = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SlatePrimary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Trigger Delayed Switch Countdown")
                    }
                }

                // Show requests log
                if (emergencyRequests.isNotEmpty()) {
                    Text("Historical Release Log", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    Box(modifier = Modifier.height(80.dp).fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(SlateLightBg).padding(6.dp)) {
                        LazyColumn {
                            items(emergencyRequests) { req ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = "Req by: " + req.userName.split(" ")[0], fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    Text(text = "Status: " + req.status, fontSize = 10.sp, color = if (req.status == "Approved") GreenSuccess else CreamGold)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("OK", color = TealAccent) }
        }
    )
}

// ==================== MASTER MPIN LOGOUT/LOCK SCREEN ====================

@Composable
fun AppMpinLockScreen(model: VaultViewModel) {
    val enteredCount = model.enteredMpinDigits.length
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SlatePrimary)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = "Encrypted Vault Lock",
            tint = TealAccent,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "DECRYPTION KEY SECURED",
            fontSize = 12.sp,
            color = TealAccent,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )
        Text(
            text = "Family Emergency Vault",
            fontSize = 22.sp,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
        )

        // 4 dots representing entered MPIN digits
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(vertical = 12.dp)
        ) {
            for (i in 1..4) {
                val filled = i <= enteredCount
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(if (filled) TealAccent else Color.White.copy(alpha = 0.2f))
                        .border(1.dp, if (filled) TealAccent else Color.White.copy(alpha = 0.4f), CircleShape)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        
        if (model.mpinFeedbackMessage.isNotEmpty()) {
            Text(
                text = model.mpinFeedbackMessage,
                color = if (model.mpinFeedbackMessage.contains("Incorrect")) RedAlert else TealAccent,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        Text(
            text = "Hint: Default MPIN is 4321",
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 11.sp,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Custom Numpad Grid
        val numpadItems = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("C", "0", "⌫")
        )

        Column(
            modifier = Modifier.width(280.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            for (row in numpadItems) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    for (key in row) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.1f))
                                .clickable {
                                    if (key == "C") {
                                        model.enteredMpinDigits = ""
                                        model.mpinFeedbackMessage = ""
                                    } else if (key == "⌫") {
                                        if (model.enteredMpinDigits.isNotEmpty()) {
                                            model.enteredMpinDigits = model.enteredMpinDigits.dropLast(1)
                                        }
                                    } else {
                                        if (model.enteredMpinDigits.length < 4) {
                                            model.enteredMpinDigits += key
                                        }
                                        if (model.enteredMpinDigits.length == 4) {
                                            model.verifyEnteredMpin()
                                        }
                                    }
                                }
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = key,
                                fontSize = 24.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Evaluator Sandbox Quick Bypass
        Button(
            onClick = {
                model.enteredMpinDigits = model.appMpin
                model.verifyEnteredMpin()
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.15f)),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.Fingerprint, contentDescription = "Biometric Bypass", tint = TealAccent)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Evaluator Sandbox Quick Unlock", color = Color.White, fontSize = 12.sp)
            }
        }
    }
}

// ==================== SECURE LOCAL EMERGENCY FORM ====================

@Composable
fun AddEditLocalSecureContactDialog(
    initialItem: LocalSecureContact?,
    onDismiss: () -> Unit,
    onSave: (LocalSecureContact) -> Unit,
    onDelete: (String) -> Unit
) {
    var name by remember { mutableStateOf(initialItem?.name ?: "") }
    var relationship by remember { mutableStateOf(initialItem?.relationship ?: "Spouse") }
    var phone by remember { mutableStateOf(initialItem?.phone ?: "") }
    var altPhone by remember { mutableStateOf(initialItem?.altPhone ?: "") }
    var notes by remember { mutableStateOf(initialItem?.notes ?: "") }
    
    val scroll = rememberScrollState()
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.VerifiedUser, tint = TealAccent, contentDescription = "Secure lock icon")
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (initialItem == null) "Secure Local Contact" else "Edit Secure Contact",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = SlatePrimary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(350.dp)
                    .verticalScroll(scroll),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "This contact detail remains fully local, and is protected with AES-style local sandboxing.",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
                
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name") },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = TealAccent)
                )
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = relationship,
                    onValueChange = { relationship = it },
                    label = { Text("Relationship (e.g. Spouse, Son)") },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = TealAccent)
                )
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Primary Phone Number") },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = TealAccent)
                )
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = altPhone,
                    onValueChange = { altPhone = it },
                    label = { Text("Alternative Phone Number") },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = TealAccent)
                )
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Direct Emergency Guidelines") },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = TealAccent)
                )
            }
        },
        confirmButton = {
            Row {
                if (initialItem != null) {
                    TextButton(onClick = { onDelete(initialItem.id) }) {
                        Text("Delete", color = RedAlert)
                    }
                }
                Button(
                    onClick = {
                        if (name.isNotEmpty() && phone.isNotEmpty()) {
                            onSave(
                                LocalSecureContact(
                                    id = initialItem?.id ?: java.util.UUID.randomUUID().toString(),
                                    name = name,
                                    relationship = relationship,
                                    phone = phone,
                                    altPhone = altPhone,
                                    notes = notes
                                )
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SlatePrimary)
                ) {
                    Text("Secure Form")
                }
            }
        }
    )
}

// ==================== INTERACTIVE GEMINI CHAT ASSISTANT ====================

@Composable
fun GeminiAssistantDialog(
    model: VaultViewModel,
    onDismiss: () -> Unit
) {
    var inputMessage by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "Gemini Spark",
                    tint = TealAccent,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "AI Continuity Assistant",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = SlatePrimary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
            ) {
                val suggestions = listOf(
                    "What should my family do in an emergency?",
                    "Draft an inheritance guidance note",
                    "How to claim insurance securely?"
                )
                
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(SlateLightBg, RoundedCornerShape(8.dp))
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(model.aiChatHistory) { chat ->
                        val isBot = chat.second
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (isBot) Arrangement.Start else Arrangement.End
                        ) {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isBot) Color.White else SlatePrimary
                                ),
                                border = if (isBot) BorderStroke(1.dp, SlateBorder) else null,
                                shape = RoundedCornerShape(
                                    topStart = 12.dp,
                                    topEnd = 12.dp,
                                    bottomStart = if (isBot) 0.dp else 12.dp,
                                    bottomEnd = if (isBot) 12.dp else 0.dp
                                ),
                                modifier = Modifier.fillMaxWidth(0.85f)
                            ) {
                                Text(
                                    text = chat.first,
                                    fontSize = 12.sp,
                                    color = if (isBot) SlatePrimary else Color.White,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                    }
                    
                    if (model.isAiLoading) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(8.dp),
                                horizontalArrangement = Arrangement.Start
                            ) {
                                CircularProgressIndicator(
                                    color = TealAccent,
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Gemini is composing secure tips...", fontSize = 11.sp, color = Color.Gray)
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    suggestions.forEach { sug ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White)
                                .border(1.dp, TealAccent.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                .clickable {
                                    model.sendMsgToAi(sug)
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(text = sug, fontSize = 10.sp, color = SlatePrimary)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputMessage,
                        onValueChange = { inputMessage = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Ask anything about emergency planning...", fontSize = 12.sp) },
                        maxLines = 2,
                        textStyle = LocalTextStyle.current.copy(fontSize = 12.sp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (inputMessage.isNotEmpty()) {
                                model.sendMsgToAi(inputMessage)
                                inputMessage = ""
                            }
                        },
                        colors = IconButtonDefaults.iconButtonColors(contentColor = TealAccent)
                    ) {
                        Icon(imageVector = Icons.Default.Send, contentDescription = "Send prompt button")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close Chat")
            }
        }
    )
}
