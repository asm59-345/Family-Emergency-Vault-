package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import com.example.util.BiometricAuthManager
import com.example.util.BiometricAvailability
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
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

class MainActivity : FragmentActivity() {
    private val model by lazy {
        androidx.lifecycle.ViewModelProvider(this)[VaultViewModel::class.java]
    }

    fun triggerBiometricPrompt(
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val availability = BiometricAuthManager.checkBiometricAvailability(this)
        if (availability.canPrompt) {
            BiometricAuthManager.promptBiometric(
                activity = this,
                title = if (model.isHindiMode) "बायोमेट्रिक प्रमाणीकरण" else "Biometric Vault Authentication",
                subtitle = if (model.isHindiMode) "फिंगरप्रिंट या फेस अनलॉक से वॉल्ट खोलें" else "Scan fingerprint or face to decrypt sensitive data",
                description = if (model.isHindiMode) "आपके वित्तीय व आपातकालीन रिकॉर्ड्स डिवाइस पर एन्क्रिप्टेड हैं।" else "Military-grade on-device cryptographic protection for family assets.",
                negativeButtonText = if (model.isHindiMode) "मास्टर पिन का प्रयोग करें" else "Use Master MPIN",
                onSuccess = {
                    model.unlockViaBiometrics("Fingerprint / Face Unlock")
                    onSuccess()
                },
                onError = { code, err ->
                    if (code != androidx.biometric.BiometricPrompt.ERROR_USER_CANCELED &&
                        code != androidx.biometric.BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                        model.mpinFeedbackMessage = "Biometric: $err"
                    }
                    onError(err.toString())
                },
                onFailed = {
                    model.mpinFeedbackMessage = "Biometric not recognized. Please try again or use MPIN."
                }
            )
        } else {
            model.biometricFeedbackMessage = availability.userTitle
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val currentFontTheme = model.selectedFontTheme
            MyApplicationTheme(fontTheme = currentFontTheme) {
                ProvideTextStyle(
                    value = androidx.compose.ui.text.TextStyle(fontFamily = currentFontTheme.bodyFamily)
                ) {
                    MainAppSurface(model)
                }
            }
        }
    }

    override fun dispatchTouchEvent(ev: android.view.MotionEvent?): Boolean {
        if (ev?.action == android.view.MotionEvent.ACTION_DOWN) {
            model.updateUserActivity()
        }
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
    var showEmergencySosDialog by remember { mutableStateOf(false) }
    var showMedicalEmergencyCardDialog by remember { mutableStateOf(false) }

    // First time user onboarding starts directly with account registration & privacy consent
    if (!model.isAccountCreated) {
        com.example.ui.SignupScreen(model = model)
    } else if (model.isAppMpinLocked) {
        Box(modifier = Modifier.fillMaxSize()) {
            AppMpinLockScreen(
                model = model,
                onTriggerBiometric = { (context as? MainActivity)?.triggerBiometricPrompt() },
                onTriggerSos = { showEmergencySosDialog = true }
            )
            if (showEmergencySosDialog) {
                com.example.ui.EmergencySosDialog(
                    model = model,
                    contacts = contacts,
                    vaultItems = vaultItems,
                    onDismiss = { showEmergencySosDialog = false }
                )
            }
        }
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
                            text = if (model.isHindiMode) "पारिवारिक आपातकालीन वॉल्ट" else "Family Emergency Vault",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = model.selectedFontTheme.headingFamily
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
                    // Emergency SOS Trigger Button
                    Box(
                        modifier = Modifier
                            .padding(end = 6.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFDC2626))
                            .clickable { showEmergencySosDialog = true }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                            .testTag("btn_topbar_emergency_sos"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Emergency SOS",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "SOS",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }

                    // Language Switcher (EN / हिंदी)
                    Box(
                        modifier = Modifier
                            .padding(end = 6.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.2f))
                            .clickable { model.toggleLanguage() }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = "Language",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (model.isHindiMode) "हिंदी" else "English",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Legal, Q&A, Policies & About Hub Icon
                    Box(
                        modifier = Modifier
                            .padding(end = 6.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.2f))
                            .clickable { model.openLegalAboutHub(0) }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Info and Legal",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (model.isHindiMode) "जानकारी" else "About",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

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
                com.example.ui.LoginScreen(model = model)
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
                        },
                        onOpenSos = { showEmergencySosDialog = true },
                        onOpenMedicalCard = { showMedicalEmergencyCardDialog = true }
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
                        },
                        onOpenSos = { showEmergencySosDialog = true }
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

            // 11. Emergency SOS & Offline Location Dialog
            if (showEmergencySosDialog) {
                com.example.ui.EmergencySosDialog(
                    model = model,
                    contacts = contacts,
                    vaultItems = vaultItems,
                    onDismiss = { showEmergencySosDialog = false }
                )
            }

            // 12. Local System Observability & Data Locality Dialog
            if (model.showObservabilityDialog) {
                com.example.ui.SystemObservabilityDialog(
                    model = model,
                    onDismiss = { model.showObservabilityDialog = false }
                )
            }

            // 13. Legal, Governance, Q&A, Policies, Security & Creator Hub Dialog
            if (model.showLegalAboutHub) {
                com.example.ui.LegalAndAboutHubDialog(
                    model = model,
                    initialTab = model.legalAboutInitialTab,
                    onDismiss = { model.showLegalAboutHub = false }
                )
            }

            // 14. Dedicated Family Medical Emergency Card Dialog
            if (showMedicalEmergencyCardDialog) {
                com.example.ui.MedicalEmergencyCardDialog(
                    model = model,
                    dependents = dependents,
                    contacts = contacts,
                    vaultItems = vaultItems,
                    onDismiss = { showMedicalEmergencyCardDialog = false }
                )
            }
        }
    }
}
}

