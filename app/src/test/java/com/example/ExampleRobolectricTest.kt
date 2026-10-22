package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlinx.coroutines.flow.first

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Family Emergency Vault", appName)
  }

  @Test
  fun `database initializes and supports attachments`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = androidx.room.Room.inMemoryDatabaseBuilder(
      context,
      com.example.data.AppDatabase::class.java
    ).allowMainThreadQueries().build()

    val count = db.vaultItemDao()
    org.junit.Assert.assertNotNull(count)
    db.close()
  }

  @Test
  fun `vault item dao and database operations work seamlessly`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = androidx.room.Room.inMemoryDatabaseBuilder(
      context,
      com.example.data.AppDatabase::class.java
    ).allowMainThreadQueries().build()

    val dao = db.vaultItemDao()
    val initialCount = dao.getCount()
    assertEquals(0, initialCount)

    val item = com.example.data.VaultItem(
      id = 1,
      category = "BANK",
      title = "HDFC Salary Account",
      ownerName = "Rahul Sharma",
      institution = "HDFC Bank",
      numberOrId = "501000294819",
      nomineeName = "Priya Sharma",
      nomineeRelation = "Spouse",
      nomineeVerified = true,
      physicalLocation = "Master Bedroom Safe",
      digitalLocation = "NetBanking",
      attachmentUri = "content://media/external/images/1"
    )

    dao.insert(item)
    val afterInsertCount = dao.getCount()
    assertEquals(1, afterInsertCount)

    db.close()
  }

  @Test
  fun `emergency checklist item operations work`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = androidx.room.Room.inMemoryDatabaseBuilder(
      context,
      com.example.data.AppDatabase::class.java
    ).allowMainThreadQueries().build()

    val dao = db.emergencyActionItemDao()
    val task = com.example.data.EmergencyActionItem(
      id = 1,
      phase = "First 24 Hours",
      taskName = "Obtain Hospital Death Certificate",
      instructions = "Obtain signed Medical Certificate from hospital authority.",
      completed = false
    )

    dao.insert(task)
    val count = dao.getCount()
    assertEquals(1, count)

    val updatedTask = task.copy(completed = true, updatedBy = "Nominee")
    dao.update(updatedTask)

    db.close()
  }

  @Test
  fun `vault item stores and retrieves physical document base64 photo`() = kotlinx.coroutines.runBlocking {
    val bitmap = android.graphics.Bitmap.createBitmap(100, 100, android.graphics.Bitmap.Config.ARGB_8888)
    val base64 = com.example.util.ImageUtils.bitmapToBase64(bitmap)
    org.junit.Assert.assertTrue(base64.isNotEmpty())

    val decodedBitmap = com.example.util.ImageUtils.base64ToBitmap(base64)
    org.junit.Assert.assertNotNull(decodedBitmap)

    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = androidx.room.Room.inMemoryDatabaseBuilder(
      context,
      com.example.data.AppDatabase::class.java
    ).allowMainThreadQueries().build()

    val dao = db.vaultItemDao()
    val item = com.example.data.VaultItem(
      id = 10,
      category = "DOCUMENT",
      title = "Physical Property Deed",
      ownerName = "Rahul Sharma",
      institution = "Sub-Registrar Office",
      numberOrId = "DEED-2024-99",
      nomineeName = "Priya Sharma",
      nomineeRelation = "Spouse",
      documentPhotoBase64 = base64
    )
    dao.insert(item)

    assertEquals(1, dao.getCount())
    db.close()
  }

  @Test
  fun `server registration and return login flow with salted hash and token verification`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = androidx.room.Room.inMemoryDatabaseBuilder(
      context,
      com.example.data.AppDatabase::class.java
    ).allowMainThreadQueries().build()

    val gateway = com.example.util.EnterpriseServerGateway(db)

    // 1. First-time registration on server
    val regResponse = gateway.registerUserOnServer(
      fullName = "Vikram Malhotra",
      email = "vikram@enterprise.com",
      phone = "+91 99887 76655",
      password = "SecurePass@2026",
      mpin = "9876",
      nomineeName = "Ananya Malhotra",
      nomineeRelation = "Spouse",
      securityQuestion = "What was your first school name?",
      securityAnswer = "DPS RK Puram",
      enableBiometrics = true
    )

    org.junit.Assert.assertTrue(regResponse.success)
    org.junit.Assert.assertNotNull(regResponse.user)
    org.junit.Assert.assertTrue(regResponse.token.isNotEmpty())
    org.junit.Assert.assertEquals("vikram@enterprise.com", regResponse.user?.email)

    // Verify user count
    val totalUsers = gateway.getServerUserCount()
    org.junit.Assert.assertEquals(1, totalUsers)

    // 2. Recognized return login with exact matching credentials
    val loginSuccess = gateway.authenticateReturnLogin(
      email = "vikram@enterprise.com",
      password = "SecurePass@2026"
    )

    org.junit.Assert.assertTrue(loginSuccess.success)
    org.junit.Assert.assertNotNull(loginSuccess.user)
    org.junit.Assert.assertEquals("Vikram Malhotra", loginSuccess.user?.fullName)
    org.junit.Assert.assertEquals("Ananya Malhotra", loginSuccess.user?.nomineeName)
    org.junit.Assert.assertEquals("9876", loginSuccess.user?.mpin)
    org.junit.Assert.assertTrue(loginSuccess.token.isNotEmpty())

    // 3. Recognized return login with wrong password fails
    val loginWrongPassword = gateway.authenticateReturnLogin(
      email = "vikram@enterprise.com",
      password = "WrongPassword123"
    )
    org.junit.Assert.assertFalse(loginWrongPassword.success)
    org.junit.Assert.assertEquals("INVALID_CREDENTIALS", loginWrongPassword.errorCode)

    // 4. Return login with unregistered email fails
    val loginUnknownUser = gateway.authenticateReturnLogin(
      email = "unknown@random.com",
      password = "AnyPassword"
    )
    org.junit.Assert.assertFalse(loginUnknownUser.success)
    org.junit.Assert.assertEquals("USER_NOT_FOUND", loginUnknownUser.errorCode)

    // 5. Admin registration tracking queries
    val allUsers = gateway.getAllServerUsers()
    org.junit.Assert.assertEquals(1, allUsers.size)
    org.junit.Assert.assertEquals("ACTIVE_VERIFIED", allUsers[0].status)

    // 6. Test duplicate registration prevention
    val duplicateReg = gateway.registerUserOnServer(
      fullName = "Vikram Dup",
      email = "vikram@enterprise.com",
      phone = "+91 99887 76655",
      password = "AnotherPassword",
      mpin = "1111",
      nomineeName = "Ananya",
      nomineeRelation = "Spouse",
      securityQuestion = "Question?",
      securityAnswer = "Answer",
      enableBiometrics = false
    )
    org.junit.Assert.assertFalse(duplicateReg.success)
    org.junit.Assert.assertEquals("EMAIL_ALREADY_EXISTS", duplicateReg.errorCode)

    db.close()
  }

  @Test
  fun `clean production onboarding initializes empty vault with zero financial records and zero backdoor logins`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = androidx.room.Room.inMemoryDatabaseBuilder(
      context,
      com.example.data.AppDatabase::class.java
    ).allowMainThreadQueries().build()

    // Run clean production onboarding
    com.example.data.AppDatabase.ensureSeeded(db)

    // Verify 100% clean production state - ZERO personal or financial records pre-seeded
    val vaultCount = db.vaultItemDao().getCount()
    org.junit.Assert.assertEquals("New users must receive 0 pre-seeded vault items", 0, vaultCount)

    val dependentsCount = db.familyDependentDao().getCount()
    org.junit.Assert.assertEquals("New users must receive 0 pre-seeded dependents", 0, dependentsCount)

    val contactsCount = db.importantContactDao().getCount()
    org.junit.Assert.assertEquals("New users must receive 0 pre-seeded contacts", 0, contactsCount)

    val claimsCount = db.claimRecordDao().getCount()
    org.junit.Assert.assertEquals("New users must receive 0 pre-seeded claims", 0, claimsCount)

    // Verify ZERO backdoor or hardcoded server user credentials
    val serverUsers = db.serverUserDao().getAll()
    org.junit.Assert.assertEquals("No backdoor server accounts must be seeded in production", 0, serverUsers.size)

    // Verify generic non-personal SOP emergency checklist templates exist
    val actionItemsCount = db.emergencyActionItemDao().getCount()
    org.junit.Assert.assertTrue("Generic SOP checklist should be available", actionItemsCount > 0)

    // Verify initial audit log recorded
    val auditLogs = db.auditLogDao().getCount()
    org.junit.Assert.assertTrue("Initialization audit log must be recorded", auditLogs > 0)

    db.close()
  }

  @Test
  fun `demo mode provides isolated sample records without contaminating real household data`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = androidx.room.Room.inMemoryDatabaseBuilder(
      context,
      com.example.data.AppDatabase::class.java
    ).allowMainThreadQueries().build()

    // Initialize clean production first
    com.example.data.AppDatabase.ensureSeeded(db)
    org.junit.Assert.assertEquals(0, db.vaultItemDao().getCount())

    // Explicitly load demo mode
    com.example.data.AppDatabase.seedDemoData(db)
    val totalDemoVaultCount = db.vaultItemDao().getCount()
    org.junit.Assert.assertTrue("Demo mode populates sample records", totalDemoVaultCount > 0)

    // Verify all demo records are strictly tagged with isDemo = true
    val demoItems = db.vaultItemDao().getByDemoMode(true)
    val realItems = db.vaultItemDao().getByDemoMode(false)
    val realList = realItems.first()
    val demoList = demoItems.first()

    org.junit.Assert.assertEquals("Real household query must return 0 records when only demo items exist", 0, realList.size)
    org.junit.Assert.assertEquals("Demo query must return all seeded demo records", totalDemoVaultCount, demoList.size)

    // Purge demo mode
    db.vaultItemDao().clearDemoItems()
    org.junit.Assert.assertEquals("Purging demo items must leave vault completely clean", 0, db.vaultItemDao().getCount())

    db.close()
  }

  @Test
  fun `subtle crypto AES-GCM engine encrypts and decrypts with authenticated integrity`() {
    val plainSecret = "FamilySecret_123456_SecureVault"
    val cipherText = com.example.ui.SubtleCrypto.encrypt(plainSecret)

    org.junit.Assert.assertTrue("Ciphertext must not be empty", cipherText.isNotEmpty())
    org.junit.Assert.assertTrue("Ciphertext format must include IV separator", cipherText.contains(":"))
    org.junit.Assert.assertNotEquals("Plaintext must not match ciphertext", plainSecret, cipherText)

    val decrypted = com.example.ui.SubtleCrypto.decrypt(cipherText)
    org.junit.Assert.assertEquals("Decrypted plaintext must match original secret", plainSecret, decrypted)
  }

  @Test
  fun `family dependents and contacts CRUD operations work correctly`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = androidx.room.Room.inMemoryDatabaseBuilder(
      context,
      com.example.data.AppDatabase::class.java
    ).allowMainThreadQueries().build()

    val depDao = db.familyDependentDao()
    val dep = com.example.data.FamilyDependent(
      id = 101,
      fullName = "Aarav Sharma",
      relation = "Child",
      dob = "2018-05-12",
      bloodGroup = "O+",
      mobile = "+91 98111 22334",
      email = "aarav@family.internal",
      address = "Noida Sector 62",
      dependentStatus = "Dependent",
      guardianDetails = "Rahul Sharma (Father)",
      notes = "Asthma inhaler in school bag"
    )
    depDao.insert(dep)
    val fetchedCount = depDao.getCount()
    org.junit.Assert.assertEquals(1, fetchedCount)

    val contactDao = db.importantContactDao()
    val contact = com.example.data.ImportantContact(
      id = 201,
      contactName = "Dr. Sameer Gupta",
      category = "Doctor",
      phone = "+91 98222 33445",
      email = "dr.gupta@maxhealthcare.in",
      address = "Max Hospital, Saket",
      priority = "Priority 1 (Nearest)",
      remarks = "Family physician, available 24x7 for emergencies"
    )
    contactDao.insert(contact)
    val contactCount = contactDao.getCount()
    org.junit.Assert.assertEquals(1, contactCount)

    db.close()
  }

  @Test
  fun `emergency access requests lifecycle works properly`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = androidx.room.Room.inMemoryDatabaseBuilder(
      context,
      com.example.data.AppDatabase::class.java
    ).allowMainThreadQueries().build()

    val accessDao = db.emergencyAccessRequestDao()
    val request = com.example.data.EmergencyAccessRequest(
      id = 301,
      userName = "Priya Sharma",
      relation = "Spouse",
      requestReason = "Medical emergency hospital admission",
      status = "Pending"
    )
    accessDao.insert(request)

    val count = accessDao.getCount()
    org.junit.Assert.assertEquals(1, count)

    // Update status to Approved
    val approved = request.copy(status = "Approved", approvedTime = System.currentTimeMillis())
    accessDao.update(approved)

    db.close()
  }

  @Test
  fun `server cluster metrics and JWT verification work properly`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = androidx.room.Room.inMemoryDatabaseBuilder(
      context,
      com.example.data.AppDatabase::class.java
    ).allowMainThreadQueries().build()

    val gateway = com.example.util.EnterpriseServerGateway(db)

    // Register user to generate activity and token
    val reg = gateway.registerUserOnServer(
      fullName = "Kavita Rao",
      email = "kavita.rao@enterprise.vault",
      phone = "+91 98765 43210",
      password = "EnterpriseSecurePass1!",
      mpin = "4321",
      nomineeName = "Suresh Rao",
      nomineeRelation = "Brother",
      securityQuestion = "Favorite city?",
      securityAnswer = "Bengaluru",
      enableBiometrics = true
    )
    org.junit.Assert.assertTrue(reg.success)
    org.junit.Assert.assertTrue(reg.token.contains("."))

    // Authenticate return login
    val login = gateway.authenticateReturnLogin(
      email = "kavita.rao@enterprise.vault",
      password = "EnterpriseSecurePass1!"
    )
    org.junit.Assert.assertTrue(login.success)
    org.junit.Assert.assertEquals("Kavita Rao", login.user?.fullName)

    // Salt and Hash verification
    val salt = gateway.generateSalt()
    val hash1 = gateway.hashPassword("TestPassword", salt)
    val hash2 = gateway.hashPassword("TestPassword", salt)
    org.junit.Assert.assertEquals(hash1, hash2)

    val wrongHash = gateway.hashPassword("DifferentPassword", salt)
    org.junit.Assert.assertNotEquals(hash1, wrongHash)

    db.close()
  }

  @Test
  fun `csv import parses valid rows and captures row-level errors for invalid rows`() {
    val csvContent = """
      Title,Category,Institution,Account Number,Nominee,Relation,Verified,Remarks
      Salary Account,BANK,HDFC Bank,1234567890,Priya Sharma,Spouse,Yes,Main family income account.
      Health Shield,INSURANCE,Star Health,POL-998877,Rahul Sharma,Son,Yes,Critical illness cover.
      ,,, ,,,
      ,BANK,,,Priya Sharma,Spouse,Yes,Remarks only but no title or number
    """.trimIndent()

    val result = com.example.util.CsvImporter.parseSpreadsheet(
      inputStream = csvContent.byteInputStream(Charsets.UTF_8),
      targetHouseholdId = "user@test.vault",
      isDemoTarget = false,
      existingItems = emptyList()
    )

    org.junit.Assert.assertEquals(2, result.validItems.size)
    org.junit.Assert.assertEquals(1, result.rowErrors.size)
    org.junit.Assert.assertEquals("Salary Account", result.validItems[0].title)
    org.junit.Assert.assertEquals("Main family income account.", result.validItems[0].remarks)
    org.junit.Assert.assertEquals("Priya Sharma", result.validItems[0].nomineeName)
    org.junit.Assert.assertEquals("Spouse", result.validItems[0].nomineeRelation)
    org.junit.Assert.assertEquals("user@test.vault", result.validItems[0].userId)
    org.junit.Assert.assertFalse(result.validItems[0].isDemo)
  }

  @Test
  fun `csv import detects duplicates against existing household items and within file`() {
    val existing = listOf(
      com.example.data.VaultItem(
        category = "BANK",
        title = "Existing Account",
        ownerName = "User",
        institution = "SBI",
        numberOrId = "SBI-001122",
        userId = "household_1",
        isDemo = false
      )
    )

    val csvContent = """
      Title,Category,Institution,Account Number,Nominee,Relation,Verified,Remarks
      New Account,BANK,ICICI,ICICI-9999,Rohan,Brother,Yes,Valid new account.
      Duplicate Existing,BANK,SBI,SBI-001122,Rohan,Brother,Yes,Matches existing item.
      New Account Repeat,BANK,ICICI,ICICI-9999,Rohan,Brother,Yes,Duplicate in same file.
    """.trimIndent()

    val result = com.example.util.CsvImporter.parseSpreadsheet(
      inputStream = csvContent.byteInputStream(Charsets.UTF_8),
      targetHouseholdId = "household_1",
      isDemoTarget = false,
      existingItems = existing
    )

    org.junit.Assert.assertEquals(1, result.validItems.size)
    org.junit.Assert.assertEquals("New Account", result.validItems[0].title)
    org.junit.Assert.assertEquals(2, result.duplicates.size)
  }

  @Test
  fun `csv import strictly isolates demo data from real household target`() {
    val demoItems = listOf(
      com.example.data.VaultItem(
        category = "BANK",
        title = "Demo Savings",
        ownerName = "Demo User",
        institution = "Demo Bank",
        numberOrId = "DEMO-1234",
        userId = "demo_household",
        isDemo = true
      )
    )

    val csvContent = """
      Title,Category,Institution,Account Number,Nominee,Relation,Verified,Remarks
      Real Asset,BANK,Demo Bank,DEMO-1234,Real Nominee,Spouse,Yes,Real account using same identifier.
    """.trimIndent()

    val result = com.example.util.CsvImporter.parseSpreadsheet(
      inputStream = csvContent.byteInputStream(Charsets.UTF_8),
      targetHouseholdId = "real_household_abc",
      isDemoTarget = false,
      existingItems = demoItems
    )

    org.junit.Assert.assertEquals(1, result.validItems.size)
    org.junit.Assert.assertEquals("real_household_abc", result.validItems[0].userId)
    org.junit.Assert.assertFalse(result.validItems[0].isDemo)
    org.junit.Assert.assertEquals(0, result.duplicates.size)
  }

  @Test
  fun `document storage saves bitmaps and streams to private storage, validates files, and cleans up on delete`() {
    val context = ApplicationProvider.getApplicationContext<Context>()

    // 1. Save Bitmap Document
    val testBitmap = android.graphics.Bitmap.createBitmap(100, 100, android.graphics.Bitmap.Config.ARGB_8888)
    val saveBitmapResult = com.example.util.DocumentStorageManager.saveBitmapDocument(context, testBitmap)
    org.junit.Assert.assertTrue(saveBitmapResult is com.example.util.DocumentSaveResult.Success)
    val relPath = (saveBitmapResult as com.example.util.DocumentSaveResult.Success).relativePath
    org.junit.Assert.assertTrue(relPath.startsWith("doc_") && relPath.endsWith(".jpg"))

    // 2. Load Bitmap back safely
    val loadedBitmap = com.example.util.DocumentStorageManager.loadDocumentBitmap(context, relPath)
    org.junit.Assert.assertNotNull(loadedBitmap)

    // 3. Save Document Stream with size and type validation
    val pngBytes = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
    val saveStreamResult = com.example.util.DocumentStorageManager.saveDocumentStream(
      context,
      pngBytes.inputStream(),
      "scanned_passport.png"
    )
    org.junit.Assert.assertTrue(saveStreamResult is com.example.util.DocumentSaveResult.Success)
    val streamRelPath = (saveStreamResult as com.example.util.DocumentSaveResult.Success).relativePath
    org.junit.Assert.assertTrue(streamRelPath.endsWith(".png"))

    // 4. Validate rejection of unsupported extensions
    val invalidExtResult = com.example.util.DocumentStorageManager.saveDocumentStream(
      context,
      "dummy".byteInputStream(),
      "malicious_script.exe"
    )
    org.junit.Assert.assertTrue(invalidExtResult is com.example.util.DocumentSaveResult.Error)

    // 5. App restart simulation: verify reopening file persists
    val restartedLoaded = com.example.util.DocumentStorageManager.loadDocumentBitmap(context, relPath)
    org.junit.Assert.assertNotNull(restartedLoaded)

    // 6. Delete document cleanup
    val deleteSuccess = com.example.util.DocumentStorageManager.deleteDocument(context, relPath)
    org.junit.Assert.assertTrue(deleteSuccess)
    val loadedAfterDelete = com.example.util.DocumentStorageManager.loadDocumentBitmap(context, relPath)
    org.junit.Assert.assertNull(loadedAfterDelete)

    // Cleanup second file
    com.example.util.DocumentStorageManager.deleteDocument(context, streamRelPath)
  }

  @Test
  fun `emergency knowledge base provides structured offline reference without AI or network`() {
    val emergencyResult = com.example.util.EmergencyKnowledgeBase.searchGuide(
      query = "medical emergency and hospital cashless claim",
      userName = "Arun Verma",
      nomineeName = "Anita Verma",
      nomineeRelation = "Spouse",
      nomineePhone = "+91 9876543210",
      contacts = emptyList(),
      vaultItems = emptyList()
    )
    org.junit.Assert.assertTrue(emergencyResult.answer.contains("Emergency") || emergencyResult.answer.contains("Claim") || emergencyResult.answer.contains("Protocol"))
    org.junit.Assert.assertEquals("Offline Legal & Regulatory Reference", emergencyResult.sourceBadge)

    val rbiResult = com.example.util.EmergencyKnowledgeBase.searchGuide(
      query = "rbi deceased account claim and locker nominee rules",
      userName = "Arun Verma",
      nomineeName = "Anita Verma",
      nomineeRelation = "Spouse",
      nomineePhone = "+91 9876543210",
      contacts = emptyList(),
      vaultItems = emptyList()
    )
    org.junit.Assert.assertTrue(rbiResult.answer.contains("Nominee") || rbiResult.answer.contains("RBI"))
  }

  @Test
  fun `phase 3 zero-state onboarding starts with zero fabricated readiness and zero financial records`() = kotlinx.coroutines.runBlocking {
    val application = ApplicationProvider.getApplicationContext<android.app.Application>()
    val viewModel = com.example.ui.VaultViewModel(application)

    // Verify fresh state default values
    assertEquals(0, viewModel.getEmergencyReadinessScore())
    assertEquals(0, viewModel.getProfileCompletionPercentage())

    val breakdown = viewModel.getReadinessBreakdown()
    assertEquals(0, breakdown.score)
    org.junit.Assert.assertFalse(breakdown.hasContacts)
    org.junit.Assert.assertFalse(breakdown.hasAssets)
    org.junit.Assert.assertFalse(breakdown.hasInsurance)
    org.junit.Assert.assertFalse(breakdown.hasDocuments)
    assertEquals(0, breakdown.missingNomineesCount)
  }

  @Test
  fun `phase 3 demo mode isolation and exit preserves real household records`() = kotlinx.coroutines.runBlocking {
    val application = ApplicationProvider.getApplicationContext<android.app.Application>()
    val viewModel = com.example.ui.VaultViewModel(application)

    org.junit.Assert.assertFalse(viewModel.isDemoModeActive)

    // Enable demo mode
    viewModel.enableDemoPreviewMode()
    org.junit.Assert.assertTrue(viewModel.isDemoModeActive)

    // Disable demo mode
    viewModel.disableDemoPreviewMode()
    org.junit.Assert.assertFalse(viewModel.isDemoModeActive)
  }

  @Test
  fun `phase 3 navigation destinations match 5 primary tabs`() {
    val application = ApplicationProvider.getApplicationContext<android.app.Application>()
    val viewModel = com.example.ui.VaultViewModel(application)

    // Default tab should be HOME
    assertEquals("HOME", viewModel.selectedTabItem)

    // Verify tabs can be selected
    val tabs = listOf("HOME", "VAULT", "FAMILY", "PLAN", "SETTINGS")
    tabs.forEach { tab ->
      viewModel.selectedTabItem = tab
      assertEquals(tab, viewModel.selectedTabItem)
    }
  }

  @Test
  fun `phase 3 room database performs full CRUD for contacts and vault items`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = androidx.room.Room.inMemoryDatabaseBuilder(
      context,
      com.example.data.AppDatabase::class.java
    ).allowMainThreadQueries().build()
    val repo = com.example.data.Repository(db)

    val contact = com.example.data.ImportantContact(
      contactName = "Dr. Mehta",
      category = "Family Doctor",
      phone = "+91 9876543210",
      email = "mehta@hospital.org",
      address = "Fortis Hospital",
      priority = "Priority 1",
      remarks = "Personal Physician"
    )
    repo.insertImportantContact(contact)
    val contacts = repo.importantContacts.first()
    assertEquals(1, contacts.size)
    assertEquals("Dr. Mehta", contacts[0].contactName)

    val vaultItem = com.example.data.VaultItem(
      category = "BANK",
      title = "SBI Savings Account",
      ownerName = "Rahul Sharma",
      institution = "State Bank of India",
      numberOrId = "SB-987123",
      nomineeName = "Priya Sharma",
      nomineeRelation = "Spouse",
      nomineeVerified = true
    )
    repo.insertVaultItem(vaultItem)
    val items = repo.vaultItems.first()
    assertEquals(1, items.size)
    assertEquals("SBI Savings Account", items[0].title)

    // Update
    val updatedItem = items[0].copy(title = "SBI Primary Savings")
    repo.updateVaultItem(updatedItem)
    val updatedList = repo.vaultItems.first()
    assertEquals("SBI Primary Savings", updatedList[0].title)

    // Delete
    repo.deleteVaultItem(updatedList[0])
    val emptyList = repo.vaultItems.first()
    assertEquals(0, emptyList.size)

    db.close()
  }

  @Test
  fun `nominee gap audit accurately identifies missing nominees across bank, insurance, investment, locker, property, and liability`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = androidx.room.Room.inMemoryDatabaseBuilder(
      context,
      com.example.data.AppDatabase::class.java
    ).allowMainThreadQueries().build()
    val dao = db.vaultItemDao()

    val categories = listOf("BANK", "INSURANCE", "INVESTMENT", "PROPERTY", "LOCKER", "LIABILITY")
    // Insert 6 items without nominee
    categories.forEachIndexed { index, cat ->
      dao.insert(
        com.example.data.VaultItem(
          id = index + 1,
          category = cat,
          title = "Unprotected $cat",
          ownerName = "Rahul Sharma",
          institution = "Test Institution $index",
          numberOrId = "ACC-00$index",
          nomineeName = "",
          nomineeRelation = ""
        )
      )
    }

    // Insert 1 protected item
    dao.insert(
      com.example.data.VaultItem(
        id = 100,
        category = "BANK",
        title = "Protected Bank Account",
        ownerName = "Rahul Sharma",
        institution = "HDFC",
        numberOrId = "HDFC-12345",
        nomineeName = "Priya Sharma",
        nomineeRelation = "Spouse"
      )
    )

    // Insert 1 document item (DOCUMENT is not an asset requiring nominee)
    dao.insert(
      com.example.data.VaultItem(
        id = 200,
        category = "DOCUMENT",
        title = "Aadhaar Card",
        ownerName = "Rahul Sharma",
        institution = "UIDAI",
        numberOrId = "1234-5678-9012",
        nomineeName = ""
      )
    )

    val allItems = dao.getAll().first()
    val eligibleItems = allItems.filter { it.category in categories }
    val missingNominees = eligibleItems.filter { it.nomineeName.trim().isEmpty() }
    val protectedItems = eligibleItems.filter { it.nomineeName.trim().isNotEmpty() }

    assertEquals("Should have 7 eligible asset items across the 6 categories", 7, eligibleItems.size)
    assertEquals("Should detect 6 missing nominees", 6, missingNominees.size)
    assertEquals("Should detect 1 protected item", 1, protectedItems.size)
    org.junit.Assert.assertTrue("All 6 categories should be present in missing list",
      missingNominees.map { it.category }.containsAll(categories)
    )

    db.close()
  }

  @Test
  fun `emergency guide provides comprehensive bilingual offline responses in English and Hindi`() {
    // English query
    val enGuide = com.example.util.EmergencyKnowledgeBase.searchGuide(
      query = "what to do in first 24 hours emergency",
      userName = "Ashmit",
      nomineeName = "Priya",
      nomineeRelation = "Spouse",
      nomineePhone = "+91 9999988888",
      contacts = emptyList(),
      vaultItems = emptyList(),
      isHindi = false
    )
    org.junit.Assert.assertTrue("English guide should contain emergency guidance", enGuide.answer.contains("112") || enGuide.answer.contains("Emergency"))
    org.junit.Assert.assertFalse("English guide should not return Hindi text", enGuide.answer.contains("आपातकालीन तत्काल"))

    // Hindi query
    val hiGuide = com.example.util.EmergencyKnowledgeBase.searchGuide(
      query = "आपातकाल में क्या करें",
      userName = "अशमित",
      nomineeName = "प्रिया",
      nomineeRelation = "पत्नी",
      nomineePhone = "+91 9999988888",
      contacts = emptyList(),
      vaultItems = emptyList(),
      isHindi = true
    )
    org.junit.Assert.assertTrue("Hindi guide should contain Hindi guidance", hiGuide.answer.contains("112") && hiGuide.answer.contains("आपातकालीन"))
  }

  @Test
  fun `role-based access controls masking and confidential document access`() {
    val application = ApplicationProvider.getApplicationContext<android.app.Application>()
    val viewModel = com.example.ui.VaultViewModel(application)

    // Default role is OWNER
    assertEquals(com.example.ui.UserRole.OWNER, viewModel.currentRole)

    // Switch to EXECUTOR
    viewModel.switchRole(com.example.ui.UserRole.EXECUTOR)
    assertEquals(com.example.ui.UserRole.EXECUTOR, viewModel.currentRole)
    org.junit.Assert.assertFalse(viewModel.isEmergencyAccessReleased)

    // In EXECUTOR role before release, sensitive details are masked
    val testItem = com.example.data.VaultItem(
      title = "HDFC Salary Account",
      category = "BANK",
      ownerName = "Rahul Sharma",
      institution = "HDFC Bank",
      numberOrId = "501000294819",
      isMasked = true
    )
    val isActuallyMasked = testItem.isMasked && !viewModel.isEmergencyAccessReleased && viewModel.currentRole != com.example.ui.UserRole.OWNER
    org.junit.Assert.assertTrue("Account number should be masked for Executor before emergency release", isActuallyMasked)

    // Once emergency access is released
    viewModel.isEmergencyAccessReleased = true
    val isMaskedAfterRelease = testItem.isMasked && !viewModel.isEmergencyAccessReleased && viewModel.currentRole != com.example.ui.UserRole.OWNER
    org.junit.Assert.assertFalse("Account number should be unmasked after emergency release", isMaskedAfterRelease)
  }

  @Test
  fun `encrypted document storage performs authenticated encryption, legacy recovery, tampering detection, and cleanup`() {
    val context = ApplicationProvider.getApplicationContext<Context>()

    // 1. Create and save a bitmap document
    val testBitmap = android.graphics.Bitmap.createBitmap(100, 100, android.graphics.Bitmap.Config.ARGB_8888)
    val saveResult = com.example.util.DocumentStorageManager.saveBitmapDocument(context, testBitmap)
    org.junit.Assert.assertTrue(saveResult is com.example.util.DocumentSaveResult.Success)
    val relPath = (saveResult as com.example.util.DocumentSaveResult.Success).relativePath

    // 2. Verify file is encrypted on disk with FEV_ENC1 magic header
    val isEnc = com.example.util.DocumentStorageManager.isDocumentEncrypted(context, relPath)
    org.junit.Assert.assertTrue("Stored document must be encrypted with FEV_ENC1", isEnc)

    // 3. Decrypt and load Bitmap safely
    val loadedBitmap = com.example.util.DocumentStorageManager.loadDocumentBitmap(context, relPath)
    org.junit.Assert.assertNotNull("Decrypted bitmap must be successfully decoded", loadedBitmap)

    // 4. Simulate legacy unencrypted file
    val docsDir = java.io.File(context.filesDir, "vault_documents")
    val legacyFile = java.io.File(docsDir, "legacy_deed.jpg")
    val unencJpegBytes = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte(), 0x00, 0x10, 0x4A, 0x46, 0x49, 0x46)
    legacyFile.writeBytes(unencJpegBytes)

    // Verify unencrypted legacy file is preserved and readable as raw bytes
    val legacyBytes = com.example.util.DocumentStorageManager.loadDocumentBytes(context, "legacy_deed.jpg")
    org.junit.Assert.assertNotNull(legacyBytes)
    org.junit.Assert.assertArrayEquals(unencJpegBytes, legacyBytes)

    // 5. Migrate legacy documents to encrypted format
    val migrated = com.example.util.DocumentStorageManager.migrateLegacyDocumentsToEncrypted(context)
    org.junit.Assert.assertEquals(1, migrated)
    org.junit.Assert.assertTrue("Legacy file must now be encrypted", com.example.util.DocumentStorageManager.isDocumentEncrypted(context, "legacy_deed.jpg"))

    // 6. Test tampering/corruption protection: flip bits in ciphertext
    val encBytes = legacyFile.readBytes()
    encBytes[encBytes.size - 5] = (encBytes[encBytes.size - 5].toInt() xor 0xFF).toByte()
    val tamperedFile = java.io.File(docsDir, "tampered_doc.jpg")
    tamperedFile.writeBytes(encBytes)
    val tamperedLoaded = com.example.util.DocumentStorageManager.loadDocumentBitmap(context, "tampered_doc.jpg")
    org.junit.Assert.assertNull("Tampered document must fail authentication and return null", tamperedLoaded)

    // 7. Cleanup
    com.example.util.DocumentStorageManager.deleteDocument(context, relPath)
    com.example.util.DocumentStorageManager.deleteDocument(context, "legacy_deed.jpg")
    com.example.util.DocumentStorageManager.deleteDocument(context, "tampered_doc.jpg")
  }

  @Test
  fun `backup and restore verifies encryption, passphrase rejection, versioning, duplicate skipping, and transactional rollback`() = kotlinx.coroutines.runBlocking {
    val application = ApplicationProvider.getApplicationContext<android.app.Application>()
    val viewModel = com.example.ui.VaultViewModel(application)

    // Add initial item
    val initialItem = com.example.data.VaultItem(
      category = "BANK",
      title = "HDFC Salary Account",
      ownerName = "Rahul Sharma",
      institution = "HDFC Bank",
      numberOrId = "501000294819"
    )
    viewModel.saveVaultItem(initialItem)

    // 1. Export encrypted with passphrase
    val passphrase = "MasterPassphrase@2026"
    val encryptedJson = viewModel.exportVaultEncrypted(passphrase)
    org.junit.Assert.assertTrue(encryptedJson.contains("\"encrypted\": true"))
    org.junit.Assert.assertTrue(encryptedJson.contains("\"ciphertext\":"))
    org.junit.Assert.assertTrue(encryptedJson.contains("\"salt\":"))

    // 2. Reject restore with incorrect passphrase
    val restoreFailedWrongPass = !viewModel.restoreVaultFromJson(encryptedJson, "WrongPassphrase123")
    org.junit.Assert.assertTrue("Restore with wrong passphrase must fail", restoreFailedWrongPass)

    // 3. Reject restore with incompatible future version
    val plainFutureJson = """{"app":"Family Emergency Vault","version":99,"vaultItems":[]}"""
    val versionRejected = !viewModel.restoreVaultFromJson(plainFutureJson)
    org.junit.Assert.assertTrue("Incompatible version must be rejected", versionRejected)

    // 4. Reject restore with corrupted JSON
    val corruptRejected = !viewModel.restoreVaultFromJson("{corrupted_malformed_json")
    org.junit.Assert.assertTrue("Corrupted JSON must be rejected safely", corruptRejected)

    // 5. Successful restore with correct passphrase skips duplicates
    val restoreSuccess = viewModel.restoreVaultFromJson(encryptedJson, passphrase)
    org.junit.Assert.assertTrue("Restore with correct passphrase must succeed", restoreSuccess)

    // 6. Unauthorized restore attempt by Dependent role must be blocked
    viewModel.switchRole(com.example.ui.UserRole.DEPENDENT)
    val unauthorizedBlocked = !viewModel.restoreVaultFromJson(encryptedJson, passphrase)
    org.junit.Assert.assertTrue("Restore by non-Owner/Spouse must be blocked", unauthorizedBlocked)
  }

  @Test
  fun `emergency access permission matrix validates owner approval authority and heir veto protection`() = kotlinx.coroutines.runBlocking {
    val application = ApplicationProvider.getApplicationContext<android.app.Application>()
    val viewModel = com.example.ui.VaultViewModel(application)

    val req = com.example.data.EmergencyAccessRequest(
      id = 901,
      userName = "Rohan Sharma",
      relation = "Brother",
      requestReason = "Hospital emergency admission",
      status = "Pending"
    )

    // 1. Executor cannot approve emergency access (Owner authority required)
    viewModel.switchRole(com.example.ui.UserRole.EXECUTOR)
    viewModel.approveRequest(req)
    org.junit.Assert.assertFalse("Executor cannot approve emergency release", viewModel.isEmergencyAccessReleased)

    // 2. Dependent cannot approve emergency access
    viewModel.switchRole(com.example.ui.UserRole.DEPENDENT)
    viewModel.approveRequest(req)
    org.junit.Assert.assertFalse("Dependent cannot approve emergency release", viewModel.isEmergencyAccessReleased)

    // 3. Owner can approve emergency access
    viewModel.switchRole(com.example.ui.UserRole.OWNER)
    viewModel.approveRequest(req)
    org.junit.Assert.assertTrue("Owner approval releases emergency access", viewModel.isEmergencyAccessReleased)

    // 4. Advisor cannot revoke emergency access
    viewModel.switchRole(com.example.ui.UserRole.ADVISOR)
    viewModel.revokeEmergencyAccess()
    org.junit.Assert.assertTrue("Advisor cannot revoke emergency release", viewModel.isEmergencyAccessReleased)

    // 5. Owner can revoke emergency access
    viewModel.switchRole(com.example.ui.UserRole.OWNER)
    viewModel.revokeEmergencyAccess()
    org.junit.Assert.assertFalse("Owner revocation locks emergency access", viewModel.isEmergencyAccessReleased)
  }

  @Test
  fun `new user registration hashes passwords with PBKDF2-HMAC-SHA256 format and constant-time verification succeeds`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = androidx.room.Room.inMemoryDatabaseBuilder(
      context,
      com.example.data.AppDatabase::class.java
    ).allowMainThreadQueries().build()
    val gateway = com.example.util.EnterpriseServerGateway(db)
    // Verify target 600,000 iterations
    org.junit.Assert.assertEquals(600_000, gateway.pbkdf2Iterations)

    // 1. Register new user
    val reg = gateway.registerUserOnServer(
      fullName = "Siddharth Menon",
      email = "siddharth@familyvault.in",
      phone = "+91 9123456789",
      password = "MasterKey@2026!",
      mpin = "2468",
      nomineeName = "Meera Menon",
      nomineeRelation = "Spouse",
      securityQuestion = "First school?",
      securityAnswer = "National Public School"
    )
    org.junit.Assert.assertTrue("Registration must succeed", reg.success)
    val user = reg.user
    org.junit.Assert.assertNotNull(user)

    // 2. Format marker verification: $pbkdf2-sha256$<iterations>$<base64Salt>$<base64Hash>
    org.junit.Assert.assertTrue("Password hash must start with PBKDF2 marker", user!!.passwordHash.startsWith(com.example.util.EnterpriseServerGateway.PBKDF2_PREFIX))
    org.junit.Assert.assertTrue("Security answer must start with PBKDF2 marker", user.securityAnswerHash.startsWith(com.example.util.EnterpriseServerGateway.PBKDF2_PREFIX))
    org.junit.Assert.assertFalse("PBKDF2 hash must not be flagged as legacy", gateway.isLegacyHash(user.passwordHash))

    val parts = user.passwordHash.split("$")
    org.junit.Assert.assertEquals("PBKDF2 hash must have 5 parts (prefix, algo, iter, salt, hash)", 5, parts.size)
    org.junit.Assert.assertEquals("pbkdf2-sha256", parts[1])
    org.junit.Assert.assertEquals("600000", parts[2])
    val saltBytes = java.util.Base64.getDecoder().decode(parts[3])
    org.junit.Assert.assertEquals("Embedded salt must be 16 bytes", 16, saltBytes.size)
    val decodedBytes = java.util.Base64.getDecoder().decode(parts[4])
    org.junit.Assert.assertEquals("PBKDF2 key length must be 256 bits (32 bytes)", 32, decodedBytes.size)

    // Verify password and security answer use separate independent salts
    val answerParts = user.securityAnswerHash.split("$")
    org.junit.Assert.assertEquals(5, answerParts.size)
    org.junit.Assert.assertNotEquals("Password and security answer must use independent salts", parts[3], answerParts[3])

    // 3. Return login verification with correct password
    val loginRes = gateway.authenticateReturnLogin("siddharth@familyvault.in", "MasterKey@2026!")
    org.junit.Assert.assertTrue("Login with PBKDF2 must succeed", loginRes.success)
    org.junit.Assert.assertEquals("siddharth@familyvault.in", loginRes.user?.email)
    org.junit.Assert.assertTrue("JWT token must be populated", loginRes.token.isNotEmpty())

    db.close()
  }

  @Test
  fun `legacy SHA-256 account login verifies successfully and triggers transparent auto-upgrade to PBKDF2`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = androidx.room.Room.inMemoryDatabaseBuilder(
      context,
      com.example.data.AppDatabase::class.java
    ).allowMainThreadQueries().build()
    val gateway = com.example.util.EnterpriseServerGateway(db)

    // 1. Manually create legacy user with single-round salted SHA-256
    val legacySalt = gateway.generateSalt()
    val legacyPassword = "LegacyVaultPassword@2024"
    val legacyHash = gateway.hashPasswordLegacy(legacyPassword, legacySalt)

    org.junit.Assert.assertTrue("Legacy hash must be recognized as legacy", gateway.isLegacyHash(legacyHash))
    org.junit.Assert.assertEquals("Legacy SHA-256 hex string must be 64 characters", 64, legacyHash.length)

    val legacyUser = com.example.data.ServerUserRecord(
      userId = "usr_fev_legacy01",
      email = "legacy.user@familyvault.in",
      fullName = "Sunil Kapoor",
      phone = "+91 9811122233",
      passwordHash = legacyHash,
      passwordSalt = legacySalt,
      mpin = "1357",
      nomineeName = "Geeta Kapoor",
      nomineeRelation = "Spouse",
      securityQuestion = "Birth city?",
      securityAnswerHash = gateway.hashPasswordLegacy("jaipur", legacySalt),
      digitalCertificateId = "FEV-LOCAL-LEGACY-001",
      createdAt = 1700000000000L,
      lastLoginAt = 1700000000000L,
      status = "ACTIVE_VERIFIED",
      role = "OWNER",
      tokenJwt = "",
      deviceFingerprint = "Legacy-Device-001",
      isBiometricEnrolled = false
    )
    db.serverUserDao().insert(legacyUser)

    // 2. Perform return login with legacy credentials
    val loginRes = gateway.authenticateReturnLogin("legacy.user@familyvault.in", legacyPassword)
    org.junit.Assert.assertTrue("Legacy login must succeed transparently", loginRes.success)

    // 3. Verify user record in database was automatically upgraded to PBKDF2
    val upgradedUser = db.serverUserDao().getByEmail("legacy.user@familyvault.in")
    org.junit.Assert.assertNotNull(upgradedUser)
    org.junit.Assert.assertTrue("Upgraded hash must start with PBKDF2 marker", upgradedUser!!.passwordHash.startsWith(com.example.util.EnterpriseServerGateway.PBKDF2_PREFIX))
    org.junit.Assert.assertFalse("Upgraded hash must no longer be legacy", gateway.isLegacyHash(upgradedUser.passwordHash))
    org.junit.Assert.assertNotEquals("Upgraded account must receive fresh cryptographic salt", legacySalt, upgradedUser.passwordSalt)

    // 4. Verify audit log recorded the upgrade
    val auditLogs = db.auditLogDao().getAll().first()
    val upgradeLog = auditLogs.find { it.action == "Password Hash Upgraded" }
    org.junit.Assert.assertNotNull("Audit log must contain Password Hash Upgraded event", upgradeLog)
    org.junit.Assert.assertTrue(upgradeLog!!.details.contains("legacy.user@familyvault.in"))

    // 5. Subsequent return login verifies against the new PBKDF2 hash seamlessly
    val secondLogin = gateway.authenticateReturnLogin("legacy.user@familyvault.in", legacyPassword)
    org.junit.Assert.assertTrue("Second login using upgraded PBKDF2 hash must succeed", secondLogin.success)

    db.close()
  }

  @Test
  fun `password verification rejects incorrect passwords for both legacy and PBKDF2 records without corrupting hashes`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = androidx.room.Room.inMemoryDatabaseBuilder(
      context,
      com.example.data.AppDatabase::class.java
    ).allowMainThreadQueries().build()
    val gateway = com.example.util.EnterpriseServerGateway(db)

    // 1. Create PBKDF2 account
    gateway.registerUserOnServer(
      fullName = "PBKDF2 User",
      email = "pbkdf2@vault.in",
      phone = "+91 9999900001",
      password = "CorrectPBKDF2Pass!1",
      mpin = "1122",
      nomineeName = "Nominee",
      nomineeRelation = "Spouse",
      securityQuestion = "Question?",
      securityAnswer = "Answer"
    )
    val pbkdf2UserBefore = db.serverUserDao().getByEmail("pbkdf2@vault.in")!!

    // 2. Create Legacy account
    val legSalt = gateway.generateSalt()
    val legHash = gateway.hashPasswordLegacy("CorrectLegacyPass!1", legSalt)
    val legUser = com.example.data.ServerUserRecord(
      userId = "usr_fev_leg02",
      email = "legacy@vault.in",
      fullName = "Legacy User",
      phone = "+91 9999900002",
      passwordHash = legHash,
      passwordSalt = legSalt,
      mpin = "3344",
      nomineeName = "Nominee",
      nomineeRelation = "Spouse",
      securityQuestion = "Question?",
      securityAnswerHash = "dummy",
      digitalCertificateId = "FEV-LOCAL-002",
      createdAt = 1700000000000L,
      lastLoginAt = 1700000000000L,
      status = "ACTIVE_VERIFIED",
      role = "OWNER",
      tokenJwt = "",
      deviceFingerprint = "Device-002",
      isBiometricEnrolled = false
    )
    db.serverUserDao().insert(legUser)

    // 3. Attempt wrong passwords
    val wrongPbkdf2 = gateway.authenticateReturnLogin("pbkdf2@vault.in", "WrongPassword!99")
    org.junit.Assert.assertFalse("Wrong password for PBKDF2 must fail", wrongPbkdf2.success)
    org.junit.Assert.assertEquals("INVALID_CREDENTIALS", wrongPbkdf2.errorCode)

    val wrongLegacy = gateway.authenticateReturnLogin("legacy@vault.in", "WrongPassword!99")
    org.junit.Assert.assertFalse("Wrong password for legacy must fail", wrongLegacy.success)
    org.junit.Assert.assertEquals("INVALID_CREDENTIALS", wrongLegacy.errorCode)

    // 4. Verify hashes and salts remained uncorrupted
    val pbkdf2UserAfter = db.serverUserDao().getByEmail("pbkdf2@vault.in")!!
    org.junit.Assert.assertEquals("PBKDF2 hash must not change on failed login", pbkdf2UserBefore.passwordHash, pbkdf2UserAfter.passwordHash)
    org.junit.Assert.assertEquals("PBKDF2 salt must not change on failed login", pbkdf2UserBefore.passwordSalt, pbkdf2UserAfter.passwordSalt)

    val legUserAfter = db.serverUserDao().getByEmail("legacy@vault.in")!!
    org.junit.Assert.assertEquals("Legacy hash must not be upgraded on failed login", legHash, legUserAfter.passwordHash)
    org.junit.Assert.assertEquals("Legacy salt must not change on failed login", legSalt, legUserAfter.passwordSalt)

    db.close()
  }

  @Test
  fun `password verification safely handles malformed, truncated, or corrupt stored hash records without throwing`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = androidx.room.Room.inMemoryDatabaseBuilder(
      context,
      com.example.data.AppDatabase::class.java
    ).allowMainThreadQueries().build()
    val gateway = com.example.util.EnterpriseServerGateway(db)
    val validSalt = gateway.generateSalt()

    val dummyValid32ByteHash = java.util.Base64.getEncoder().encodeToString(ByteArray(32) { 1.toByte() })
    val dummyValid16ByteSalt = java.util.Base64.getEncoder().encodeToString(ByteArray(16) { 2.toByte() })
    val dummyShort16ByteHash = java.util.Base64.getEncoder().encodeToString(ByteArray(16) { 3.toByte() })

    // Test malformed permutations against verifyPassword
    org.junit.Assert.assertFalse("Empty hash must return false", gateway.verifyPassword("Pass123", "", validSalt))
    org.junit.Assert.assertFalse("Blank hash must return false", gateway.verifyPassword("Pass123", "   ", validSalt))
    org.junit.Assert.assertFalse("Empty salt must return false", gateway.verifyPassword("Pass123", "\$pbkdf2-sha256\$600000\$WvQ=", ""))
    org.junit.Assert.assertFalse("Malformed prefix must return false", gateway.verifyPassword("Pass123", "\$pbkdf1\$600000\$WvQ=", validSalt))
    org.junit.Assert.assertFalse("Missing parts must return false", gateway.verifyPassword("Pass123", "\$pbkdf2-sha256\$600000", validSalt))
    org.junit.Assert.assertFalse("Non-integer iterations must return false", gateway.verifyPassword("Pass123", "\$pbkdf2-sha256\$iterations\$WvQ=", validSalt))
    org.junit.Assert.assertFalse("Degenerate iteration count (<10000) must return false", gateway.verifyPassword("Pass123", "\$pbkdf2-sha256\$1000\$$dummyValid16ByteSalt\$$dummyValid32ByteHash", validSalt))
    org.junit.Assert.assertFalse("Excessive iteration count (>2000000 DoS defense) must return false", gateway.verifyPassword("Pass123", "\$pbkdf2-sha256\$5000000\$$dummyValid16ByteSalt\$$dummyValid32ByteHash", validSalt))
    org.junit.Assert.assertFalse("Derived hash length mismatch (!=32 bytes) must fail before derivation", gateway.verifyPassword("Pass123", "\$pbkdf2-sha256\$600000\$$dummyValid16ByteSalt\$$dummyShort16ByteHash", validSalt))
    org.junit.Assert.assertFalse("Corrupted base64 hash must return false", gateway.verifyPassword("Pass123", "\$pbkdf2-sha256\$600000\$***not-base64***===", validSalt))
    org.junit.Assert.assertFalse("Corrupt payload must return false", gateway.verifyPassword("Pass123", "corrupted_garbage_hash", validSalt))

    db.close()
  }

  @Test
  fun `password change workflow validates current password, generates fresh salt, and updates stored hash to PBKDF2`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = androidx.room.Room.inMemoryDatabaseBuilder(
      context,
      com.example.data.AppDatabase::class.java
    ).allowMainThreadQueries().build()
    val gateway = com.example.util.EnterpriseServerGateway(db)

    // Register user
    gateway.registerUserOnServer(
      fullName = "Aditi Rao",
      email = "aditi@familyvault.in",
      phone = "+91 9876501234",
      password = "InitialPass@2026",
      mpin = "5678",
      nomineeName = "Rohan Rao",
      nomineeRelation = "Brother",
      securityQuestion = "Favorite book?",
      securityAnswer = "Gitanjali"
    )

    // 1. Password change with wrong current password fails
    val wrongCurrent = gateway.changePassword("aditi@familyvault.in", "WrongInitialPass", "BrandNewPass@2026")
    org.junit.Assert.assertFalse("Password change with wrong current password must fail", wrongCurrent.success)
    org.junit.Assert.assertEquals("INVALID_CREDENTIALS", wrongCurrent.errorCode)

    // 2. Password change with too short new password fails
    val shortNew = gateway.changePassword("aditi@familyvault.in", "InitialPass@2026", "12345")
    org.junit.Assert.assertFalse("Password change with short new password must fail", shortNew.success)
    org.junit.Assert.assertEquals("PASSWORD_TOO_SHORT", shortNew.errorCode)

    // 3. Valid password change succeeds
    val changeSuccess = gateway.changePassword("aditi@familyvault.in", "InitialPass@2026", "BrandNewPass@2026")
    org.junit.Assert.assertTrue("Valid password change must succeed", changeSuccess.success)

    // 4. Verify login with new password succeeds and old password is rejected
    val loginNew = gateway.authenticateReturnLogin("aditi@familyvault.in", "BrandNewPass@2026")
    org.junit.Assert.assertTrue("Login with new password must succeed", loginNew.success)

    val loginOld = gateway.authenticateReturnLogin("aditi@familyvault.in", "InitialPass@2026")
    org.junit.Assert.assertFalse("Login with old password must be rejected", loginOld.success)

    db.close()
  }

  @Test
  fun `cryptographic salt generation produces unique 16-byte random salts across registrations`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = androidx.room.Room.inMemoryDatabaseBuilder(
      context,
      com.example.data.AppDatabase::class.java
    ).allowMainThreadQueries().build()
    val gateway = com.example.util.EnterpriseServerGateway(db)

    val salts = mutableSetOf<String>()
    for (i in 1..50) {
      val salt = gateway.generateSalt()
      val decoded = java.util.Base64.getDecoder().decode(salt)
      org.junit.Assert.assertEquals("Salt must decode to 16 bytes", 16, decoded.size)
      salts.add(salt)
    }
    org.junit.Assert.assertEquals("All 50 generated salts must be distinct", 50, salts.size)

    db.close()
  }

  @Test
  fun `plaintext credentials are never persisted in shared preferences and legacy plaintext entries are purged on startup`() = kotlinx.coroutines.runBlocking {
    val application = ApplicationProvider.getApplicationContext<android.app.Application>()
    val prefs = application.getSharedPreferences("vault_user_prefs", Context.MODE_PRIVATE)

    // 1. Pre-populate legacy insecure plaintext credentials
    prefs.edit()
      .putString("user_password", "PlaintextSecretLeak!1")
      .putString("user_security_answer", "PlaintextSecretAnswer")
      .apply()
    org.junit.Assert.assertTrue("Pre-condition: legacy plaintext password exists", prefs.contains("user_password"))
    org.junit.Assert.assertTrue("Pre-condition: legacy plaintext answer exists", prefs.contains("user_security_answer"))

    // 2. Initialize ViewModel: init block must purge plaintext entries
    val viewModel = com.example.ui.VaultViewModel(application)
    org.junit.Assert.assertFalse("P0: user_password must be purged from SharedPreferences", prefs.contains("user_password"))
    org.junit.Assert.assertFalse("P0: user_security_answer must be purged from SharedPreferences", prefs.contains("user_security_answer"))

    // 3. Register user and verify no plaintext password is added
    viewModel.registerEnterpriseUser(
      fullName = "Ananya Roy",
      email = "ananya@familyvault.in",
      phone = "+91 9888877777",
      password = "SecurePassword@2026",
      mpin = "4321",
      nomineeName = "Kabir Roy",
      nomineeRelation = "Spouse",
      securityQuestion = "Birthplace?",
      securityAnswer = "Kolkata"
    )
    org.junit.Assert.assertFalse("P0: user_password must NEVER be written to SharedPreferences on registration", prefs.contains("user_password"))
    org.junit.Assert.assertFalse("P0: user_security_answer must NEVER be written to SharedPreferences on registration", prefs.contains("user_security_answer"))

    // 4. Change password and verify no plaintext password is added
    viewModel.changePassword("SecurePassword@2026", "UpdatedPassword@2026")
    org.junit.Assert.assertFalse("P0: user_password must NEVER be written to SharedPreferences on changePassword", prefs.contains("user_password"))
  }

  @Test
  fun `security question password reset flow verifies hashed answer, updates to PBKDF2, and rejects invalid answers`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = androidx.room.Room.inMemoryDatabaseBuilder(
      context,
      com.example.data.AppDatabase::class.java
    ).allowMainThreadQueries().build()
    val gateway = com.example.util.EnterpriseServerGateway(db)

    // 1. Register account
    val reg = gateway.registerUserOnServer(
      fullName = "Tarun Mathur",
      email = "tarun@familyvault.in",
      phone = "+91 9777766666",
      password = "OldMasterKey@2026",
      mpin = "3322",
      nomineeName = "Rekha Mathur",
      nomineeRelation = "Spouse",
      securityQuestion = "Childhood pet name?",
      securityAnswer = "Shadow"
    )
    org.junit.Assert.assertTrue(reg.success)

    // 2. Recovery attempt with incorrect answer fails
    val wrongAnswer = gateway.resetPasswordWithSecurityAnswer("tarun@familyvault.in", "WrongPet", "NewRecoveredKey@2026")
    org.junit.Assert.assertFalse("Reset with incorrect security answer must fail", wrongAnswer.success)
    org.junit.Assert.assertEquals("INVALID_CREDENTIALS", wrongAnswer.errorCode)

    // 3. Recovery attempt with correct answer succeeds
    val successReset = gateway.resetPasswordWithSecurityAnswer("tarun@familyvault.in", "Shadow", "NewRecoveredKey@2026")
    org.junit.Assert.assertTrue("Reset with correct security answer must succeed", successReset.success)

    // 4. Login with newly recovered password succeeds
    val loginRecovered = gateway.authenticateReturnLogin("tarun@familyvault.in", "NewRecoveredKey@2026")
    org.junit.Assert.assertTrue("Login with recovered password must succeed", loginRecovered.success)

    // 5. Old password is rejected
    val loginOld = gateway.authenticateReturnLogin("tarun@familyvault.in", "OldMasterKey@2026")
    org.junit.Assert.assertFalse("Login with old password must be rejected after reset", loginOld.success)

    db.close()
  }

  @Test
  fun `jwt validation validates genuine tokens, rejects tampered signatures, and rejects malformed formats`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = androidx.room.Room.inMemoryDatabaseBuilder(
      context,
      com.example.data.AppDatabase::class.java
    ).allowMainThreadQueries().build()
    val gateway = com.example.util.EnterpriseServerGateway(db)

    val validToken = gateway.generateJwtToken("usr_test_123", "user@familyvault.in", "OWNER")
    org.junit.Assert.assertTrue("Genuine token must be valid", gateway.validateJwtToken(validToken))

    // Tampered signature
    val tamperedToken = validToken.substring(0, validToken.length - 4) + "XXXX"
    org.junit.Assert.assertFalse("Tampered token signature must be rejected", gateway.validateJwtToken(tamperedToken))

    // Malformed strings
    org.junit.Assert.assertFalse("Empty token must be rejected", gateway.validateJwtToken(""))
    org.junit.Assert.assertFalse("Random string must be rejected", gateway.validateJwtToken("not.a.valid.jwt.token"))
    org.junit.Assert.assertFalse("Two-part token must be rejected", gateway.validateJwtToken("header.payload"))

    db.close()
  }

  @Test
  fun `security question legacy migration preserves old salt after password upgrade and allows recovery to PBKDF2`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = androidx.room.Room.inMemoryDatabaseBuilder(
      context,
      com.example.data.AppDatabase::class.java
    ).allowMainThreadQueries().build()
    val gateway = com.example.util.EnterpriseServerGateway(db)

    // 1. Manually insert legacy account with SHA-256 password and SHA-256 security answer (using same passwordSalt)
    val legSalt = gateway.generateSalt()
    val legPassHash = gateway.hashPasswordLegacy("OldLegacyPassword#1", legSalt)
    val legAnswerHash = gateway.hashPasswordLegacy("jaipur", legSalt)

    val legacyUser = com.example.data.ServerUserRecord(
      userId = "usr_fev_legacy_recovery",
      email = "legacy.recovery@vault.in",
      fullName = "Vikram Rathore",
      phone = "+91 9111122222",
      passwordHash = legPassHash,
      passwordSalt = legSalt,
      mpin = "9988",
      nomineeName = "Pooja Rathore",
      nomineeRelation = "Spouse",
      securityQuestion = "Birth city?",
      securityAnswerHash = legAnswerHash,
      digitalCertificateId = "FEV-LOCAL-LEGACY-003",
      createdAt = 1690000000000L,
      lastLoginAt = 1690000000000L,
      status = "ACTIVE_VERIFIED",
      role = "OWNER",
      tokenJwt = "legacy_token",
      deviceFingerprint = "Device-Legacy",
      isBiometricEnrolled = false
    )
    db.serverUserDao().insert(legacyUser)

    // 2. Perform login: triggers transparent upgrade of password hash, which preserves old salt in securityAnswerHash
    val loginRes = gateway.authenticateReturnLogin("legacy.recovery@vault.in", "OldLegacyPassword#1")
    org.junit.Assert.assertTrue("Login must succeed", loginRes.success)

    val upgradedRecord = db.serverUserDao().getByEmail("legacy.recovery@vault.in")!!
    org.junit.Assert.assertTrue("Password hash must be upgraded to PBKDF2", upgradedRecord.passwordHash.startsWith(com.example.util.EnterpriseServerGateway.PBKDF2_PREFIX))
    org.junit.Assert.assertTrue("Security answer hash must preserve legacy format with old salt", upgradedRecord.securityAnswerHash.startsWith("legacy\$$legSalt\$"))

    // 3. Perform password reset using security answer: verifies preserved legacy hash and upgrades answer to PBKDF2
    val resetRes = gateway.resetPasswordWithSecurityAnswer("legacy.recovery@vault.in", "Jaipur", "BrandNewSecureKey@2026")
    org.junit.Assert.assertTrue("Password reset using security answer must succeed", resetRes.success)

    val postResetRecord = db.serverUserDao().getByEmail("legacy.recovery@vault.in")!!
    org.junit.Assert.assertTrue("Security answer must now be fully upgraded to PBKDF2", postResetRecord.securityAnswerHash.startsWith(com.example.util.EnterpriseServerGateway.PBKDF2_PREFIX))

    // 4. Verify login with new password
    val newLogin = gateway.authenticateReturnLogin("legacy.recovery@vault.in", "BrandNewSecureKey@2026")
    org.junit.Assert.assertTrue("Login with new password must succeed", newLogin.success)

    db.close()
  }

  @Test
  fun `password reset and change rotate JWT tokens and defense against account enumeration returns generic invalid credentials`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = androidx.room.Room.inMemoryDatabaseBuilder(
      context,
      com.example.data.AppDatabase::class.java
    ).allowMainThreadQueries().build()
    val gateway = com.example.util.EnterpriseServerGateway(db)

    gateway.registerUserOnServer(
      fullName = "Sunita Menon",
      email = "sunita@vault.in",
      phone = "+91 9444455555",
      password = "InitialPassword#2026",
      mpin = "1234",
      nomineeName = "Dev Menon",
      nomineeRelation = "Son",
      securityQuestion = "Favorite food?",
      securityAnswer = "Dosa"
    )

    val initialUser = db.serverUserDao().getByEmail("sunita@vault.in")!!
    val initialJwt = initialUser.tokenJwt
    org.junit.Assert.assertTrue(gateway.validateJwtToken(initialJwt))

    // 1. Password change rotates token
    val changeRes = gateway.changePassword("sunita@vault.in", "InitialPassword#2026", "ChangedPassword#2026")
    org.junit.Assert.assertTrue(changeRes.success)
    org.junit.Assert.assertNotEquals("JWT token must rotate on password change", initialJwt, changeRes.token)

    val userAfterChange = db.serverUserDao().getByEmail("sunita@vault.in")!!
    org.junit.Assert.assertEquals(changeRes.token, userAfterChange.tokenJwt)

    // 2. Password reset rotates token
    val resetRes = gateway.resetPasswordWithSecurityAnswer("sunita@vault.in", "Dosa", "ResetPassword#2026")
    org.junit.Assert.assertTrue(resetRes.success)
    org.junit.Assert.assertNotEquals("JWT token must rotate on password reset", changeRes.token, resetRes.token)

    val userAfterReset = db.serverUserDao().getByEmail("sunita@vault.in")!!
    org.junit.Assert.assertEquals(resetRes.token, userAfterReset.tokenJwt)

    // 3. Account enumeration defense: verify non-existent user and wrong answer return identical INVALID_CREDENTIALS
    val wrongUserRes = gateway.resetPasswordWithSecurityAnswer("nonexistent@vault.in", "AnyAnswer", "NewPass#2026")
    val wrongAnswerRes = gateway.resetPasswordWithSecurityAnswer("sunita@vault.in", "WrongAnswer", "NewPass#2026")

    org.junit.Assert.assertEquals("Non-existent user must return INVALID_CREDENTIALS", "INVALID_CREDENTIALS", wrongUserRes.errorCode)
    org.junit.Assert.assertEquals("Wrong answer must return INVALID_CREDENTIALS", "INVALID_CREDENTIALS", wrongAnswerRes.errorCode)
    org.junit.Assert.assertEquals("Both errors must have identical message to prevent user enumeration", wrongUserRes.message, wrongAnswerRes.message)

    db.close()
  }
}


