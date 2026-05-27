package com.example.data

import kotlinx.coroutines.flow.Flow

class Repository(private val db: AppDatabase) {

    // 1. Family Dependents
    val familyDependents: Flow<List<FamilyDependent>> = db.familyDependentDao().getAll()
    suspend fun insertFamilyDependent(item: FamilyDependent) = db.familyDependentDao().insert(item)
    suspend fun updateFamilyDependent(item: FamilyDependent) = db.familyDependentDao().update(item)
    suspend fun deleteFamilyDependent(item: FamilyDependent) = db.familyDependentDao().delete(item)

    // 2. Important Contacts
    val importantContacts: Flow<List<ImportantContact>> = db.importantContactDao().getAll()
    suspend fun insertImportantContact(item: ImportantContact) = db.importantContactDao().insert(item)
    suspend fun updateImportantContact(item: ImportantContact) = db.importantContactDao().update(item)
    suspend fun deleteImportantContact(item: ImportantContact) = db.importantContactDao().delete(item)

    // 3. Vault Items
    val vaultItems: Flow<List<VaultItem>> = db.vaultItemDao().getAll()
    fun getVaultItemsByCategory(category: String): Flow<List<VaultItem>> = db.vaultItemDao().getByCategory(category)
    fun searchVaultItems(query: String): Flow<List<VaultItem>> = db.vaultItemDao().search(query)
    suspend fun insertVaultItem(item: VaultItem) = db.vaultItemDao().insert(item)
    suspend fun updateVaultItem(item: VaultItem) = db.vaultItemDao().update(item)
    suspend fun deleteVaultItem(item: VaultItem) = db.vaultItemDao().delete(item)

    // 4. Emergency Action Items
    val emergencyActionItems: Flow<List<EmergencyActionItem>> = db.emergencyActionItemDao().getAll()
    suspend fun insertEmergencyActionItem(item: EmergencyActionItem) = db.emergencyActionItemDao().insert(item)
    suspend fun updateEmergencyActionItem(item: EmergencyActionItem) = db.emergencyActionItemDao().update(item)
    suspend fun deleteEmergencyActionItem(item: EmergencyActionItem) = db.emergencyActionItemDao().delete(item)

    // 5. Claim Records
    val claimRecords: Flow<List<ClaimRecord>> = db.claimRecordDao().getAll()
    suspend fun insertClaimRecord(item: ClaimRecord) = db.claimRecordDao().insert(item)
    suspend fun updateClaimRecord(item: ClaimRecord) = db.claimRecordDao().update(item)
    suspend fun deleteClaimRecord(item: ClaimRecord) = db.claimRecordDao().delete(item)

    // 6. Emergency Access Requests
    val emergencyAccessRequests: Flow<List<EmergencyAccessRequest>> = db.emergencyAccessRequestDao().getAll()
    suspend fun insertEmergencyAccessRequest(item: EmergencyAccessRequest) = db.emergencyAccessRequestDao().insert(item)
    suspend fun updateEmergencyAccessRequest(item: EmergencyAccessRequest) = db.emergencyAccessRequestDao().update(item)
    suspend fun deleteEmergencyAccessRequest(item: EmergencyAccessRequest) = db.emergencyAccessRequestDao().delete(item)

    // 7. Audit Logs
    val auditLogs: Flow<List<AuditLog>> = db.auditLogDao().getAll()
    suspend fun logAction(action: String, details: String, role: String) {
        db.auditLogDao().insert(AuditLog(action = action, details = details, userRole = role))
    }
}
