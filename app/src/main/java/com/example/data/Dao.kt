package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FamilyDependentDao {
    @Query("SELECT * FROM family_dependents ORDER BY fullName ASC")
    fun getAll(): Flow<List<FamilyDependent>>

    @Query("SELECT COUNT(*) FROM family_dependents")
    suspend fun getCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: FamilyDependent)

    @Update
    suspend fun update(item: FamilyDependent)

    @Delete
    suspend fun delete(item: FamilyDependent)

    @Query("DELETE FROM family_dependents")
    suspend fun deleteAll()
}

@Dao
interface ImportantContactDao {
    @Query("SELECT * FROM important_contacts ORDER BY priority ASC, contactName ASC")
    fun getAll(): Flow<List<ImportantContact>>

    @Query("SELECT COUNT(*) FROM important_contacts")
    suspend fun getCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ImportantContact)

    @Update
    suspend fun update(item: ImportantContact)

    @Delete
    suspend fun delete(item: ImportantContact)

    @Query("DELETE FROM important_contacts")
    suspend fun deleteAll()
}

@Dao
interface VaultItemDao {
    @Query("SELECT * FROM vault_items ORDER BY category ASC, title ASC")
    fun getAll(): Flow<List<VaultItem>>

    @Query("SELECT COUNT(*) FROM vault_items")
    suspend fun getCount(): Int

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

    @Query("DELETE FROM vault_items")
    suspend fun deleteAll()
}

@Dao
interface EmergencyActionItemDao {
    @Query("SELECT * FROM emergency_action_items ORDER BY phase ASC, id ASC")
    fun getAll(): Flow<List<EmergencyActionItem>>

    @Query("SELECT COUNT(*) FROM emergency_action_items")
    suspend fun getCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: EmergencyActionItem)

    @Update
    suspend fun update(item: EmergencyActionItem)

    @Delete
    suspend fun delete(item: EmergencyActionItem)

    @Query("DELETE FROM emergency_action_items")
    suspend fun deleteAll()
}

@Dao
interface ClaimRecordDao {
    @Query("SELECT * FROM claim_records ORDER BY status ASC, lastUpdated DESC")
    fun getAll(): Flow<List<ClaimRecord>>

    @Query("SELECT COUNT(*) FROM claim_records")
    suspend fun getCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ClaimRecord)

    @Update
    suspend fun update(item: ClaimRecord)

    @Delete
    suspend fun delete(item: ClaimRecord)

    @Query("DELETE FROM claim_records")
    suspend fun deleteAll()
}

@Dao
interface EmergencyAccessRequestDao {
    @Query("SELECT * FROM emergency_requests ORDER BY requestTime DESC")
    fun getAll(): Flow<List<EmergencyAccessRequest>>

    @Query("SELECT COUNT(*) FROM emergency_requests")
    suspend fun getCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: EmergencyAccessRequest)

    @Update
    suspend fun update(item: EmergencyAccessRequest)

    @Delete
    suspend fun delete(item: EmergencyAccessRequest)

    @Query("DELETE FROM emergency_requests")
    suspend fun deleteAll()
}

@Dao
interface AuditLogDao {
    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC LIMIT 100")
    fun getAll(): Flow<List<AuditLog>>

    @Query("SELECT COUNT(*) FROM audit_logs")
    suspend fun getCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: AuditLog)

    @Query("DELETE FROM audit_logs")
    suspend fun deleteAll()
}
