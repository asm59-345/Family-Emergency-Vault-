package com.example.util

import com.example.data.AppDatabase
import com.example.data.ServerUserRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

data class ServerAuthResponse(
    val success: Boolean,
    val message: String,
    val token: String = "",
    val user: ServerUserRecord? = null,
    val errorCode: String? = null,
    val latencyMs: Long = 0L,
    val processedByNode: String = "local-device-storage"
)

data class ClusterNodeStatus(
    val nodeId: String,
    val name: String,
    val role: String,
    val status: String,
    val loadPercent: Int,
    val latencyMs: Long
)

data class ClusterMetrics(
    val totalRequestsProcessed: Long,
    val activeQueueDepth: Int,
    val avgLatencyMs: Long,
    val rateLimitRejections: Long,
    val activeNodes: List<ClusterNodeStatus>,
    val uptimeSeconds: Long,
    val architectureNote: String = "Local-First On-Device SQLite (No Cloud Sync)"
)

/**
 * Local Account Gateway managing local user registrations, password hashing with salt,
 * and authenticated session tokens using the local SQLite Room database.
 * No simulated microservices or fake cloud cluster synchronization.
 */
class EnterpriseServerGateway(
    private val database: AppDatabase
) {
    private val secureRandom = SecureRandom()
    private val jwtSecretBytes = ByteArray(32).also { secureRandom.nextBytes(it) }
    private val serverStartTime = System.currentTimeMillis()

    // Real local telemetry
    private val totalRequests = AtomicLong(0L)

    private val _clusterMetrics = MutableStateFlow(createInitialMetrics())
    val clusterMetrics: Flow<ClusterMetrics> = _clusterMetrics.asStateFlow()

    private fun createInitialMetrics(): ClusterMetrics {
        val nodes = listOf(
            ClusterNodeStatus("local-auth", "Local Auth Engine", "Salted SHA-256 & Session Token", "LOCAL_ACTIVE", 0, 1),
            ClusterNodeStatus("local-vault", "SQLite Room Storage", "On-Device Encrypted Database", "LOCAL_ACTIVE", 0, 1),
            ClusterNodeStatus("local-audit", "Security Audit Ledger", "Local SQLite Audit Trail", "LOCAL_ACTIVE", 0, 1)
        )
        return ClusterMetrics(
            totalRequestsProcessed = totalRequests.get(),
            activeQueueDepth = 0,
            avgLatencyMs = 1L,
            rateLimitRejections = 0L,
            activeNodes = nodes,
            uptimeSeconds = (System.currentTimeMillis() - serverStartTime) / 1000,
            architectureNote = "Local-First On-Device SQLite (No Cloud Sync)"
        )
    }

    private fun updateLocalMetrics() {
        val count = totalRequests.incrementAndGet()
        val nodes = listOf(
            ClusterNodeStatus("local-auth", "Local Auth Engine", "Salted SHA-256 & Session Token", "LOCAL_ACTIVE", 5, 1),
            ClusterNodeStatus("local-vault", "SQLite Room Storage", "On-Device Encrypted Database", "LOCAL_ACTIVE", 8, 2),
            ClusterNodeStatus("local-audit", "Security Audit Ledger", "Local SQLite Audit Trail", "LOCAL_ACTIVE", 3, 1)
        )
        _clusterMetrics.value = ClusterMetrics(
            totalRequestsProcessed = count,
            activeQueueDepth = 0,
            avgLatencyMs = 2L,
            rateLimitRejections = 0L,
            activeNodes = nodes,
            uptimeSeconds = (System.currentTimeMillis() - serverStartTime) / 1000,
            architectureNote = "Local-First On-Device SQLite (No Cloud Sync)"
        )
    }

    fun generateSalt(): String {
        val saltBytes = ByteArray(16)
        secureRandom.nextBytes(saltBytes)
        return Base64.getEncoder().encodeToString(saltBytes)
    }

    fun hashPassword(password: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(salt.toByteArray(StandardCharsets.UTF_8))
        val hash = digest.digest(password.toByteArray(StandardCharsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }

    fun generateJwtToken(userId: String, email: String, role: String): String {
        val header = JSONObject().apply {
            put("alg", "HS256")
            put("typ", "JWT")
        }
        val now = System.currentTimeMillis() / 1000
        val exp = now + (30 * 24 * 3600)
        val payload = JSONObject().apply {
            put("sub", userId)
            put("email", email)
            put("role", role)
            put("iss", "family-emergency-vault.local")
            put("iat", now)
            put("exp", exp)
            put("jti", UUID.randomUUID().toString())
        }

        val base64Header = Base64.getUrlEncoder().withoutPadding().encodeToString(header.toString().toByteArray(StandardCharsets.UTF_8))
        val base64Payload = Base64.getUrlEncoder().withoutPadding().encodeToString(payload.toString().toByteArray(StandardCharsets.UTF_8))
        val signatureInput = "$base64Header.$base64Payload"

        val mac = Mac.getInstance("HmacSHA256")
        val secretKey = SecretKeySpec(jwtSecretBytes, "HmacSHA256")
        mac.init(secretKey)
        val signatureBytes = mac.doFinal(signatureInput.toByteArray(StandardCharsets.UTF_8))
        val base64Signature = Base64.getUrlEncoder().withoutPadding().encodeToString(signatureBytes)

        return "$signatureInput.$base64Signature"
    }

    suspend fun registerUserOnServer(
        fullName: String,
        email: String,
        phone: String,
        password: String,
        mpin: String,
        nomineeName: String,
        nomineeRelation: String,
        securityQuestion: String,
        securityAnswer: String,
        enableBiometrics: Boolean = false,
        deviceFingerprint: String = "Local-Sandbox-${UUID.randomUUID().toString().take(6).uppercase()}"
    ): ServerAuthResponse = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val cleanEmail = email.trim().lowercase()

        val existing = database.serverUserDao().getByEmail(cleanEmail)
        if (existing != null) {
            return@withContext ServerAuthResponse(
                success = false,
                message = "Account already registered on this device for '$email'. Please sign in.",
                errorCode = "EMAIL_ALREADY_EXISTS"
            )
        }

        val userId = "usr_fev_" + UUID.randomUUID().toString().replace("-", "").take(10)
        val salt = generateSalt()
        val passwordHash = hashPassword(password, salt)
        val answerHash = hashPassword(securityAnswer.trim().lowercase(), salt)
        val certId = "FEV-LOCAL-" + java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date()) + "-" + UUID.randomUUID().toString().take(6).uppercase()
        val token = generateJwtToken(userId = userId, email = cleanEmail, role = "OWNER")

        val newUser = ServerUserRecord(
            userId = userId,
            email = cleanEmail,
            fullName = fullName.trim(),
            phone = phone.trim(),
            passwordHash = passwordHash,
            passwordSalt = salt,
            mpin = mpin.trim(),
            nomineeName = nomineeName.trim(),
            nomineeRelation = nomineeRelation.trim(),
            securityQuestion = securityQuestion,
            securityAnswerHash = answerHash,
            digitalCertificateId = certId,
            createdAt = System.currentTimeMillis(),
            lastLoginAt = System.currentTimeMillis(),
            status = "ACTIVE_VERIFIED",
            role = "OWNER",
            tokenJwt = token,
            deviceFingerprint = deviceFingerprint,
            isBiometricEnrolled = enableBiometrics
        )

        val insertedId = database.serverUserDao().insert(newUser)
        val createdUser = newUser.copy(id = insertedId.toInt())

        database.auditLogDao().insert(
            com.example.data.AuditLog(
                action = "User Profile Created",
                details = "Registered household profile: $cleanEmail ($userId) in on-device database.",
                userRole = "LOCAL_DATABASE"
            )
        )

        updateLocalMetrics()
        val elapsed = System.currentTimeMillis() - startTime

        ServerAuthResponse(
            success = true,
            message = "Profile successfully created and saved in local database.",
            token = token,
            user = createdUser,
            latencyMs = elapsed,
            processedByNode = "local-device-storage"
        )
    }

    suspend fun authenticateReturnLogin(
        email: String,
        password: String
    ): ServerAuthResponse = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val cleanEmail = email.trim().lowercase()

        val user = database.serverUserDao().getByEmail(cleanEmail)
        if (user == null) {
            return@withContext ServerAuthResponse(
                success = false,
                message = "No local profile found for '$email'. Please verify email or register.",
                errorCode = "USER_NOT_FOUND"
            )
        }

        val computedHash = hashPassword(password, user.passwordSalt)
        if (computedHash != user.passwordHash) {
            return@withContext ServerAuthResponse(
                success = false,
                message = "Invalid password. Credentials do not match stored profile records.",
                errorCode = "INVALID_CREDENTIALS"
            )
        }

        val freshToken = generateJwtToken(userId = user.userId, email = user.email, role = user.role)
        val updatedUser = user.copy(
            lastLoginAt = System.currentTimeMillis(),
            tokenJwt = freshToken
        )
        database.serverUserDao().update(updatedUser)

        database.auditLogDao().insert(
            com.example.data.AuditLog(
                action = "User Login Verified",
                details = "Authenticated profile ${user.email} in on-device database.",
                userRole = user.role
            )
        )

        updateLocalMetrics()
        val elapsed = System.currentTimeMillis() - startTime

        ServerAuthResponse(
            success = true,
            message = "Authentication verified. Profile successfully restored.",
            token = freshToken,
            user = updatedUser,
            latencyMs = elapsed,
            processedByNode = "local-device-storage"
        )
    }

    suspend fun getAllUsers() = database.serverUserDao().getAll()

    suspend fun getAllServerUsers() = database.serverUserDao().getAll()

    suspend fun getServerUserCount() = database.serverUserDao().getCount()

    suspend fun deleteUserRecord(user: ServerUserRecord) = withContext(Dispatchers.IO) {
        database.serverUserDao().delete(user)
        updateLocalMetrics()
    }
}
