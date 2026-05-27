package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters

@Entity(tableName = "family_dependents")
data class FamilyDependent(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val fullName: String,
    val relation: String,
    val dob: String,
    val bloodGroup: String,
    val mobile: String,
    val email: String,
    val address: String,
    val dependentStatus: String, // "Dependent", "Independent", "Guardian"
    val guardianDetails: String,
    val notes: String
)

@Entity(tableName = "important_contacts")
data class ImportantContact(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val contactName: String,
    val category: String, // Spouse, Parent, Child, CA, Lawyer, Doctor, Employer HR, RM/Broker
    val phone: String,
    val email: String,
    val address: String,
    val priority: String, // "Priority 1 (Nearest)", "Priority 2", "Secondary"
    val remarks: String
)

@Entity(tableName = "vault_items")
data class VaultItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val category: String, // "BANK", "INVESTMENT", "INSURANCE", "CARD", "PROPERTY", "LOCKER", "LIABILITY", "DOCUMENT", "TAX", "EMPLOYER", "INCOME", "DEBIT", "DIGITAL"
    val title: String, // e.g. "HDFC Salary Account", "PPF SBI", "Nishant Life Cover"
    val ownerName: String, // Owner / Holder
    val institution: String, // Bank / Platform / Insurer / Employer e.g. "SBI", "LIC", "EPFO"
    val numberOrId: String, // Account number / Folio / Policy number / PAN
    val nomineeName: String, // Primary Nominee
    val nomineeRelation: String, // Relation
    val nomineeVerified: Boolean = false,
    val physicalLocation: String = "", // e.g., "Steel Almirah Binder"
    val digitalLocation: String = "", // e.g., "Drive Link"
    val status: String = "Active", // "Active", "Inactive"
    val isMasked: Boolean = true,
    val remarks: String = "",
    val detailsString: String = "", // Custom payload e.g. IFSC, UPI ID, Expiry Date, MATURITY, Premium due dates, etc.
    val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(tableName = "emergency_action_items")
data class EmergencyActionItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val phase: String, // "First 24 Hours", "First 7 Days", "First 30 Days"
    val taskName: String,
    val instructions: String,
    val completed: Boolean = false,
    val updatedBy: String = "",
    val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(tableName = "claim_records")
data class ClaimRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val itemType: String, // "Bank Balance Transfer", "Insurance Claim", "Demat Inheritance"
    val institution: String,
    val status: String, // "Not Started", "Documents Submitted", "In Review", "Completed", "Rejected"
    val assignedPerson: String,
    val pendingDocuments: String,
    val expectedTimeline: String,
    val notes: String,
    val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(tableName = "emergency_requests")
data class EmergencyAccessRequest(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userName: String,
    val relation: String,
    val requestReason: String,
    val requestTime: Long = System.currentTimeMillis(),
    val delayHours: Int = 48,
    val status: String = "Pending", // "Pending", "Approved", "Rejected", "Revoked", "Active"
    val approvedTime: Long = 0L
)

@Entity(tableName = "audit_logs")
data class AuditLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val action: String, // "Login", "Vault Item View", "Vault Item Edit", "Emergency Access Request"
    val details: String,
    val userRole: String
)
