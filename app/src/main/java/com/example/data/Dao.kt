package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FamilyDependentDao {
    @Query("SELECT * FROM family_dependents ORDER BY fullName ASC")
    fun getAll(): Flow<List<FamilyDependent>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: FamilyDependent)

    @Update
    suspend fun update(item: FamilyDependent)

    @Delete
    suspend fun delete(item: FamilyDependent)
}

@Dao
interface ImportantContactDao {
    @Query("SELECT * FROM important_contacts ORDER BY priority ASC, contactName ASC")
    fun getAll(): Flow<List<ImportantContact>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ImportantContact)

    @Update
    suspend fun update(item: ImportantContact)

    @Delete
    suspend fun delete(item: ImportantContact)
}

@Dao
interface VaultItemDao {
    @Query("SELECT * FROM vault_items ORDER BY category ASC, title ASC")
    fun getAll(): Flow<List<VaultItem>>

    @Query("SELECT * FROM vault_items WHERE category = :category ORDER BY title ASC")
    fun getByCategory(category: String): Flow<List<VaultItem>>

    @Query("SELECT * FROM vault_items WHERE title LIKE '%' || :query || '%' OR institution LIKE '%' || :query || '%' OR nomineeName LIKE '%' || :query || '%' OR numberOrId LIKE '%' || :query || '%'")
    fun search(query: String): Flow<List<VaultItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: VaultItem)

    @Update
    suspend fun update(item: VaultItem)

    @Delete
    suspend fun delete(item: VaultItem)
}

@Dao
interface EmergencyActionItemDao {
    @Query("SELECT * FROM emergency_action_items ORDER BY phase ASC, id ASC")
    fun getAll(): Flow<List<EmergencyActionItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: EmergencyActionItem)

    @Update
    suspend fun update(item: EmergencyActionItem)

    @Delete
    suspend fun delete(item: EmergencyActionItem)
}

@Dao
interface ClaimRecordDao {
    @Query("SELECT * FROM claim_records ORDER BY status ASC, lastUpdated DESC")
    fun getAll(): Flow<List<ClaimRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ClaimRecord)

    @Update
    suspend fun update(item: ClaimRecord)

    @Delete
    suspend fun delete(item: ClaimRecord)
}

@Dao
interface EmergencyAccessRequestDao {
    @Query("SELECT * FROM emergency_requests ORDER BY requestTime DESC")
    fun getAll(): Flow<List<EmergencyAccessRequest>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: EmergencyAccessRequest)

    @Update
    suspend fun update(item: EmergencyAccessRequest)

    @Delete
    suspend fun delete(item: EmergencyAccessRequest)
}

@Dao
interface AuditLogDao {
    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC LIMIT 100")
    fun getAll(): Flow<List<AuditLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: AuditLog)
}
