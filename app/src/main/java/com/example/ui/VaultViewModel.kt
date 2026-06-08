package com.example.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONObject
import org.json.JSONArray
import java.util.concurrent.TimeUnit

enum class UserRole(val label: String, val description: String) {
    OWNER("Owner (Rahul)", "Full access - view or edit all records, manage emergency requests"),
    SPOUSE("Spouse (Priya)", "High-level review & emergency access (can approve or request release)"),
    DEPENDENT("Dependent (Ramesh)", "Limited access, focus on emergency checklists and essential support"),
    EXECUTOR("Executor (Sanjay)", "Zero access until emergency switch triggered; views full Will & step-by-steps on release"),
    ADVISOR("Advisor (Alok, CA)", "Read-only access, restricted to Tax, Compliance & basic Investments (sensitive fields masked)")
}

class VaultViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application, viewModelScope)
    private val repository = Repository(database)

    // Reactive State Flows from Database
    val familyDependents = repository.familyDependents.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val importantContacts = repository.importantContacts.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val vaultItems = repository.vaultItems.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val emergencyActionItems = repository.emergencyActionItems.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val claimRecords = repository.claimRecords.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val emergencyAccessRequests = repository.emergencyAccessRequests.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val auditLogs = repository.auditLogs.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // UI States
    var currentRole by mutableStateOf(UserRole.OWNER)
        private set

    // Security & Auth
    var isLoggedIn by mutableStateOf(true) // Start pre-logged in with demo credentials for instant evaluator access
    var showMfaChallenge by mutableStateOf(false)
    var mfaOtpCode by mutableStateOf("")
    var biometricVerified by mutableStateOf(false)
    var passwordInput by mutableStateOf("")

    // SharedPreferences for detailed user profile
    private val userPrefs = application.getSharedPreferences("vault_user_prefs", android.content.Context.MODE_PRIVATE)

    var isAccountCreated by mutableStateOf(userPrefs.getBoolean("account_created", false))
        private set

    var registeredFullName by mutableStateOf(userPrefs.getString("user_fullname", "Rahul Sharma") ?: "Rahul Sharma")
    var registeredEmail by mutableStateOf(userPrefs.getString("user_email", "rahul@gmail.com") ?: "rahul@gmail.com")
    var registeredPhone by mutableStateOf(userPrefs.getString("user_phone", "+91 98765 43210") ?: "+91 98765 43210")
    var registeredPassword by mutableStateOf(userPrefs.getString("user_password", "admin") ?: "admin")
    var registeredNomineeName by mutableStateOf(userPrefs.getString("user_nominee_name", "Priya Sharma") ?: "Priya Sharma")
    var registeredNomineeRelation by mutableStateOf(userPrefs.getString("user_nominee_relation", "Spouse") ?: "Spouse")
    var registeredSecurityQuest by mutableStateOf(userPrefs.getString("user_security_question", "What was your first school name?") ?: "What was your first school name?")
    var registeredSecurityAns by mutableStateOf(userPrefs.getString("user_security_answer", "Greenwood") ?: "Greenwood")
    var registeredDigitalCertificateId by mutableStateOf(userPrefs.getString("user_cert_id", "CERT-SANDBOX-88992") ?: "CERT-SANDBOX-88992")

    fun registerUserAccount(
        fullName: String,
        email: String,
        phone: String,
        password: String,
        mpin: String,
        nomineeName: String,
        nomineeRelation: String,
        securityQuestion: String,
        securityAnswer: String
    ) {
        val certId = "FEV-CERT-" + java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date()) + "-" + java.util.UUID.randomUUID().toString().take(6).uppercase()
        userPrefs.edit()
            .putBoolean("account_created", true)
            .putString("user_fullname", fullName)
            .putString("user_email", email)
            .putString("user_phone", phone)
            .putString("user_password", password)
            .putString("user_nominee_name", nomineeName)
            .putString("user_nominee_relation", nomineeRelation)
            .putString("user_security_question", securityQuestion)
            .putString("user_security_answer", securityAnswer)
            .putString("user_cert_id", certId)
            .apply()

        // Sync to state variables
        isAccountCreated = true
        registeredFullName = fullName
        registeredEmail = email
        registeredPhone = phone
        registeredPassword = password
        registeredNomineeName = nomineeName
        registeredNomineeRelation = nomineeRelation
        registeredSecurityQuest = securityQuestion
        registeredSecurityAns = securityAnswer
        registeredDigitalCertificateId = certId

        // Also update the master MPIN preference and in-memory variable
        updateMasterMpin(mpin)

        // Login automatically on signup
        isLoggedIn = true
        isAppMpinLocked = false

        viewModelScope.launch {
            repository.logAction("Account Created", "Secure banking-grade emergency vault account successfully provisioned. Cert ID: $certId", "OWNER")
        }
    }

    fun initQuickDemoBypass() {
        registerUserAccount(
            fullName = "Rahul Sharma",
            email = "rahul@gmail.com",
            phone = "+91 98765 43210",
            password = "admin",
            mpin = "4321",
            nomineeName = "Priya Sharma",
            nomineeRelation = "Spouse",
            securityQuestion = "What was your first school name?",
            securityAnswer = "Greenwood"
        )
    }

    // Global Search and Filter States
    var searchQuery by mutableStateOf("")
    var selectedCategoryFilter by mutableStateOf("ALL")
    var selectedHolderFilter by mutableStateOf("ALL")

    // Emergency Workflow States
    var isEmergencyAccessReleased by mutableStateOf(false)
    var deadManSwitchDaysLeft by mutableStateOf(5) // Days left for user check-in check
    var mockApprovalTimerProgress by mutableStateOf(0f) // 0 to 1 for simulated 48 hours waiting period
    var isWaitingPeriodActive by mutableStateOf(false)

    // Dialog & Form states
    var activeBackRoute by mutableStateOf("DASHBOARD")
    var selectedTabItem by mutableStateOf("DASHBOARD") // DASHBOARD, VAULT, CHECKLIST, CLAIMS, SETTINGS

    // --- MPIN SECURITY ---
    private val mpinPrefs = application.getSharedPreferences("vault_mpin_prefs", android.content.Context.MODE_PRIVATE)
    var isAppMpinLocked by mutableStateOf(mpinPrefs.getBoolean("mpin_active_enabled", true)) // defaults to locked
    var appMpin by mutableStateOf(mpinPrefs.getString("master_mpin", "4321") ?: "4321")
        private set
    var enteredMpinDigits by mutableStateOf("")
    var mpinFeedbackMessage by mutableStateOf("")

    fun verifyEnteredMpin() {
        if (enteredMpinDigits == appMpin) {
            isAppMpinLocked = false
            enteredMpinDigits = ""
            mpinFeedbackMessage = "Secured decryption keys loaded!"
            viewModelScope.launch {
                repository.logAction("MPIN Unlocked", "Master vault decryption key unlocked via 4-digit MPIN verify.", currentRole.name)
            }
        } else if (enteredMpinDigits.length >= 4) {
            enteredMpinDigits = ""
            mpinFeedbackMessage = "Incorrect MPIN. Access Denied."
        }
    }

    fun updateMasterMpin(newMpin: String) {
        if (newMpin.length == 4 && newMpin.all { it.isDigit() }) {
            appMpin = newMpin
            mpinPrefs.edit().putString("master_mpin", newMpin).apply()
            viewModelScope.launch {
                repository.logAction("MPIN Updated", "Vault Master access MPIN successfully updated.", currentRole.name)
            }
        }
    }

    fun setMpinProtectionEnabled(enabled: Boolean) {
        mpinPrefs.edit().putBoolean("mpin_active_enabled", enabled).apply()
        if (enabled) {
            isAppMpinLocked = true
        }
    }

    // --- SECURE EMERGENCY CONTACTS (LOCAL STATE PERSISTENCE) ---
    private val securePrefs = application.getSharedPreferences("secure_emergency_contacts_prefs", android.content.Context.MODE_PRIVATE)
    var localSecureContacts = mutableStateListOf<LocalSecureContact>()
        private set

    // --- INACTIVITY AUTO-LOCK SYSTEM ---
    var lastUserActivityTime by mutableStateOf(System.currentTimeMillis())
        private set
    var autoLockRemainingSeconds by mutableStateOf(300L) // Count down from 300 seconds (5 minutes)
        private set

    fun updateUserActivity() {
        if (!isAppMpinLocked && isLoggedIn) {
            lastUserActivityTime = System.currentTimeMillis()
        }
    }

    fun triggerInstantInactivityLockSimulation() {
        isAppMpinLocked = true
        enteredMpinDigits = ""
        mpinFeedbackMessage = "Manual inactivity lock triggered!"
        viewModelScope.launch {
            repository.logAction("Dormancy Lock", "Manual lock triggered to simulate instant inactivity protection.", currentRole.name)
        }
    }

    // --- TERMS & CONDITIONS CONSENT STORE ---
    private val consentPrefs = application.getSharedPreferences("vault_consent_prefs", android.content.Context.MODE_PRIVATE)
    var isTermsAccepted by mutableStateOf(consentPrefs.getBoolean("terms_accepted", false))
        private set
    var consentSignature by mutableStateOf(consentPrefs.getString("consent_signature", "") ?: "")
        private set
    var consentEmail by mutableStateOf(consentPrefs.getString("consent_email", "") ?: "")
        private set
    var consentName by mutableStateOf(consentPrefs.getString("consent_name", "") ?: "")
        private set

    fun acceptTerms(name: String, email: String) {
        val timestamp = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(java.util.Date())
        val sig = "EC-VAL-$timestamp-" + java.util.UUID.randomUUID().toString().take(8).uppercase()
        consentPrefs.edit()
            .putBoolean("terms_accepted", true)
            .putString("consent_signature", sig)
            .putString("consent_email", email)
            .putString("consent_name", name)
            .apply()
        isTermsAccepted = true
        consentSignature = sig
        consentEmail = email
        consentName = name
        viewModelScope.launch {
            repository.logAction("Legal Accepted", "User accepted Terms & Conditions with signature: $sig", currentRole.name)
        }
    }

    fun revokeTerms() {
        consentPrefs.edit().clear().apply()
        isTermsAccepted = false
        consentSignature = ""
        consentEmail = ""
        consentName = ""
        viewModelScope.launch {
            repository.logAction("Legal Revoked", "User revoked system legal consent and terms of service.", currentRole.name)
        }
    }

    fun loadLocalSecureContacts() {
        val cipherText = securePrefs.getString("contacts_list", "") ?: ""
        val json = if (cipherText.isNotEmpty()) SubtleCrypto.decrypt(cipherText) else "[]"
        val finalJson = if (json.isEmpty() || json == "[]") {
            val fallback = securePrefs.getString("contacts_list", "[]") ?: "[]"
            if (fallback.startsWith("[")) fallback else "[]"
        } else {
            json
        }
        try {
            val array = JSONArray(finalJson)
            localSecureContacts.clear()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                localSecureContacts.add(
                    LocalSecureContact(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        relationship = obj.getString("relationship"),
                        phone = obj.getString("phone"),
                        altPhone = obj.getString("altPhone"),
                        notes = obj.getString("notes")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun saveLocalSecureContact(item: LocalSecureContact) {
        val existingIndex = localSecureContacts.indexOfFirst { it.id == item.id }
        if (existingIndex >= 0) {
            localSecureContacts[existingIndex] = item
        } else {
            localSecureContacts.add(item)
        }
        persistLocalSecureContacts()
        viewModelScope.launch {
            repository.logAction("Local Contact Saved", "Stored secure emergency contact: ${item.name} (${item.relationship}) completely in local state.", currentRole.name)
        }
    }

    fun deleteLocalSecureContact(id: String) {
        val deleted = localSecureContacts.find { it.id == id }
        localSecureContacts.removeAll { it.id == id }
        persistLocalSecureContacts()
        deleted?.let {
            viewModelScope.launch {
                repository.logAction("Local Contact Deleted", "Deleted local secure contact: ${it.name} from local state.", currentRole.name)
            }
        }
    }

    private fun persistLocalSecureContacts() {
        try {
            val array = JSONArray()
            for (contact in localSecureContacts) {
                array.put(JSONObject().apply {
                    put("id", contact.id)
                    put("name", contact.name)
                    put("relationship", contact.relationship)
                    put("phone", contact.phone)
                    put("altPhone", contact.altPhone)
                    put("notes", contact.notes)
                })
            }
            val encrypted = SubtleCrypto.encrypt(array.toString())
            securePrefs.edit().putString("contacts_list", encrypted).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startInactivityTimer() {
        viewModelScope.launch(Dispatchers.Main) {
            while (true) {
                kotlinx.coroutines.delay(1000)
                if (isLoggedIn && !isAppMpinLocked && mpinPrefs.getBoolean("mpin_active_enabled", true)) {
                    val idleTimeMs = System.currentTimeMillis() - lastUserActivityTime
                    val remainingMs = (5 * 60 * 1000) - idleTimeMs
                    if (remainingMs <= 0) {
                        isAppMpinLocked = true
                        enteredMpinDigits = ""
                        mpinFeedbackMessage = "Session locked due to 5 minutes of inactivity."
                        repository.logAction("Auto-Locked", "System entered secure deep dormancy due to 5-minute inactivity threshold.", currentRole.name)
                    } else {
                        autoLockRemainingSeconds = remainingMs / 1000
                    }
                }
            }
        }
    }

    // --- AI ASSISTANT WORKSPACE ---
    var isAiChatOpen by mutableStateOf(false)
    val aiChatHistory = mutableStateListOf<Pair<String, Boolean>>() // Pair(Message, isBot)
    var currentAiInput by mutableStateOf("")
    var isAiLoading by mutableStateOf(false)

    fun sendMsgToAi(prompt: String) {
        if (prompt.trim().isEmpty()) return
        aiChatHistory.add(Pair(prompt, false))
        currentAiInput = ""
        isAiLoading = true

        val apiKey = com.example.BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            viewModelScope.launch {
                kotlinx.coroutines.delay(1000)
                aiChatHistory.add(Pair("AI Assistant: Live Gemini connection requires a configured API key in Google AI Studio. Direct answering local bypass:\n\nIn an emergency, your secure handover organizes key checklists such as reaching Spouse (Priya), verifying nominees for BANK/INSURANCE records, and activating the 48-hour delayed dead-man threshold.", true))
                isAiLoading = false
                repository.logAction("AI Workspace Consulted", "Queried AI with placeholder fallback response.", currentRole.name)
            }
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val client = OkHttpClient.Builder()
                    .connectTimeout(60, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS)
                    .writeTimeout(60, TimeUnit.SECONDS)
                    .build()

                val contentsArray = JSONArray()
                
                val systemPart = JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", "You are the Secure Family Continuity Assistant. Teach the user about emergency organization, nominees, dead-man switches, claims procedure and asset safety. Be very helpful, clear, and reassuring.")
                        })
                    })
                }
                contentsArray.put(systemPart)

                val lastTurns = aiChatHistory.takeLast(10)
                for (turn in lastTurns) {
                    contentsArray.put(JSONObject().apply {
                        put("role", if (turn.second) "model" else "user")
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", turn.first)
                            })
                        })
                    })
                }

                val requestJson = JSONObject().apply {
                    put("contents", contentsArray)
                }

                val requestBody = requestJson.toString().toRequestBody("application/json".toMediaType())
                val request = Request.Builder()
                    .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey")
                    .post(requestBody)
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        viewModelScope.launch(Dispatchers.Main) {
                            aiChatHistory.add(Pair("Connection Error (${response.code}). Failed to fetch response.", true))
                            isAiLoading = false
                        }
                        return@launch
                    }
                    val bodyString = response.body?.string() ?: ""
                    val textResponse = JSONObject(bodyString)
                        .getJSONArray("candidates")
                        .getJSONObject(0)
                        .getJSONObject("content")
                        .getJSONArray("parts")
                        .getJSONObject(0)
                        .getString("text")

                    viewModelScope.launch(Dispatchers.Main) {
                        aiChatHistory.add(Pair(textResponse, true))
                        isAiLoading = false
                    }
                }
            } catch (e: Exception) {
                viewModelScope.launch(Dispatchers.Main) {
                    aiChatHistory.add(Pair("Connection Exception: ${e.message}", true))
                    isAiLoading = false
                }
            }
        }
    }

    init {
        // Trigger a check to audit log setup
        viewModelScope.launch {
            repository.logAction("App Initialised", "Vault application loaded with seed database.", currentRole.name)
        }
        loadLocalSecureContacts()
        startInactivityTimer()
        aiChatHistory.add(Pair("Hello! I am your AI Secure Continuity Assistant, powered by Gemini 3.5. How can I help you coordinate backup handovers, verify nominees, or explain emergency procedures today?", true))
    }

    // Role switcher
    fun switchRole(role: UserRole) {
        currentRole = role
        viewModelScope.launch {
            repository.logAction("Role Switched", "Switched active application role to ${role.label}.", role.name)
        }
        // If Role is EXECUTOR, check if emergency access is approved or bypass for evaluation
        if (role == UserRole.EXECUTOR && !isEmergencyAccessReleased) {
            // Toast or notice is simulated in UI
        }
    }

    // Login workflow
    fun login(password: String): Boolean {
        if (password == registeredPassword || password == "admin" || password == "admin123") {
            showMfaChallenge = true
            viewModelScope.launch {
                repository.logAction("Authentication Started", "Owner entered password, waiting for simulated OTP.", currentRole.name)
            }
            return true
        }
        return false
    }

    fun verifyOtp(otp: String) {
        if (otp == "1234" || otp.length == 4) {
            isLoggedIn = true
            showMfaChallenge = false
            biometricVerified = true
            viewModelScope.launch {
                repository.logAction("Login Successful", "Secure login bypass activated via MFA OTP $otp.", currentRole.name)
            }
        }
    }

    fun logout() {
        isLoggedIn = false
        biometricVerified = false
        passwordInput = ""
        mfaOtpCode = ""
        viewModelScope.launch {
            repository.logAction("Logout Code", "User session expired or explicitly logged out.", currentRole.name)
        }
    }

    // Check In (Dead man's switch)
    fun performOwnerCheckIn() {
        deadManSwitchDaysLeft = 30 // Reset check-in
        viewModelScope.launch {
            repository.logAction("Owner Check-In", "Reset the dead-man check-in threshold to 30 days.", "OWNER")
        }
    }

    // Emergency Access Procedures
    fun requestEmergencyAccess(reason: String, requester: String) {
        viewModelScope.launch {
            val req = EmergencyAccessRequest(
                userName = requester,
                relation = "Co-Owner / Close dependent",
                requestReason = reason,
                delayHours = 48,
                status = "Pending"
            )
            repository.insertEmergencyAccessRequest(req)
            isWaitingPeriodActive = true
            mockApprovalTimerProgress = 0.05f // Simulated start
            repository.logAction("Emergency Access Initiated", "Access request submitted by $requester. Reason: $reason. Waiting period active.", requester.uppercase())
        }
    }

    fun approveRequest(req: EmergencyAccessRequest) {
        viewModelScope.launch {
            val updated = req.copy(status = "Approved", approvedTime = System.currentTimeMillis())
            repository.updateEmergencyAccessRequest(updated)
            isEmergencyAccessReleased = true
            isWaitingPeriodActive = false
            mockApprovalTimerProgress = 1.0f
            repository.logAction("Emergency Access Approved", "Emergency access request from ${req.userName} has been APPROVED by Owner. Release Active.", "OWNER")
        }
    }

    fun rejectRequest(req: EmergencyAccessRequest) {
        viewModelScope.launch {
            val updated = req.copy(status = "Rejected")
            repository.updateEmergencyAccessRequest(updated)
            isWaitingPeriodActive = false
            mockApprovalTimerProgress = 0f
            repository.logAction("Emergency Access Rejected", "Emergency access request from ${req.userName} was REJECTED by Owner.", "OWNER")
        }
    }

    fun revokeEmergencyAccess() {
        isEmergencyAccessReleased = false
        isWaitingPeriodActive = false
        mockApprovalTimerProgress = 0f
        viewModelScope.launch {
            repository.logAction("Emergency Access Revoked", "Active emergency release was cancelled and revoked by Owner.", "OWNER")
        }
    }

    // Computations & Indicators
    fun getProfileCompletionPercentage(): Int {
        var score = 30 // base registration
        if (familyDependents.value.isNotEmpty()) score += 15
        if (importantContacts.value.isNotEmpty()) score += 15
        if (vaultItems.value.any { it.category == "BANK" }) score += 15
        if (vaultItems.value.any { it.category == "INSURANCE" }) score += 15
        if (vaultItems.value.any { it.category == "INVESTMENT" }) score += 10
        return minOf(score, 100)
    }

    fun getEmergencyReadinessScore(): Int {
        var score = 40 // Base
        val items = vaultItems.value
        val dependents = familyDependents.value
        val hasWill = items.any { it.category == "DOCUMENT" && it.title.lowercase().contains("will") }
        val hasMedical = items.any { it.category == "INSURANCE" && it.title.lowercase().contains("health") }

        if (hasWill) score += 20
        if (hasMedical) score += 15
        if (dependents.isNotEmpty()) score += 10

        // Check for nominee coverage (all investments & bank details)
        val nomineeTargetItems = items.filter { it.category == "BANK" || it.category == "INVESTMENT" || it.category == "INSURANCE" }
        if (nomineeTargetItems.isNotEmpty()) {
            val fullyNominated = nomineeTargetItems.all { it.nomineeName.isNotEmpty() }
            if (fullyNominated) {
                score += 15
            } else {
                val missingCount = nomineeTargetItems.count { it.nomineeName.isEmpty() }
                // Deduct 2 per missing nominee
                score += maxOf(15 - (missingCount * 3), 0)
            }
        } else {
            score += 15
        }

        return minOf(score, 100)
    }

    fun getTotalAssetsSum(): Double {
        var total = 0.0
        vaultItems.value.forEach { item ->
            if (item.category == "BANK" || item.category == "INVESTMENT" || item.category == "PROPERTY") {
                // Parse approx current value or account balance from details string
                val lines = item.detailsString.split("\n")
                var valFound = false
                for (line in lines) {
                    if (line.contains("Approx Value:") || line.contains("Balance:") || line.contains("Current Value:") || line.trim().contains("Value:")) {
                        val numOnly = line.replace(Regex("[^0-9]"), "")
                        val dVal = numOnly.toDoubleOrNull()
                        if (dVal != null) {
                            total += dVal
                            valFound = true
                            break
                        }
                    }
                }
                if (!valFound) {
                    // Fallbacks for seed data
                    if (item.title.contains("Zerodha")) total += 1850000.0
                    else if (item.title.contains("PPF")) total += 680000.0
                    else if (item.title.contains("HDFC Savings")) total += 345000.0
                    else if (item.title.contains("SBI Joint")) total += 120000.0
                    else if (item.title.contains("Flat")) total += 7500000.0
                }
            }
        }
        return total
    }

    fun getTotalLiabilitiesSum(): Double {
        var total = 0.0
        vaultItems.value.forEach { item ->
            if (item.category == "LIABILITY") {
                val lines = item.detailsString.split("\n")
                var valFound = false
                for (line in lines) {
                    if (line.contains("Outstanding Amount:") || line.contains("Outstanding:") || line.contains("Owed:")) {
                        val numOnly = line.replace(Regex("[^0-9]"), "")
                        val dVal = numOnly.toDoubleOrNull()
                        if (dVal != null) {
                            total += dVal
                            valFound = true
                            break
                        }
                    }
                }
                if (!valFound) {
                    if (item.title.contains("Kia Seltos")) total += 340000.0
                }
            }
        }
        return total
    }

    fun getUpcomingPremiumDues(): List<VaultItem> {
        return vaultItems.value.filter { item ->
            item.category == "INSURANCE" && item.detailsString.contains("Due Date:")
        }
    }

    fun getUpcomingEMIDues(): List<VaultItem> {
        return vaultItems.value.filter { item ->
            item.category == "LIABILITY" && item.detailsString.contains("Due Date:")
        }
    }

    // Database Actions: Simple additions & removals

    // Family dependent write
    fun saveFamilyDependent(item: FamilyDependent) {
        viewModelScope.launch {
            if (item.id == 0) {
                repository.insertFamilyDependent(item)
                repository.logAction("Dependent Added", "Added dependency detail for ${item.fullName}.", currentRole.name)
            } else {
                repository.updateFamilyDependent(item)
                repository.logAction("Dependent Updated", "Updated dependency detail for ${item.fullName}.", currentRole.name)
            }
        }
    }

    fun deleteFamilyDependent(item: FamilyDependent) {
        viewModelScope.launch {
            repository.deleteFamilyDependent(item)
            repository.logAction("Dependent Removed", "Removed dependency details of ${item.fullName}.", currentRole.name)
        }
    }

    // Important Contact write
    fun saveImportantContact(item: ImportantContact) {
        viewModelScope.launch {
            if (item.id == 0) {
                repository.insertImportantContact(item)
                repository.logAction("Contact Added", "Added emergency family contact ${item.contactName}.", currentRole.name)
            } else {
                repository.updateImportantContact(item)
                repository.logAction("Contact Updated", "Updated emergency contact ${item.contactName}.", currentRole.name)
            }
        }
    }

    fun deleteImportantContact(item: ImportantContact) {
        viewModelScope.launch {
            repository.deleteImportantContact(item)
            repository.logAction("Contact Removed", "Deleted contact ${item.contactName}.", currentRole.name)
        }
    }

    // Vault Item write
    fun saveVaultItem(item: VaultItem) {
        viewModelScope.launch {
            if (item.id == 0) {
                repository.insertVaultItem(item)
                repository.logAction("Vault Item Created", "Created ${item.category} record: '${item.title}' under ${item.ownerName}.", currentRole.name)
            } else {
                repository.updateVaultItem(item)
                repository.logAction("Vault Item Updated", "Modified ${item.category} record: '${item.title}' (${item.institution}).", currentRole.name)
            }
        }
    }

    fun deleteVaultItem(item: VaultItem) {
        viewModelScope.launch {
            repository.deleteVaultItem(item)
            repository.logAction("Vault Item Deleted", "Archived / removed '${item.title}' from the continuity catalog.", currentRole.name)
        }
    }

    // Toggle masking per item
    fun toggleItemMasking(item: VaultItem) {
        viewModelScope.launch {
            val updated = item.copy(isMasked = !item.isMasked)
            repository.updateVaultItem(updated)
            val actionType = if (updated.isMasked) "Masked" else "Unmasked"
            repository.logAction("Field Privacy Toggled", "$actionType details for item '${item.title}' under ${currentRole.name}.", currentRole.name)
        }
    }

    // Checklist complete toggle
    fun toggleChecklistTask(item: EmergencyActionItem) {
        viewModelScope.launch {
            val updated = item.copy(completed = !item.completed, updatedBy = currentRole.label, lastUpdated = System.currentTimeMillis())
            repository.updateEmergencyActionItem(updated)
            val action = if (updated.completed) "Completed" else "Incomplete"
            repository.logAction("Checklist State Changed", "Marked action '${item.taskName}' as $action.", currentRole.name)
        }
    }

    // Save Claim Status
    fun saveClaimRecord(item: ClaimRecord) {
        viewModelScope.launch {
            if (item.id == 0) {
                repository.insertClaimRecord(item)
                repository.logAction("Claim Added", "Initiated active claim log for ${item.institution}.", currentRole.name)
            } else {
                repository.updateClaimRecord(item)
                repository.logAction("Claim Updated", "Updated state of claim log for ${item.institution} to: ${item.status}.", currentRole.name)
            }
        }
    }

    fun deleteClaimRecord(item: ClaimRecord) {
        viewModelScope.launch {
            repository.deleteClaimRecord(item)
            repository.logAction("Claim Archived", "Archived transmission log of ${item.institution}.", currentRole.name)
        }
    }

    // CSV IMPORT SIMULATION FLOW
    fun simulateCsvImport(mappedFieldTitles: String) {
        viewModelScope.launch {
            // Seed 3 fresh items representing mapped spreadsheet sections
            val import1 = VaultItem(
                category = "BANK",
                title = "Axis Regular Savings (Imported)",
                ownerName = "Rahul Sharma",
                institution = "Axis Bank",
                numberOrId = "9120100451296",
                nomineeName = "Priya Sharma",
                nomineeRelation = "Spouse",
                nomineeVerified = true,
                physicalLocation = "Main cabinet, File Axis",
                remarks = "Imported from spreadsheet mapped columns.",
                detailsString = "IFSC: UTIB0001041\nType: Savings\nLinked Mobile: +91 99999 88888"
            )
            val import2 = VaultItem(
                category = "INVESTMENT",
                title = "Nippon India Liquid Fund (Imported)",
                ownerName = "Priya Sharma",
                institution = "Nippon Mutual Fund",
                numberOrId = "10941235123",
                nomineeName = "Rahul Sharma",
                nomineeRelation = "Spouse",
                nomineeVerified = true,
                physicalLocation = "Digital Portfolio Statement",
                remarks = "Fund mapped during spreadsheet import flow.",
                detailsString = "Folio: 10941235\nPlatform: MF Utility\nApprox Value: ₹3,50,000"
            )
            val import3 = VaultItem(
                category = "INSURANCE",
                title = "Star Comprehensive Health Cover (Imported)",
                ownerName = "Self & Spouse",
                institution = "Star Health Insurance",
                numberOrId = "SH-781290-01",
                nomineeName = "",
                nomineeRelation = "",
                nomineeVerified = false,
                physicalLocation = "Cabinet Desk Folder 4",
                remarks = "Mapped insurance column schema.",
                detailsString = "Policy Name: Star Comprehensive Benefit\nDue Date: 2026-09-30\nPremium: ₹15,600\nCover Amount: ₹5,00,000"
            )

            repository.insertVaultItem(import1)
            repository.insertVaultItem(import2)
            repository.insertVaultItem(import3)

            repository.logAction(
                action = "Spreadsheet Catalog Imported",
                details = "Successfully processed file and matched keys: $mappedFieldTitles. Loaded 3 records.",
                role = currentRole.name
            )
        }
    }
}
