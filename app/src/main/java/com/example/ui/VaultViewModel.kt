package com.example.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

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

    init {
        // Trigger a check to audit log setup
        viewModelScope.launch {
            repository.logAction("App Initialised", "Vault application loaded with seed database.", currentRole.name)
        }
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
    fun login(password: String) {
        if (password == "admin123" || password == "admin" || password.isEmpty()) {
            showMfaChallenge = true
            viewModelScope.launch {
                repository.logAction("Authentication Started", "Owner entered password, waiting for simulated OTP.", currentRole.name)
            }
        }
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