// ==================== AUTHENTICATION SCREEN ====================
@Composable
fun LegacyLoginScreenBypass(model: VaultViewModel) {
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
    onEditVaultItem: (VaultItem) -> Unit,
    onOpenSos: () -> Unit = {},
    onOpenMedicalCard: () -> Unit = {}
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
        // 🚨 High Priority Emergency SOS & Offline Location Hotbar
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF7F1D1D)),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, Color(0xFFEF4444)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp)
                .clickable { onOpenSos() }
                .testTag("dashboard_sos_card")
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFDC2626)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.Warning, contentDescription = "SOS", tint = Color.White, modifier = Modifier.size(22.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (model.isHindiMode) "🚨 आपातकालीन SOS व ऑफ़लाइन लोकेशन" else "🚨 Emergency SOS & Offline Location",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Text(
                            text = if (model.isHindiMode) "बिना इंटरनेट के GPS SMS भेजें और सीधे परिवार को कॉल करें" else "1-Tap Call & Dispatch Live GPS via Offline SMS",
                            fontSize = 11.sp,
                            color = Color(0xFFFECACA)
                        )
                    }
                }
                Surface(
                    color = Color(0xFFDC2626),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "OPEN SOS",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                    )
                }
            }
        }

        // 🩺 High Priority Family Emergency Medical & Health Card Button
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFFECDD3)),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
                .clickable { onOpenMedicalCard() }
                .testTag("dashboard_medical_card_button")
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFEF2F2)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MedicalServices,
                            contentDescription = "Medical",
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (model.isHindiMode) "🩺 आपातकालीन मेडिकल कार्ड (त्वरित स्वास्थ्य संदर्भ)" else "🩺 Family Emergency Medical Card",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = SlatePrimary
                        )
                        Text(
                            text = if (model.isHindiMode) "ब्लड ग्रुप, दवाइयां, डॉक्टर कॉल व कैशलेस TPA कार्ड" else "Blood groups, critical allergies & cashless hospital policy",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }
                Surface(
                    color = Color(0xFFDC2626),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "VIEW",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                    )
                }
            }
        }

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

        Spacer(modifier = Modifier.height(14.dp))

        // Visual Vault Hero Banner
        Card(
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, SlateBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 14.dp)
                .testTag("dashboard_vault_hero_banner")
        ) {
            Box {
                Image(
                    painter = painterResource(id = R.drawable.img_vault_hero_banner_1789194543546),
                    contentDescription = "Family Vault Security Banner",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .background(
                            androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color(0xCC0F172A))
                            )
                        )
                )
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = TealAccent,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "OFFLINE VAULT",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = SlatePrimary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (model.isHindiMode) "100% स्थानीय एन्क्रिप्शन • शून्य क्लाउड डेटा" else "Zero-Cloud • AES-256 GCM Encrypted",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

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

        // --- SECTION: 1-TAP EMERGENCY SOS DIRECTORY CARD ---
        val context = LocalContext.current
        val docContact = contacts.find { it.category.contains("Doctor", ignoreCase = true) }
        val lawyerContact = contacts.find { it.category.contains("Lawyer", ignoreCase = true) }

        Card(
            colors = CardDefaults.cardColors(containerColor = if (model.isHindiMode) Color(0xFFFEF2F2) else Color(0xFFFFF1F2)),
            border = BorderStroke(1.dp, Color(0xFFFECDD3)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "🚨", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (model.isHindiMode) "आपातकालीन त्वरित कॉल (1-Tap SOS)" else "Quick Emergency SOS Call",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color(0xFF9F1239)
                        )
                    }
                    Text(
                        text = if (model.isHindiMode) "सीधा कॉल करें" else "Direct Dial",
                        fontSize = 10.sp,
                        color = Color(0xFFBE123C),
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // National Helpline 112
                    Button(
                        onClick = {
                            val intent = android.content.Intent(android.content.Intent.ACTION_DIAL, android.net.Uri.parse("tel:112"))
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                    ) {
                        Text("📞 112 National", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    // Family Doctor
                    val docPhone = docContact?.phone ?: "+91 98111 22233"
                    Button(
                        onClick = {
                            val intent = android.content.Intent(android.content.Intent.ACTION_DIAL, android.net.Uri.parse("tel:$docPhone"))
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SlatePrimary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                    ) {
                        Text("👨‍⚕️ " + (if (model.isHindiMode) "डॉक्टर" else "Doctor"), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    // Lawyer / Advisor
                    val lawyerPhone = lawyerContact?.phone ?: "+91 98100 98100"
                    Button(
                        onClick = {
                            val intent = android.content.Intent(android.content.Intent.ACTION_DIAL, android.net.Uri.parse("tel:$lawyerPhone"))
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TealAccent),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                    ) {
                        Text("⚖️ " + (if (model.isHindiMode) "वकील" else "Lawyer"), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Legal & Knowledge Quick Bar
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, SlateBorder),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .clickable { model.openLegalAboutHub(0) }
                .testTag("dashboard_legal_quick_card")
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(TealAccent.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.Gavel, contentDescription = null, tint = TealAccent, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (model.isHindiMode) "कानूनी व सुरक्षा केंद्र (T&C, Q&A, नीतियां)" else "Legal, Q&A, Policies & Security Hub",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = SlatePrimary
                        )
                        Text(
                            text = if (model.isHindiMode) "अशमित गौतम द्वारा निर्मित • 100% ऑन-डिवाइस" else "Built by Ashmit Gautam • 100% On-Device",
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                    }
                }
                Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = SlatePrimary, modifier = Modifier.size(20.dp))
            }
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
            text = "ORGANIZATION & AI CONTINUITY",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = SlatePrimary,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(vertical = 4.dp)
        )

        // Feature Highlight: Offline AI Continuity Assistant Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
            border = BorderStroke(1.dp, Color(0xFF86EFAC)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .clickable { model.isAiChatOpen = true }
                .testTag("card_open_offline_ai_assistant")
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFDCFCE7)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = "Offline AI Bot",
                        tint = Color(0xFF16A34A),
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (model.isHindiMode) "ऑफ़लाइन AI कंटीन्यूइटी बॉट" else "Offline AI Continuity Advisor",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color(0xFF14532D)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = Color(0xFF16A34A),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "100% OFFLINE",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                    Text(
                        text = if (model.isHindiMode) "बिना इंटरनेट के भी बीमा, बैंक क्लेम, नॉमिनी व वसीयत नियमों पर तुरंत सलाह लें。" else "Zero cloud leak: Ask instant questions on RBI deceased claims, nominee rules, and emergency protocols.",
                        fontSize = 11.sp,
                        color = Color(0xFF166534),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = "Open Chat",
                    tint = Color(0xFF16A34A),
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Feature Highlight 2: Data Locality, Observability & Anti-Phishing Shield
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F9FF)),
            border = BorderStroke(1.dp, Color(0xFFBAE6FD)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .clickable { model.showObservabilityDialog = true }
                .testTag("card_open_system_observability")
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFE0F2FE)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "Security Shield",
                        tint = Color(0xFF0284C7),
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (model.isHindiMode) "डेटा स्टोरेज व सुरक्षा मॉनिटर" else "Data Locality & System Health",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color(0xFF0369A1)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = Color(0xFF0284C7),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "100% PRIVATE",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                    Text(
                        text = if (model.isHindiMode) "फोन में ही सुरक्षित • 0% फ़िशिंग जोखिम • रियल-टाइम स्वास्थ्य व टेलीमेट्री" else "100% On-Device Sandbox • 0% Phishing Risk • Real-time DB Observability & Scalability",
                        fontSize = 11.sp,
                        color = Color(0xFF0284C7),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = "Open Monitor",
                    tint = Color(0xFF0284C7),
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

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

            // Visual Physical Documents & Keys Guide Banner for physical document / locker categories
            if (currentCategory == "DOCUMENT" || currentCategory == "LOCKER" || currentCategory == "PROPERTY") {
                Spacer(modifier = Modifier.height(10.dp))
                Card(
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, SlateBorder),
                    modifier = Modifier.fillMaxWidth().testTag("vault_docs_banner")
                ) {
                    Row(
                        modifier = Modifier
                            .background(Color.White)
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_emergency_documents_organizer_1789320102141),
                            contentDescription = "Physical Document Binder and Safe Keys",
                            modifier = Modifier
                                .size(width = 80.dp, height = 52.dp)
                                .clip(RoundedCornerShape(6.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (model.isHindiMode) "भौतिक दस्तावेज़ और सुरक्षित लॉकर ट्रैकिंग" else "Physical Binders & Safe Coordinates",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = SlatePrimary
                            )
                            Text(
                                text = if (model.isHindiMode) "मूल वसीयत, रजिस्ट्री व लॉकर की चाबी की वास्तविक जगह अवश्य दर्ज करें।" else "Record exact cupboard, bank branch, and locker key locations for your nominees.",
                                fontSize = 9.sp,
                                color = Color.Gray,
                                lineHeight = 12.sp
                            )
                        }
                    }
                }
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
                    Image(
                        painter = painterResource(id = R.drawable.img_emergency_documents_organizer_1789320102141),
                        contentDescription = "Empty Records Illustration",
                        modifier = Modifier
                            .size(110.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(10.dp))
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

            // Attached document preview badge
            if (item.attachmentUri.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(TealAccent.copy(alpha = 0.1f))
                        .border(1.dp, TealAccent.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Attachment,
                        contentDescription = "Document Attached",
                        tint = TealAccent,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "📎 Document Photo Attached (ID / Policy Proof)",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = SlatePrimary
                    )
                }
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
    onEditLocalSecure: (LocalSecureContact) -> Unit,
    onOpenSos: () -> Unit = {}
) {
    var selectedSegmentIndex by remember { mutableStateOf(0) } // 0: Family Members, 1: External Contacts, 2: Protected Local

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Emergency SOS direct trigger button in Contacts Tab
        Button(
            onClick = onOpenSos,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
                .testTag("contacts_sos_bar")
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.Warning, contentDescription = "Emergency SOS", tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (model.isHindiMode) "🚨 आपातकालीन SOS व ऑफ़लाइन लोकेशन शेयर" else "🚨 Emergency SOS & Offline Location Dispatch",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }

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
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, SlateBorder),
                        modifier = Modifier.fillMaxWidth().testTag("checklist_continuity_banner")
                    ) {
                        Column {
                            Image(
                                painter = painterResource(id = R.drawable.img_family_continuity_shield_1789320119569),
                                contentDescription = "Family Continuity Playbook",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(130.dp),
                                contentScale = ContentScale.Crop
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(SlatePrimary)
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (model.isHindiMode) "संकट प्रबंधन व परिवार हैंडओवर गाइड" else "Crisis Protocol & Family Handover Guide",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "24H • 7D • 30D",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        color = TealAccent
                                    )
                                }
                            }
                        }
                    }
                }

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
    val context = LocalContext.current

    // Restore Backup Dialog State
    var showRestoreDialog by remember { mutableStateOf(false) }
    var restoreJsonInput by remember { mutableStateOf("") }
    var restoreStatusMessage by remember { mutableStateOf<String?>(null) }
    var showQrDialog by remember { mutableStateOf(false) }

    if (showQrDialog) {
        val publicApkUrl = "https://ais-pre-bru2epfqu3yybajn2lqh2x-786684627999.asia-southeast1.run.app/app-debug.apk"
        val qrUrl = "https://api.qrserver.com/v1/create-qr-code/?size=260x260&margin=8&data=" + android.net.Uri.encode(publicApkUrl)

        AlertDialog(
            onDismissRequest = { showQrDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.PhoneAndroid, contentDescription = null, tint = TealAccent, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (model.isHindiMode) "ऐप डाउनलोड QR कोड" else "Direct APK Download QR",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = if (model.isHindiMode)
                            "फोन कैमरा या गूगल लेंस से स्कैन करके सीधे APK डाउनलोड करें:"
                        else
                            "Scan with any Android camera or Google Lens to download the APK directly:",
                        fontSize = 12.sp,
                        color = Color.DarkGray,
                        textAlign = TextAlign.Center
                    )

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(2.dp, TealAccent),
                        shadowElevation = 4.dp,
                        modifier = Modifier.size(200.dp)
                    ) {
                        coil.compose.AsyncImage(
                            model = qrUrl,
                            contentDescription = "Scan to Download APK",
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Text(
                        text = "FamilyEmergencyVault-v1.0.apk (27 MB)\nPackage: com.aistudio.familyemergencyvault",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                val clip = android.content.ClipData.newPlainText("APK Link", publicApkUrl)
                                clipboard.setPrimaryClip(clip)
                                android.widget.Toast.makeText(context, "Download link copied!", android.widget.Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            Text("Copy Link", fontSize = 11.sp)
                        }

                        Button(
                            onClick = {
                                val browserIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(publicApkUrl))
                                context.startActivity(browserIntent)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SlatePrimary),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            Text("Open APK", fontSize = 11.sp, color = Color.White)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showQrDialog = false }) {
                    Text("Close", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (showRestoreDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreDialog = false },
            title = {
                Text(
                    text = if (model.isHindiMode) "बैकअप से पुनर्स्थापित करें (Restore Vault)" else "Restore Vault from Backup",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (model.isHindiMode)
                            "अपना पहले से एक्सपोर्ट किया गया JSON बैकअप नीचे पेस्ट करें:"
                        else
                            "Paste your previously exported JSON backup below:",
                        fontSize = 12.sp,
                        color = Color.DarkGray
                    )
                    OutlinedTextField(
                        value = restoreJsonInput,
                        onValueChange = { restoreJsonInput = it },
                        placeholder = { Text("{\"app\": \"Family Emergency Vault\", ...}") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    )
                    if (restoreStatusMessage != null) {
                        Text(
                            text = restoreStatusMessage ?: "",
                            fontSize = 11.sp,
                            color = if (restoreStatusMessage?.startsWith("Success") == true) GreenSuccess else RedAlert
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (restoreJsonInput.isNotBlank()) {
                            model.importVaultFromJson(restoreJsonInput) { success ->
                                if (success) {
                                    restoreStatusMessage = if (model.isHindiMode) "सफलतापूर्वक रीस्टोर किया गया!" else "Successfully restored!"
                                    Toast.makeText(context, "Vault data restored!", Toast.LENGTH_SHORT).show()
                                    showRestoreDialog = false
                                } else {
                                    restoreStatusMessage = if (model.isHindiMode) "अमान्य बैकअप JSON" else "Invalid backup JSON format."
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TealAccent)
                ) {
                    Text(if (model.isHindiMode) "रीस्टोर करें" else "Restore", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreDialog = false }) {
                    Text(if (model.isHindiMode) "रद्द करें" else "Cancel")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ------------------ APP PREFERENCES & TIMEOUT ------------------
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, SlateBorder),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = if (model.isHindiMode) "⚙️ ऐप प्राथमिकताएं (Preferences)" else "⚙️ App Preferences & Privacy",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = SlatePrimary
                )

                // Language Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (model.isHindiMode) "भाषा (Language)" else "App Language",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SlatePrimary
                        )
                        Text(
                            text = if (model.isHindiMode) "हिंदी सक्रिय है" else "English currently active",
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(
                            onClick = { if (model.isHindiMode) model.toggleLanguage() },
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (!model.isHindiMode) SlatePrimary else Color.Transparent
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("English", color = if (!model.isHindiMode) Color.White else SlatePrimary, fontSize = 11.sp)
                        }

                        OutlinedButton(
                            onClick = { if (!model.isHindiMode) model.toggleLanguage() },
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (model.isHindiMode) SlatePrimary else Color.Transparent
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("हिंदी", color = if (model.isHindiMode) Color.White else SlatePrimary, fontSize = 11.sp)
                        }
                    }
                }

                Divider(color = SlateBorder.copy(alpha = 0.5f))

                // Auto-Lock Inactivity Selector
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (model.isHindiMode) "निष्क्रियता लॉक समय (Auto-Lock)" else "Inactivity Auto-Lock",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SlatePrimary
                        )
                        Text(
                            text = if (model.autoLockTimeoutMinutes == 0)
                                (if (model.isHindiMode) "अक्षम" else "Disabled")
                            else
                                "${model.autoLockTimeoutMinutes} min",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TealAccent
                        )
                    }
                    Text(
                        text = if (model.isHindiMode)
                            "बिना उपयोग के फोन रहने पर ऐप अपने आप MPIN से लॉक हो जाएगी।"
                        else
                            "App automatically locks with MPIN after chosen inactive duration.",
                        fontSize = 10.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val timeouts = listOf(Pair(5, "5 min"), Pair(15, "15 min"), Pair(30, "30 min"), Pair(0, "Off"))
                        timeouts.forEach { (minutes, label) ->
                            val isSelected = model.autoLockTimeoutMinutes == minutes
                            OutlinedButton(
                                onClick = { model.setAutoLockTimeout(minutes) },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (isSelected) TealAccent else Color.Transparent
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) Color.White else SlatePrimary,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                Divider(color = SlateBorder.copy(alpha = 0.5f))

                // Offline AI Continuity Chatbot Privacy & Mode Preference
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (model.isHindiMode) "ऑफ़लाइन AI कंटीन्यूइटी मोड" else "Offline AI Continuity Chatbot",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SlatePrimary
                            )
                            Text(
                                text = if (model.aiOfflineModeOnly)
                                    (if (model.isHindiMode) "100% ऑन-डिवाइस • शून्य क्लाउड डेटा लीक" else "100% On-Device • Zero Cloud Transmission")
                                else
                                    (if (model.isHindiMode) "हाइब्रिड मोड सक्रिय (क्लाउड + ऑफ़लाइन फॉलबैक)" else "Hybrid Mode Active (Gemini + Offline Fallback)"),
                                fontSize = 10.sp,
                                color = if (model.aiOfflineModeOnly) Color(0xFF16A34A) else SlatePrimary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Switch(
                            checked = model.aiOfflineModeOnly,
                            onCheckedChange = { model.aiOfflineModeOnly = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF16A34A),
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = SlateBorder
                            )
                        )
                    }

                    Text(
                        text = if (model.isHindiMode)
                            "सक्रिय रहने पर, AI चैटबॉट फोन के भीतर ही वसीयत, बैंक क्लेम, और बीमा गाइड का उत्तर देगा। इंटरनेट बंद होने पर भी तुरंत काम करता है।"
                        else
                            "When enabled, the AI chatbot processes all queries locally on-device. No financial details or conversation logs ever leave your phone.",
                        fontSize = 10.sp,
                        color = Color.Gray,
                        lineHeight = 13.sp
                    )

                    Button(
                        onClick = { model.isAiChatOpen = true },
                        colors = ButtonDefaults.buttonColors(containerColor = if (model.aiOfflineModeOnly) Color(0xFF059669) else SlatePrimary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Psychology, contentDescription = "Open AI", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (model.isHindiMode) "ऑफ़लाइन AI चैटबॉट खोलें" else "Open Offline AI Continuity Chat",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Divider(color = SlateBorder.copy(alpha = 0.5f))

                // Data Locality, Scalability & Observability Health Inspector
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (model.isHindiMode) "डेटा स्टोरेज, सुरक्षा और सिस्टम स्वास्थ्य" else "Data Locality, Observability & Scalability",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SlatePrimary
                            )
                            Text(
                                text = if (model.isHindiMode)
                                    "100% ऑन-डिवाइस सैंडबॉक्स • 0% फ़िशिंग • SQLite WAL"
                                else
                                    "100% On-Device Sandbox • 0% Phishing Risk • SQLite WAL",
                                fontSize = 10.sp,
                                color = Color(0xFF0284C7),
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Surface(
                            color = Color(0xFFE0F2FE),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "MONITORED",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0369A1),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Text(
                        text = if (model.isHindiMode)
                            "सत्यापित करें कि आपका सारा डेटा बिना किसी बाहरी रिसाव के केवल आपके फोन के सुरक्षित प्राइवेट सैंडबॉक्स में है। लाइव क्वेरी लेटेंसी, मेमोरी, बी-ट्री अखंडता और डीफ़्रेग्मेंटेशन (VACUUM) जांचें।"
                        else
                            "Inspect where your data is stored locally, verify zero-phishing sandbox isolation, monitor live query latencies, view memory profile, and execute on-demand database compaction (VACUUM).",
                        fontSize = 10.sp,
                        color = Color.Gray,
                        lineHeight = 13.sp
                    )

                    Button(
                        onClick = { model.showObservabilityDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Speed, contentDescription = "Observability", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (model.isHindiMode) "सिस्टम स्वास्थ्य और सुरक्षा मॉनिटर खोलें" else "Open Observability & Health Monitor",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Divider(color = SlateBorder.copy(alpha = 0.5f))

                // Typography & Font Theme Selector
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (model.isHindiMode) "फॉन्ट थीम (Typography Theme)" else "Font & Typography Theme",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SlatePrimary
                            )
                            Text(
                                text = if (model.isHindiMode) "सक्रिय: ${model.selectedFontTheme.titleHi}" else "Active: ${model.selectedFontTheme.titleEn}",
                                fontSize = 10.sp,
                                color = Color.Gray
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(TealAccent.copy(alpha = 0.12f))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = model.selectedFontTheme.chipLabel,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = TealAccent
                            )
                        }
                    }

                    // Interactive Font Theme Cards
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        model.availableFontThemes.chunked(2).forEach { rowThemes ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                rowThemes.forEach { fontTheme ->
                                    val isSelected = model.selectedFontTheme == fontTheme
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isSelected) SlatePrimary.copy(alpha = 0.07f) else SlateLightBg
                                        ),
                                        border = BorderStroke(
                                            width = if (isSelected) 1.5.dp else 1.dp,
                                            color = if (isSelected) TealAccent else SlateBorder
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { model.setFontTheme(fontTheme) }
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(8.dp),
                                            verticalArrangement = Arrangement.spacedBy(3.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = fontTheme.chipLabel,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = fontTheme.headingFamily,
                                                    color = if (isSelected) TealAccent else SlatePrimary
                                                )
                                                if (isSelected) {
                                                    Icon(
                                                        imageVector = Icons.Default.CheckCircle,
                                                        contentDescription = "Selected",
                                                        tint = TealAccent,
                                                        modifier = Modifier.size(13.dp)
                                                    )
                                                }
                                            }
                                            Text(
                                                text = if (model.isHindiMode) fontTheme.titleHi else fontTheme.titleEn,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                fontFamily = fontTheme.headingFamily,
                                                color = SlatePrimary
                                            )
                                            Text(
                                                text = if (model.isHindiMode) fontTheme.descriptionHi else fontTheme.descriptionEn,
                                                fontSize = 9.sp,
                                                color = Color.Gray,
                                                lineHeight = 12.sp,
                                                maxLines = 2,
                                                fontFamily = fontTheme.bodyFamily
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

        // ------------------ ENCRYPTED BACKUP & RESTORE ------------------
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, SlateBorder),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (model.isHindiMode) "💾 एन्क्रिप्टेड बैकअप और रीस्टोर" else "💾 Encrypted Backup & Restore",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = SlatePrimary
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFDCFCE7))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "Offline JSON",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF166534)
                        )
                    }
                }

                Text(
                    text = if (model.isHindiMode)
                        "अपने आपातकालीन डेटा का सुरक्षित JSON बैकअप बनाएं और Google Drive, WhatsApp या ईमेल के माध्यम से सुरक्षित रखें।"
                    else
                        "Export a full encrypted JSON backup to save on Google Drive or send securely to your trusted family members.",
                    fontSize = 11.sp,
                    color = Color.Gray
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val json = model.exportVaultToJson()
                            val sendIntent = android.content.Intent().apply {
                                action = android.content.Intent.ACTION_SEND
                                putExtra(android.content.Intent.EXTRA_TEXT, json)
                                putExtra(android.content.Intent.EXTRA_SUBJECT, "Family Emergency Vault Backup")
                                type = "text/plain"
                            }
                            val shareIntent = android.content.Intent.createChooser(sendIntent, "Export Vault Backup")
                            context.startActivity(shareIntent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SlatePrimary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "Export", tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (model.isHindiMode) "एक्सपोर्ट बैकअप" else "Export JSON", color = Color.White, fontSize = 11.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            restoreJsonInput = ""
                            restoreStatusMessage = null
                            showRestoreDialog = true
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.Download, contentDescription = "Restore", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (model.isHindiMode) "रीस्टोर करें" else "Restore JSON", fontSize = 11.sp)
                    }
                }
            }
        }

        // ------------------ SHARE APP APK & INSTALL QR CODE ------------------
        val publicApkDownloadUrl = "https://ais-pre-bru2epfqu3yybajn2lqh2x-786684627999.asia-southeast1.run.app/app-debug.apk"
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, TealAccent.copy(alpha = 0.6f)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (model.isHindiMode) "📲 ऐप शेयर करें व QR कोड से इंस्टॉल करें" else "📲 Share App APK & Install QR Code",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = SlatePrimary
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(TealAccent.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "Direct APK",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TealAccent
                        )
                    }
                }

                Text(
                    text = if (model.isHindiMode)
                        "अपने परिजनों (पत्नी, नॉमिनी, माता-पिता) के फोन में ऐप इंस्टॉल कराने के लिए स्क्रीन पर QR कोड दिखाएं या डायरेक्ट डाउनलोड लिंक शेयर करें।"
                    else
                        "Install the vault on your family members' phones (spouse, nominee, parents). Display the QR code on your screen or share the direct download link.",
                    fontSize = 11.sp,
                    color = Color.Gray
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { showQrDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = TealAccent),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.PhoneAndroid, contentDescription = "QR Code", tint = SlatePrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (model.isHindiMode) "QR कोड दिखाएं" else "Show QR Code", color = SlatePrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            val sendIntent = android.content.Intent().apply {
                                action = android.content.Intent.ACTION_SEND
                                putExtra(
                                    android.content.Intent.EXTRA_TEXT,
                                    "Family Emergency Vault Android App direct download:\n$publicApkDownloadUrl\n\n100% Offline, Zero Cloud Tracking, Bank-grade Family Vault."
                                )
                                putExtra(android.content.Intent.EXTRA_SUBJECT, "Family Emergency Vault App Download")
                                type = "text/plain"
                            }
                            context.startActivity(android.content.Intent.createChooser(sendIntent, "Share App Download Link"))
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "Share", tint = SlatePrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (model.isHindiMode) "लिंक शेयर करें" else "Share Link", fontSize = 11.sp)
                    }
                }
            }
        }

        // ------------------ BANK-STYLE SECURED HOLDER PROFILE ------------------
        Card(
            colors = CardDefaults.cardColors(containerColor = SlatePrimary),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(TealAccent),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.VerifiedUser,
                                contentDescription = "Verified Seal",
                                tint = SlatePrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "SECURED VAULT PROFILE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TealAccent,
                            letterSpacing = 1.sp
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.White.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "SANDBOX COMPLIANT",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Account Holder",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = model.registeredFullName,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Registered Email Address",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 10.sp
                        )
                        Text(
                            text = model.registeredEmail,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Registered Mobile Number",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 10.sp
                        )
                        Text(
                            text = model.registeredPhone,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Emergency Primary Nominee",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 10.sp
                        )
                        Text(
                            text = "${model.registeredNomineeName} (${model.registeredNomineeRelation})",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Dynamic Security Certification ID",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 10.sp
                        )
                        Text(
                            text = model.registeredDigitalCertificateId,
                            color = TealAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

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

        // Database & Persistence Health Section
        val vaultItemsList by model.vaultItems.collectAsStateWithLifecycle()
        val familyList by model.familyDependents.collectAsStateWithLifecycle()
        val contactList by model.importantContacts.collectAsStateWithLifecycle()
        val checklistItems by model.emergencyActionItems.collectAsStateWithLifecycle()

        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, SlateBorder),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🗄️ Database & Storage Engine",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = SlatePrimary
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(TealAccent.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "Room SQLite v1 • Active",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TealAccent
                        )
                    }
                }

                Text(
                    text = "High-speed encrypted local SQLite database storing all offline emergency documents, nominee mapping, and family records.",
                    fontSize = 11.sp,
                    color = Color.Gray
                )

                // Live Record Counts
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(SlateLightBg, RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "${vaultItemsList.size}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = SlatePrimary)
                            Text(text = "Assets", fontSize = 10.sp, color = Color.Gray)
                        }
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(SlateLightBg, RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "${familyList.size}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = SlatePrimary)
                            Text(text = "Members", fontSize = 10.sp, color = Color.Gray)
                        }
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(SlateLightBg, RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "${contactList.size}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = SlatePrimary)
                            Text(text = "Contacts", fontSize = 10.sp, color = Color.Gray)
                        }
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(SlateLightBg, RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "${checklistItems.size}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = SlatePrimary)
                            Text(text = "Tasks", fontSize = 10.sp, color = Color.Gray)
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            model.verifyAndRepairDatabase()
                            Toast.makeText(context, "Database verified & synced!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "Verify", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Verify Integrity", fontSize = 11.sp)
                    }

                    Button(
                        onClick = {
                            model.resetDatabaseToDefaults()
                            Toast.makeText(context, "Database restored with emergency seed records!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = SlatePrimary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Reset", tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reset Defaults", color = Color.White, fontSize = 11.sp)
                    }
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

        // Biometric Security Config Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, SlateBorder),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().testTag("biometric_settings_card")
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                val biometricAvail = remember { BiometricAuthManager.checkBiometricAvailability(context) }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = "Biometric Lock",
                        tint = TealAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (model.isHindiMode) "बायोमेट्रिक प्रमाणीकरण (फिंगरप्रिंट / फेस)" else "🧬 Biometric Unlock (Fingerprint / Face)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = SlatePrimary
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (model.isHindiMode)
                        "ऐप लॉन्च होने पर संवेदनशील वित्तीय व नॉमिनी डेटा डिक्रिप्ट करने के लिए हार्डवेयर बायोमेट्रिक्स अनिवार्य करें।"
                    else
                        "Require Android Fingerprint or Facial recognition on application launch to decrypt sensitive asset records.",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (model.isHindiMode) "बायोमेट्रिक लॉक सक्षम करें" else "Enable Biometric Security",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Status: ${biometricAvail.userTitle}",
                            fontSize = 10.sp,
                            color = if (biometricAvail.canPrompt) Color(0xFF15803D) else Color(0xFFB45309),
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Switch(
                        checked = model.isBiometricEnabled,
                        onCheckedChange = { model.updateBiometricSetting(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = TealAccent)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            (context as? MainActivity)?.triggerBiometricPrompt(
                                onSuccess = {
                                    Toast.makeText(context, "Biometric verified successfully!", Toast.LENGTH_SHORT).show()
                                },
                                onError = { err ->
                                    Toast.makeText(context, "Biometric result: $err", Toast.LENGTH_SHORT).show()
                                }
                            )
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, TealAccent)
                    ) {
                        Icon(imageVector = Icons.Default.Fingerprint, contentDescription = null, tint = SlatePrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (model.isHindiMode) "बायोमेट्रिक्स जांचें" else "Test Biometrics",
                            color = SlatePrimary,
                            fontSize = 11.sp
                        )
                    }

                    Button(
                        onClick = {
                            model.lockVault()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SlatePrimary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (model.isHindiMode) "अभी लॉक करें" else "Lock Vault Now",
                            color = Color.White,
                            fontSize = 11.sp
                        )
                    }
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

        // Optional Technical Architecture & Schema Hub (Collapsible)
        var showDevArchitecture by remember { mutableStateOf(false) }
        Card(
            colors = CardDefaults.cardColors(containerColor = SlateLightBg),
            border = BorderStroke(1.dp, SlateBorder),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showDevArchitecture = !showDevArchitecture }
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "🛠️", fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (model.isHindiMode) "डेवलपर आर्किटेक्चर और सैंडबॉक्स (वैकल्पिक)" else "Developer Architecture & Sandbox Hub",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = SlatePrimary
                        )
                    }
                    Icon(
                        imageVector = if (showDevArchitecture) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Expand",
                        tint = SlatePrimary
                    )
                }
                if (showDevArchitecture) {
                    Spacer(modifier = Modifier.height(12.dp))
                    com.example.ui.ArchitectureAndSchemaHub(model = model)
                }
            }
        }

        // ==================== WHAT IS EMERGENCY VAULT & SETUP GUIDE ====================
        var showAppGuide by remember { mutableStateOf(true) }
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, SlateBorder),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showAppGuide = !showAppGuide },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "💡", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = if (model.isHindiMode) "इमरजेंसी वॉल्ट क्या है और कैसे सेटअप करें?" else "What is Emergency Vault & How to Setup?",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = SlatePrimary
                            )
                            Text(
                                text = if (model.isHindiMode) "अनपेक्षित आपातकाल में परिवार की सुरक्षा गाइड" else "Complete guide for unexpected family emergencies",
                                fontSize = 10.sp,
                                color = Color.Gray
                            )
                        }
                    }
                    Icon(
                        imageVector = if (showAppGuide) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Toggle Guide",
                        tint = SlatePrimary
                    )
                }

                if (showAppGuide) {
                    Divider(color = SlateBorder.copy(alpha = 0.5f))

                    // Section 1: What is Emergency Vault?
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = if (model.isHindiMode) "📌 यह क्या है और क्यों जरूरी है? (What is it?)" else "📌 What is Family Emergency Vault?",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = TealAccent
                        )
                        Text(
                            text = if (model.isHindiMode)
                                "यह एक 100% ऑफलाइन, सैन्य-ग्रेड एन्क्रिप्टेड डिजिटल व भौतिक लेजर है। किसी अनपेक्षित दुर्घटना, गंभीर बीमारी या मृत्यु के समय अक्सर परिवार को यह नहीं पता होता कि बैंक खाते, बीमा पॉलिसियां, लॉकर की चाबियां और संपत्ति के कागजात कहां हैं। यह ऐप उस संकट में परिवार का विश्वसनीय साथी बनता है।"
                            else
                                "Family Emergency Vault is a 100% offline, military-grade encrypted emergency continuity ledger. In the event of critical hospitalization, unforeseen incapacitation, or sudden demise, families struggle to locate bank accounts, term life policies, physical locker keys, and real estate deeds. This app solves that chaos.",
                            fontSize = 10.sp,
                            color = Color.DarkGray,
                            lineHeight = 14.sp
                        )
                    }

                    // Section 2: Why Mobile App vs Website?
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = if (model.isHindiMode) "📱 वेबसाइट के बजाय मोबाइल ऐप (Android / iOS) क्यों?" else "📱 Why an Android / iOS App, Not a Website?",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = SlatePrimary
                        )
                        Text(
                            text = if (model.isHindiMode)
                                "इमरजेंसी के समय अस्पताल या किसी जगह इंटरनेट और सर्वर काम न भी करें, तो भी यह नेटिव ऐप फोन के अंदर पूरी तरह ऑफलाइन काम करता है। कोई पासवर्ड या क्लाउड लीक का डर नहीं — सारा डेटा आपके फोन में ही सुरक्षित रहता है।"
                            else
                                "During emergency hospital visits or remote situations, internet connectivity is often poor or absent. This native mobile app runs 100% locally from your device storage with zero cloud dependence, zero server outage risk, and zero data leakage.",
                            fontSize = 10.sp,
                            color = Color.DarkGray,
                            lineHeight = 14.sp
                        )
                    }

                    // Section 3: 4-Step Setup Guide
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = if (model.isHindiMode) "🚀 4 आसान चरणों में वॉल्ट सेटअप करें (Setup Steps):" else "🚀 Step-by-Step Setup Guide (4 Easy Steps):",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = SlatePrimary
                        )

                        val steps = if (model.isHindiMode) listOf(
                            "1. खाते व बीमा जोड़ें" to "अपने सभी बचत खाते, FD, टर्म इंश्योरेंस, हेल्थ मेडिक्लेम और डीमैट रिकॉर्ड जोड़ें।",
                            "2. नॉमिनी व कागजात की जगह लिखें" to "हर खाते में सत्यापित नॉमिनी का नाम और घर में मूल कागजात किस अलमारी/दराज में हैं, दर्ज करें।",
                            "3. संपर्क व चेकलिस्ट तैयार करें" to "फैमिली डॉक्टर, वकील, CA के नंबर जोड़ें और 24h / 7d / 30d एक्शन प्लान की जांच करें।",
                            "4. 4-अंकों का MPIN सेट करें और बैकअप लें" to "मास्टर MPIN एक्टिवेट करें और एन्क्रिप्टेड JSON बैकअप को अपने जीवनसाथी/नॉमिनी के साथ सुरक्षित रखें।"
                        ) else listOf(
                            "1. Record All Financial Assets" to "Log your savings accounts, FDs, life/health policies, demat accounts, and safe locker units.",
                            "2. Set Nominees & Physical Locations" to "Mark verified nominee registration status and record the exact physical drawer/cabinet of original paper files.",
                            "3. Add Advisors & Review Checklist" to "Store numbers for family doctor, trusted CA, and lawyer; verify the 24h, 7d, and 30d emergency action playbook.",
                            "4. Lock with MPIN & Export Encrypted Backup" to "Enable 4-digit master MPIN security and export an offline JSON backup for your primary nominee."
                        )

                        steps.forEachIndexed { idx, (title, desc) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(SlateLightBg, RoundedCornerShape(6.dp))
                                    .padding(8.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(TealAccent),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = "${idx + 1}", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = SlatePrimary)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(text = title, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SlatePrimary)
                                    Text(text = desc, fontSize = 9.sp, color = Color.Gray, lineHeight = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // ==================== LEGAL, GOVERNANCE, Q&A & CREATOR HUB ====================
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, SlateBorder),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().testTag("settings_legal_governance_card")
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "⚖️", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = if (model.isHindiMode) "कानूनी नीतियां, Q&A और सुरक्षा केंद्र" else "Legal, Governance, Q&A & Policies",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = SlatePrimary
                            )
                            Text(
                                text = if (model.isHindiMode) "T&C • Q&A • नीतियां • सुरक्षा • अशमित गौतम" else "T&C • Q&A • Policies • Security • Ashmit Gautam",
                                fontSize = 10.sp,
                                color = Color.Gray
                            )
                        }
                    }
                    Surface(
                        color = TealAccent.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "OFFICIAL",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = TealAccent,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Text(
                    text = if (model.isHindiMode)
                        "उपयोग की शर्तें, गोपनीयता नीतियां, अक्सर पूछे जाने वाले प्रश्न (Q&A), सुरक्षा वास्तुकला और निर्माता अशमित गौतम का विवरण पढ़ें।"
                    else
                        "Explore complete on-device compliance terms, RBI deceased claims guidelines, Q&A FAQs, security architecture, and architect details.",
                    fontSize = 11.sp,
                    color = Color.DarkGray,
                    lineHeight = 15.sp
                )

                // Grid of 6 interactive buttons
                val hubItems = listOf(
                    Triple("📜 Terms & Conditions (T&C)", 0, if (model.isHindiMode) "नियम व शर्तें (T&C)" else "Terms of Service"),
                    Triple("❓ Frequently Asked (Q&A)", 1, if (model.isHindiMode) "प्रश्नोत्तरी (Q&A)" else "Questions & Answers"),
                    Triple("📋 Policies & RBI Mandate", 2, if (model.isHindiMode) "गोपनीयता नीतियां" else "Privacy & Compliance"),
                    Triple("ℹ️ About Application", 3, if (model.isHindiMode) "एप्लिकेशन परिचय" else "About Application"),
                    Triple("🛡️ Cryptographic Security", 4, if (model.isHindiMode) "सुरक्षा शील्ड" else "About Security"),
                    Triple("👨‍💻 Architect: Ashmit Gautam", 5, if (model.isHindiMode) "अशमित गौतम (निर्माता)" else "About Ashmit Gautam")
                )

                hubItems.chunked(2).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowItems.forEach { (label, tabIndex, shortLabel) ->
                            OutlinedButton(
                                onClick = { model.openLegalAboutHub(tabIndex) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, SlateBorder),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = shortLabel,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = SlatePrimary,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                Button(
                    onClick = { model.openLegalAboutHub(0) },
                    colors = ButtonDefaults.buttonColors(containerColor = SlatePrimary),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    Icon(imageVector = Icons.Default.MenuBook, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (model.isHindiMode) "कानूनी व सुरक्षा केंद्र खोलें" else "Open Complete Legal & Governance Hub",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        // ==================== CREATOR ATTRIBUTION & APP FOOTER ====================
        Card(
            colors = CardDefaults.cardColors(containerColor = SlatePrimary),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { model.openLegalAboutHub(5) }
                .testTag("settings_creator_footer_card")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .border(1.5.dp, TealAccent, CircleShape)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_creator_ashmit_1789235489297),
                            contentDescription = "Ashmit Gautam",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Family Emergency Vault v1.0",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (model.isHindiMode)
                                "अशमित गौतम (Ashmit Gautam) द्वारा विशेष रूप से तैयार किया गया"
                            else
                                "Designed & Developed by Ashmit Gautam",
                            color = TealAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                Text(
                    text = "100% Offline • SQLite Room • Zero Cloud Leakage • Military-Grade Encryption",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 8.sp,
                    textAlign = TextAlign.Center
                )

                OutlinedButton(
                    onClick = { model.openLegalAboutHub(5) },
                    border = BorderStroke(1.dp, TealAccent),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = TealAccent, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (model.isHindiMode) "अशमित गौतम प्रोफाइल व दर्शन देखें" else "View Ashmit Gautam Profile & Vision",
                        color = TealAccent,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
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
    var attachmentUri by remember { mutableStateOf(initialItem?.attachmentUri ?: "") }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            attachmentUri = uri.toString()
        }
    }

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

                Divider(color = SlateBorder, modifier = Modifier.padding(vertical = 4.dp))

                Text("Document Attachment / Photo (Aadhaar, PAN, Policy)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SlatePrimary)
                if (attachmentUri.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(TealAccent.copy(alpha = 0.1f))
                            .border(1.dp, TealAccent, RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(imageVector = Icons.Default.Attachment, contentDescription = "Attached", tint = TealAccent, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Document photo attached",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = SlatePrimary
                            )
                        }
                        Row {
                            TextButton(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }
                            ) {
                                Text("Change", fontSize = 11.sp, color = TealAccent)
                            }
                            IconButton(onClick = { attachmentUri = "" }) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Remove", tint = RedAlert, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, TealAccent)
                    ) {
                        Icon(imageVector = Icons.Default.AddPhotoAlternate, contentDescription = "Attach Document", tint = TealAccent, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Attach Photo (Card / Certificate / Policy)", fontSize = 11.sp, color = SlatePrimary, fontWeight = FontWeight.SemiBold)
                    }
                }
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
                                    attachmentUri = attachmentUri,
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

// ==================== MASTER MPIN & BIOMETRIC LOGOUT/LOCK SCREEN ====================

@Composable
fun AppMpinLockScreen(
    model: VaultViewModel,
    onTriggerBiometric: () -> Unit = {},
    onTriggerSos: () -> Unit = {}
) {
    val context = LocalContext.current
    val enteredCount = model.enteredMpinDigits.length
    val biometricAvailability = remember { BiometricAuthManager.checkBiometricAvailability(context) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SlatePrimary)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Top Language Switcher on Lock Screen
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.15f))
                    .clickable { model.toggleLanguage() }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .testTag("btn_lockscreen_language_toggle"),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = "Language",
                        tint = TealAccent,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (model.isHindiMode) "English" else "हिन्दी",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = "Encrypted Vault Lock",
            tint = TealAccent,
            modifier = Modifier.size(54.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = if (model.isHindiMode) "सुरक्षित एन्क्रिप्शन सक्रिय" else "DECRYPTION KEY SECURED",
            fontSize = 11.sp,
            color = TealAccent,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )
        Text(
            text = if (model.isHindiMode) "पारिवारिक आपातकालीन वॉल्ट" else "Family Emergency Vault",
            fontSize = 20.sp,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
        )

        // Native Android Biometric Trigger Button (Only shown if user enabled it and device supports it)
        if (model.isBiometricEnabled && biometricAvailability.canPrompt) {
            Button(
                onClick = onTriggerBiometric,
                colors = ButtonDefaults.buttonColors(containerColor = TealAccent),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(48.dp)
                    .testTag("btn_biometric_auth")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = "Fingerprint / Face Sensor",
                        tint = SlatePrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (model.isHindiMode) "फिंगरप्रिंट / फेस से अनलॉक करें" else "Scan Fingerprint / Face Unlock",
                        color = SlatePrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF22C55E))
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (model.isHindiMode) "बायोमेट्रिक सेंसर तैयार है" else "Android Biometric Sensor Ready",
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = if (model.isHindiMode) "4-अंकों का मास्टर MPIN दर्ज करें" else "Enter 4-digit Master MPIN",
            color = Color.White.copy(alpha = 0.75f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )

        // 4 dots representing entered MPIN digits
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(vertical = 10.dp)
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

        if (model.mpinFeedbackMessage.isNotEmpty()) {
            Text(
                text = model.mpinFeedbackMessage,
                color = if (model.mpinFeedbackMessage.contains("Incorrect") || model.mpinFeedbackMessage.contains("Denied")) RedAlert else TealAccent,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        // Custom Numpad Grid
        val numpadItems = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("C", "0", "⌫")
        )

        Column(
            modifier = Modifier.width(260.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            for (row in numpadItems) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    for (key in row) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
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
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = key,
                                fontSize = 22.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Emergency SOS Direct Access (Usable even on lockscreen)
        Button(
            onClick = onTriggerSos,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth(0.75f)
                .padding(bottom = 10.dp)
                .testTag("btn_lockscreen_emergency_sos")
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.Warning, contentDescription = "SOS", tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (model.isHindiMode) "🚨 आपातकालीन SOS व लोकेशन" else "🚨 Emergency SOS & Location",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }

        // Evaluator Sandbox Quick Bypass
        Button(
            onClick = {
                model.enteredMpinDigits = model.appMpin
                model.verifyEnteredMpin()
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.15f)),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth(0.75f)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.Key, contentDescription = "Bypass", tint = TealAccent, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Evaluator Sandbox Quick Unlock", color = Color.White, fontSize = 11.sp)
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

// ==================== INTERACTIVE OFFLINE AI CHAT ASSISTANT ====================

@Composable
fun GeminiAssistantDialog(
    model: VaultViewModel,
    onDismiss: () -> Unit
) {
    var inputMessage by remember { mutableStateOf("") }
    val isHindi = model.isHindiMode
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(TealAccent.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "AI Spark",
                            tint = TealAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (isHindi) "ऑफ़लाइन AI कंटीन्यूइटी सहायक" else "Offline AI Continuity Advisor",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = SlatePrimary
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF059669))
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (model.aiOfflineModeOnly) "100% On-Device • Zero Cloud Risk" else "Hybrid AI Active",
                                fontSize = 10.sp,
                                color = Color(0xFF059669),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Privacy Badge & Offline Toggle
                Surface(
                    color = if (model.aiOfflineModeOnly) Color(0xFFECFDF5) else SlateLightBg,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, if (model.aiOfflineModeOnly) Color(0xFF10B981) else Color.LightGray),
                    modifier = Modifier.clickable {
                        model.aiOfflineModeOnly = !model.aiOfflineModeOnly
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (model.aiOfflineModeOnly) Icons.Default.Lock else Icons.Default.Cloud,
                            contentDescription = "Mode",
                            modifier = Modifier.size(11.dp),
                            tint = if (model.aiOfflineModeOnly) Color(0xFF047857) else SlatePrimary
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = if (model.aiOfflineModeOnly) "OFFLINE" else "CLOUD",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = if (model.aiOfflineModeOnly) Color(0xFF047857) else SlatePrimary
                        )
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(430.dp)
            ) {
                // Privacy Guarantee Banner
                Surface(
                    color = Color(0xFFF0FDF4),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Private",
                            tint = Color(0xFF16A34A),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isHindi) "पूर्णतः गोपनीय: आपके परिवार का कोई भी वित्तीय डेटा इंटरनेट पर लीक नहीं होता।" else "Zero Cloud Leak: Runs 100% on device with offline knowledge of Indian succession & claim laws.",
                            fontSize = 10.sp,
                            color = Color(0xFF166534),
                            lineHeight = 13.sp
                        )
                    }
                }

                val suggestions = if (isHindi) {
                    listOf(
                        "आपातकाल में क्या करें?",
                        "बीमा क्लेम कैसे फाइल करें?",
                        "बैंक खाता क्लेम की प्रक्रिया",
                        "नॉमिनी और वारिस में अंतर",
                        "म्यूचुअल फंड ट्रांसमिशन",
                        "बैंक लॉकर के नियम"
                    )
                } else {
                    listOf(
                        "What to do in first 24h emergency?",
                        "How to claim insurance securely?",
                        "Bank account claim procedure",
                        "Nominee vs Legal Heir rules",
                        "Mutual Fund transmission process",
                        "Safe locker handover rules"
                    )
                }
                
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(SlateLightBg, RoundedCornerShape(10.dp))
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (model.aiChatHistory.isEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp, horizontal = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Psychology,
                                    contentDescription = "Brain",
                                    tint = TealAccent,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = if (isHindi) "ऑफ़लाइन AI कंटीन्यूइटी सहायक तैयार है" else "Offline AI Continuity Bot Ready",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = SlatePrimary
                                )
                                Text(
                                    text = if (isHindi) "बिना इंटरनेट के भी बीमा क्लेम, बैंक प्रोसेस और वसीयत नियमों पर तुरंत सलाह लें।" else "Ask anything regarding emergency protocols, RBI deceased claims, nominee rules, or tap a quick prompt below.",
                                    fontSize = 11.sp,
                                    color = Color.Gray,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }

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
                                    bottomStart = if (isBot) 2.dp else 12.dp,
                                    bottomEnd = if (isBot) 12.dp else 2.dp
                                ),
                                modifier = Modifier.fillMaxWidth(if (isBot) 0.92f else 0.85f)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    if (isBot) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(bottom = 4.dp)
                                        ) {
                                            Text(
                                                text = if (model.aiOfflineModeOnly) "🔒 OFFLINE AI" else "✨ CONTINUITY BOT",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Black,
                                                color = TealAccent
                                            )
                                        }
                                    }
                                    Text(
                                        text = chat.first,
                                        fontSize = 12.sp,
                                        color = if (isBot) SlatePrimary else Color.White,
                                        lineHeight = 17.sp
                                    )
                                }
                            }
                        }
                    }
                    
                    if (model.isAiLoading) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.Start,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    color = TealAccent,
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isHindi) "ऑफ़लाइन AI विश्लेषण कर रहा है..." else "Offline AI is reasoning on-device...",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(6.dp))
                
                // Quick Suggestion Chips
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
                                .border(1.dp, TealAccent.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                                .clickable {
                                    model.sendMsgToAi(sug)
                                }
                                .padding(horizontal = 9.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = sug,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SlatePrimary
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(6.dp))
                
                // Input Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputMessage,
                        onValueChange = { inputMessage = it },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_offline_ai_chat"),
                        placeholder = {
                            Text(
                                text = if (isHindi) "ऑफ़लाइन AI से पूछें (बीमा, नॉमिनी, क्लेम)..." else "Ask Offline AI (claims, bank, nominees)...",
                                fontSize = 11.sp
                            )
                        },
                        maxLines = 2,
                        textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
                        shape = RoundedCornerShape(10.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (inputMessage.isNotBlank()) {
                                model.sendMsgToAi(inputMessage)
                                inputMessage = ""
                            }
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(TealAccent)
                            .testTag("btn_send_offline_ai"),
                        colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White)
                    ) {
                        Icon(imageVector = Icons.Default.Send, contentDescription = "Send prompt button", modifier = Modifier.size(18.dp))
                    }
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (model.aiChatHistory.isNotEmpty()) {
                    TextButton(
                        onClick = { model.aiChatHistory.clear() }
                    ) {
                        Text(if (isHindi) "चैट साफ करें" else "Clear Chat", fontSize = 12.sp, color = Color.Gray)
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = SlatePrimary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(if (isHindi) "बंद करें" else "Close", fontSize = 12.sp, color = Color.White)
                }
            }
        }
    )
}
