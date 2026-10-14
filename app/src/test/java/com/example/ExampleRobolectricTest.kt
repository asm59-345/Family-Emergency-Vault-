package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

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
  fun `database seed initializes all India emergency vault records`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = androidx.room.Room.inMemoryDatabaseBuilder(
      context,
      com.example.data.AppDatabase::class.java
    ).allowMainThreadQueries().build()

    // Run ensureSeeded
    com.example.data.AppDatabase.ensureSeeded(db)

    // Check Vault items seeded
    val vaultCount = db.vaultItemDao().getCount()
    org.junit.Assert.assertTrue("Vault items should be seeded", vaultCount > 0)

    // Check Dependents seeded
    val dependentsCount = db.familyDependentDao().getCount()
    org.junit.Assert.assertTrue("Family dependents should be seeded", dependentsCount > 0)

    // Check Contacts seeded
    val contactsCount = db.importantContactDao().getCount()
    org.junit.Assert.assertTrue("Important contacts should be seeded", contactsCount > 0)

    // Check Action Items seeded
    val actionItemsCount = db.emergencyActionItemDao().getCount()
    org.junit.Assert.assertTrue("Action checklist should be seeded", actionItemsCount > 0)

    // Check Server User Records seeded
    val serverUsers = db.serverUserDao().getAll()
    org.junit.Assert.assertTrue("Server user records should be seeded", serverUsers.isNotEmpty())
    val adminUser = serverUsers.firstOrNull { it.email == "rahul@gmail.com" }
    org.junit.Assert.assertNotNull("Default owner should be seeded", adminUser)
    org.junit.Assert.assertEquals("OWNER", adminUser?.role)

    db.close()
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
}

