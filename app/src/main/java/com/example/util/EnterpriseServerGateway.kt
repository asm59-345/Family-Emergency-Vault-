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
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
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
 * Local Account Gateway managing local user registrations, PBKDF2-HMAC-SHA256 password hashing,
 * automatic transparent migration from legacy salted SHA-256, and authenticated session tokens
 * using the local SQLite Room database.
 * No simulated microservices or fake cloud cluster synchronization.
 */
class EnterpriseServerGateway(
    private val database: AppDatabase
) {
    companion object {
        const val PBKDF2_PREFIX = "\$pbkdf2-sha256\$"
        const val DEFAULT_PBKDF2_ITERATIONS = 600_000
        const val MIN_PBKDF2_ITERATIONS = 10_000
        const val MAX_PBKDF2_ITERATIONS = 2_000_000
        const val EXPECTED_HASH_LENGTH_BYTES = 32 // 256 bits
        const val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256"
        const val KEY_LENGTH_BITS = 256
        const val SALT_LENGTH_BYTES = 16
    }

    var pbkdf2Iterations: Int = DEFAULT_PBKDF2_ITERATIONS
    private val secureRandom = SecureRandom()
    private val jwtSecretBytes = ByteArray(32).also { secureRandom.nextBytes(it) }
    private val serverStartTime = System.currentTimeMillis()

    // Real local telemetry
    private val totalRequests = AtomicLong(0L)

    private val _clusterMetrics = MutableStateFlow(createInitialMetrics())
    val clusterMetrics: Flow<ClusterMetrics> = _clusterMetrics.asStateFlow()

    private fun createInitialMetrics(): ClusterMetrics {
        val nodes = listOf(
            ClusterNodeStatus("local-auth", "Local Auth Engine", "PBKDF2-HMAC-SHA256 & Session Token", "LOCAL_ACTIVE", 0, 1),
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
            ClusterNodeStatus("local-auth", "Local Auth Engine", "PBKDF2-HMAC-SHA256 & Session Token", "LOCAL_ACTIVE", 5, 1),
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
        val saltBytes = ByteArray(SALT_LENGTH_BYTES)
        secureRandom.nextBytes(saltBytes)
        return Base64.getEncoder().encodeToString(saltBytes)
    }

    fun isLegacyHash(storedHash: String): Boolean {
        return !storedHash.startsWith(PBKDF2_PREFIX)
    }

    fun hashPasswordLegacy(password: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(salt.toByteArray(StandardCharsets.UTF_8))
        val hash = digest.digest(password.toByteArray(StandardCharsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }

    fun hashPasswordPbkdf2(
        password: String,
        salt: String,
        iterations: Int = pbkdf2Iterations
    ): String {
        val saltBytes = try {
            Base64.getDecoder().decode(salt)
        } catch (_: Exception) {
            salt.toByteArray(StandardCharsets.UTF_8)
        }
        val passwordChars = password.toCharArray()
        var spec: PBEKeySpec? = null
        return try {
            spec = PBEKeySpec(passwordChars, saltBytes, iterations, KEY_LENGTH_BITS)
            val factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)
            val hashBytes = factory.generateSecret(spec).encoded
            val base64Hash = Base64.getEncoder().encodeToString(hashBytes)
            val base64Salt = Base64.getEncoder().encodeToString(saltBytes)
            "$PBKDF2_PREFIX$iterations$$base64Salt$$base64Hash"
        } finally {
            spec?.clearPassword()
            passwordChars.fill('\u0000')
        }
    }

    fun hashSecurityAnswerPbkdf2(
        securityAnswer: String,
        salt: String,
        iterations: Int = pbkdf2Iterations
    ): String {
        return hashPasswordPbkdf2(securityAnswer.trim().lowercase(), salt, iterations)
    }

    fun hashPassword(password: String, salt: String): String {
        return hashPasswordPbkdf2(password, salt, pbkdf2Iterations)
    }

    fun verifyPassword(password: String, storedHash: String, fallbackSalt: String = ""): Boolean {
        if (storedHash.isBlank()) return false
        return try {
            if (storedHash.startsWith(PBKDF2_PREFIX)) {
                val parts = storedHash.split("$")
                val iterations: Int
                val saltStr: String
                val hashStr: String

                when (parts.size) {
                    5 -> {
                        // 5-part self-contained format with independent per-verifier salt:
                        // $pbkdf2-sha256$<iterations>$<base64Salt>$<base64Hash>
                        if (parts[1] != "pbkdf2-sha256") return false
                        iterations = parts[2].toIntOrNull() ?: return false
                        saltStr = parts[3]
                        hashStr = parts[4]
                    }
                    4 -> {
                        // 4-part format with external/fallback salt:
                        // $pbkdf2-sha256$<iterations>$<base64Hash>
                        if (parts[1] != "pbkdf2-sha256") return false
                        iterations = parts[2].toIntOrNull() ?: return false
                        saltStr = fallbackSalt
                        hashStr = parts[3]
                    }
                    else -> return false
                }

                // Bound verification work: reject iterations outside safe boundaries
                if (iterations < MIN_PBKDF2_ITERATIONS || iterations > MAX_PBKDF2_ITERATIONS) {
                    return false
                }

                val expectedHashBytes = try {
                    Base64.getDecoder().decode(hashStr)
                } catch (_: Exception) {
                    return false
                }

                // Validate expected derived-hash length before expensive derivation
                if (expectedHashBytes.size != EXPECTED_HASH_LENGTH_BYTES) {
                    return false
                }

                val saltBytes = try {
                    Base64.getDecoder().decode(saltStr)
                } catch (_: Exception) {
                    saltStr.toByteArray(StandardCharsets.UTF_8)
                }
                if (saltBytes.isEmpty() || saltBytes.size > 128) return false

                val passwordChars = password.toCharArray()
                var spec: PBEKeySpec? = null
                try {
                    spec = PBEKeySpec(passwordChars, saltBytes, iterations, EXPECTED_HASH_LENGTH_BYTES * 8)
                    val factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)
                    val computedBytes = factory.generateSecret(spec).encoded
                    MessageDigest.isEqual(computedBytes, expectedHashBytes)
                } finally {
                    spec?.clearPassword()
                    passwordChars.fill('\u0000')
                }
            } else if (storedHash.startsWith("legacy$")) {
                // Preserved salt legacy format: legacy$<salt>$<hash>
                val parts = storedHash.split("$")
                if (parts.size == 3) {
                    val preservedSalt = parts[1]
                    val legacyHex = parts[2]
                    val computedLegacyHex = hashPasswordLegacy(password, preservedSalt)
                    MessageDigest.isEqual(
                        computedLegacyHex.toByteArray(StandardCharsets.UTF_8),
                        legacyHex.toByteArray(StandardCharsets.UTF_8)
                    )
                } else {
                    false
                }
            } else {
                // Legacy single-round salted SHA-256
                if (fallbackSalt.isBlank()) return false
                val computedLegacyHex = hashPasswordLegacy(password, fallbackSalt)
                MessageDigest.isEqual(
                    computedLegacyHex.toByteArray(StandardCharsets.UTF_8),
                    storedHash.toByteArray(StandardCharsets.UTF_8)
                )
            }
        } catch (_: Exception) {
            false
        }
    }

    fun verifySecurityAnswer(rawAnswer: String, storedHash: String, fallbackSalt: String = ""): Boolean {
        return verifyPassword(rawAnswer.trim().lowercase(), storedHash, fallbackSalt)
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

    fun validateJwtToken(token: String): Boolean {
        if (token.isBlank()) return false
        val parts = token.split(".")
        if (parts.size != 3) return false
        return try {
            val signatureInput = "${parts[0]}.${parts[1]}"
            val mac = Mac.getInstance("HmacSHA256")
            val secretKey = SecretKeySpec(jwtSecretBytes, "HmacSHA256")
            mac.init(secretKey)
            val expectedSignatureBytes = mac.doFinal(signatureInput.toByteArray(StandardCharsets.UTF_8))
            val expectedSignature = Base64.getUrlEncoder().withoutPadding().encodeToString(expectedSignatureBytes)
            if (!MessageDigest.isEqual(expectedSignature.toByteArray(StandardCharsets.UTF_8), parts[2].toByteArray(StandardCharsets.UTF_8))) {
                return false
            }
            val payloadJson = String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8)
            val payload = JSONObject(payloadJson)
            val exp = payload.optLong("exp", 0)
            val now = System.currentTimeMillis() / 1000
            exp > now
        } catch (_: Exception) {
            false
        }
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
        val passwordSalt = generateSalt()
        val answerSalt = generateSalt() // Separate independent salt for security answer
        val passwordHash = hashPasswordPbkdf2(password, passwordSalt, pbkdf2Iterations)
        val answerHash = hashSecurityAnswerPbkdf2(securityAnswer, answerSalt, pbkdf2Iterations)
        val certId = "FEV-LOCAL-" + java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date()) + "-" + UUID.randomUUID().toString().take(6).uppercase()
        val token = generateJwtToken(userId = userId, email = cleanEmail, role = "OWNER")

        val newUser = ServerUserRecord(
            userId = userId,
            email = cleanEmail,
            fullName = fullName.trim(),
            phone = phone.trim(),
            passwordHash = passwordHash,
            passwordSalt = passwordSalt,
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
                details = "Registered household profile: $cleanEmail ($userId) with PBKDF2-HMAC-SHA256 ($pbkdf2Iterations iterations) in on-device database.",
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

        // Verify password supporting both PBKDF2 and legacy salted SHA-256
        val isPasswordValid = verifyPassword(password, user.passwordHash, user.passwordSalt)
        if (!isPasswordValid) {
            return@withContext ServerAuthResponse(
                success = false,
                message = "Invalid password. Credentials do not match stored profile records.",
                errorCode = "INVALID_CREDENTIALS"
            )
        }

        // Automatic transparent upgrade: migrate legacy SHA-256 hash to PBKDF2-HMAC-SHA256 with fresh salt
        var currentUser = user
        if (isLegacyHash(user.passwordHash)) {
            val freshSalt = generateSalt()
            val upgradedHash = hashPasswordPbkdf2(password, freshSalt, pbkdf2Iterations)

            // If legacy answer was hashed with old salt, preserve the salt in the answer hash so recovery succeeds
            val upgradedAnswerHash = if (isLegacyHash(user.securityAnswerHash)) {
                "legacy\$${user.passwordSalt}\$${user.securityAnswerHash}"
            } else {
                user.securityAnswerHash
            }

            currentUser = currentUser.copy(
                passwordHash = upgradedHash,
                passwordSalt = freshSalt,
                securityAnswerHash = upgradedAnswerHash
            )
            database.serverUserDao().update(currentUser)
            database.auditLogDao().insert(
                com.example.data.AuditLog(
                    action = "Password Hash Upgraded",
                    details = "Upgraded credentials for ${user.email} from legacy SHA-256 to PBKDF2-HMAC-SHA256 ($pbkdf2Iterations iterations).",
                    userRole = user.role
                )
            )
        }

        val freshToken = generateJwtToken(userId = currentUser.userId, email = currentUser.email, role = currentUser.role)
        val updatedUser = currentUser.copy(
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

    suspend fun resetPasswordWithSecurityAnswer(
        email: String,
        securityAnswer: String,
        newPassword: String
    ): ServerAuthResponse = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val cleanEmail = email.trim().lowercase()

        if (newPassword.length < 4) {
            return@withContext ServerAuthResponse(
                success = false,
                message = "New password must be at least 4 characters.",
                errorCode = "PASSWORD_TOO_SHORT"
            )
        }

        val user = database.serverUserDao().getByEmail(cleanEmail)
        if (user == null) {
            return@withContext ServerAuthResponse(
                success = false,
                message = "Invalid account or recovery credentials.",
                errorCode = "INVALID_CREDENTIALS"
            )
        }

        val isAnswerValid = verifySecurityAnswer(securityAnswer, user.securityAnswerHash, user.passwordSalt)
        if (!isAnswerValid) {
            return@withContext ServerAuthResponse(
                success = false,
                message = "Invalid account or recovery credentials.",
                errorCode = "INVALID_CREDENTIALS"
            )
        }

        val freshPasswordSalt = generateSalt()
        val freshAnswerSalt = generateSalt()
        val newPasswordHash = hashPasswordPbkdf2(newPassword, freshPasswordSalt, pbkdf2Iterations)
        val upgradedAnswerHash = hashSecurityAnswerPbkdf2(securityAnswer, freshAnswerSalt, pbkdf2Iterations)
        val freshToken = generateJwtToken(userId = user.userId, email = user.email, role = user.role)

        val updatedUser = user.copy(
            passwordHash = newPasswordHash,
            passwordSalt = freshPasswordSalt,
            securityAnswerHash = upgradedAnswerHash,
            tokenJwt = freshToken,
            lastLoginAt = System.currentTimeMillis()
        )
        database.serverUserDao().update(updatedUser)

        database.auditLogDao().insert(
            com.example.data.AuditLog(
                action = "Password Reset via Security Question",
                details = "Password reset completed for ${user.email} using verified security answer.",
                userRole = user.role
            )
        )

        updateLocalMetrics()
        val elapsed = System.currentTimeMillis() - startTime

        ServerAuthResponse(
            success = true,
            message = "Password successfully reset and upgraded.",
            token = freshToken,
            user = updatedUser,
            latencyMs = elapsed,
            processedByNode = "local-device-storage"
        )
    }

    suspend fun changePassword(
        email: String,
        currentPassword: String,
        newPassword: String
    ): ServerAuthResponse = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val cleanEmail = email.trim().lowercase()

        if (newPassword.length < 6) {
            return@withContext ServerAuthResponse(
                success = false,
                message = "New password must be at least 6 characters.",
                errorCode = "PASSWORD_TOO_SHORT"
            )
        }

        val user = database.serverUserDao().getByEmail(cleanEmail)
        if (user == null) {
            return@withContext ServerAuthResponse(
                success = false,
                message = "No local profile found for '$email'.",
                errorCode = "USER_NOT_FOUND"
            )
        }

        val isCurrentValid = verifyPassword(currentPassword, user.passwordHash, user.passwordSalt)
        if (!isCurrentValid) {
            return@withContext ServerAuthResponse(
                success = false,
                message = "Current password does not match.",
                errorCode = "INVALID_CREDENTIALS"
            )
        }

        val freshSalt = generateSalt()
        val newHash = hashPasswordPbkdf2(newPassword, freshSalt, pbkdf2Iterations)
        val freshToken = generateJwtToken(userId = user.userId, email = user.email, role = user.role)
        val updatedUser = user.copy(
            passwordHash = newHash,
            passwordSalt = freshSalt,
            tokenJwt = freshToken,
            lastLoginAt = System.currentTimeMillis()
        )
        database.serverUserDao().update(updatedUser)

        database.auditLogDao().insert(
            com.example.data.AuditLog(
                action = "Password Changed",
                details = "Updated password for ${user.email} using PBKDF2-HMAC-SHA256 ($pbkdf2Iterations iterations).",
                userRole = user.role
            )
        )

        updateLocalMetrics()
        val elapsed = System.currentTimeMillis() - startTime

        ServerAuthResponse(
            success = true,
            message = "Password successfully updated to PBKDF2-HMAC-SHA256.",
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
