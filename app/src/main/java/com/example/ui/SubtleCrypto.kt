package com.example.ui

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Android KeyStore-backed secure AES-256-GCM encryption engine.
 * Eliminates static/hardcoded keys by generating and maintaining hardware-backed
 * cryptographic keys inside the Android KeyStore enclave.
 */
object SubtleCrypto {
    private const val ALGORITHM = "AES/GCM/NoPadding"
    private const val TAG_LENGTH_BIT = 128
    private const val IV_LENGTH_BYTE = 12
    private const val KEYSTORE_ALIAS = "fev_vault_hardware_key"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"

    @Volatile
    private var ephemeralTestKey: SecretKey? = null

    private fun getSecretKey(): SecretKey {
        return try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            if (keyStore.containsAlias(KEYSTORE_ALIAS)) {
                val entry = keyStore.getEntry(KEYSTORE_ALIAS, null) as? KeyStore.SecretKeyEntry
                entry?.secretKey ?: generateKeystoreKey()
            } else {
                generateKeystoreKey()
            }
        } catch (e: Exception) {
            // Secure in-memory dynamic fallback for JVM/Robolectric test environments
            // without AndroidKeyStore SPI. Uses SecureRandom 256-bit key instead of hardcoded bytes.
            ephemeralTestKey ?: synchronized(this) {
                ephemeralTestKey ?: run {
                    val randomBytes = ByteArray(32)
                    SecureRandom().nextBytes(randomBytes)
                    SecretKeySpec(randomBytes, "AES").also { ephemeralTestKey = it }
                }
            }
        }
    }

    private fun generateKeystoreKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            KEYSTORE_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setRandomizedEncryptionRequired(true)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    fun encrypt(plainText: String): String {
        return try {
            val cipher = Cipher.getInstance(ALGORITHM)
            val iv = ByteArray(IV_LENGTH_BYTE)
            SecureRandom().nextBytes(iv)
            val spec = GCMParameterSpec(TAG_LENGTH_BIT, iv)
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey(), spec)

            val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

            // Format: Base64(IV) + ":" + Base64(CipherText)
            val ivBase64 = Base64.encodeToString(iv, Base64.NO_WRAP)
            val cipherBase64 = Base64.encodeToString(cipherText, Base64.NO_WRAP)
            "$ivBase64:$cipherBase64"
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    fun decrypt(encryptedData: String): String {
        if (encryptedData.isEmpty() || encryptedData == "[]") return encryptedData
        return try {
            if (!encryptedData.contains(":")) {
                // Return as-is if unencrypted legacy payload
                return encryptedData
            }
            val parts = encryptedData.split(":")
            if (parts.size != 2) return ""

            val iv = Base64.decode(parts[0], Base64.NO_WRAP)
            val cipherText = Base64.decode(parts[1], Base64.NO_WRAP)

            val cipher = Cipher.getInstance(ALGORITHM)
            val spec = GCMParameterSpec(TAG_LENGTH_BIT, iv)
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec)

            val decryptedBytes = cipher.doFinal(cipherText)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    val FILE_MAGIC = byteArrayOf(0x46, 0x45, 0x56, 0x5F, 0x45, 0x4E, 0x43, 0x31) // "FEV_ENC1"

    /**
     * Authenticated AES-256-GCM binary encryption for private documents.
     * Prepends an 8-byte magic header ("FEV_ENC1") + 12-byte random IV + ciphertext + 16-byte GCM tag.
     */
    fun encryptBytes(plainBytes: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(ALGORITHM)
        val iv = ByteArray(IV_LENGTH_BYTE)
        SecureRandom().nextBytes(iv)
        val spec = GCMParameterSpec(TAG_LENGTH_BIT, iv)
        cipher.init(Cipher.ENCRYPT_MODE, getSecretKey(), spec)
        val cipherBytes = cipher.doFinal(plainBytes)

        val result = ByteArray(FILE_MAGIC.size + iv.size + cipherBytes.size)
        System.arraycopy(FILE_MAGIC, 0, result, 0, FILE_MAGIC.size)
        System.arraycopy(iv, 0, result, FILE_MAGIC.size, iv.size)
        System.arraycopy(cipherBytes, 0, result, FILE_MAGIC.size + iv.size, cipherBytes.size)
        return result
    }

    /**
     * Checks whether raw file bytes contain the authenticated encryption magic header.
     */
    fun isEncryptedFile(bytes: ByteArray): Boolean {
        if (bytes.size < FILE_MAGIC.size) return false
        for (i in FILE_MAGIC.indices) {
            if (bytes[i] != FILE_MAGIC[i]) return false
        }
        return true
    }

    /**
     * Decrypts authenticated AES-256-GCM document bytes.
     * If the bytes do not start with the FEV_ENC1 magic header, returns the raw bytes as-is
     * to preserve existing documents during migration.
     * Throws an exception or returns null if corruption/tampering occurs.
     */
    fun decryptBytes(encryptedBytes: ByteArray): ByteArray {
        if (!isEncryptedFile(encryptedBytes)) {
            // Unencrypted legacy file - preserve as-is for backward compatibility
            return encryptedBytes
        }
        val iv = ByteArray(IV_LENGTH_BYTE)
        System.arraycopy(encryptedBytes, FILE_MAGIC.size, iv, 0, IV_LENGTH_BYTE)
        val cipherLength = encryptedBytes.size - FILE_MAGIC.size - IV_LENGTH_BYTE
        val cipherBytes = ByteArray(cipherLength)
        System.arraycopy(encryptedBytes, FILE_MAGIC.size + IV_LENGTH_BYTE, cipherBytes, 0, cipherLength)

        val cipher = Cipher.getInstance(ALGORITHM)
        val spec = GCMParameterSpec(TAG_LENGTH_BIT, iv)
        cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec)
        return cipher.doFinal(cipherBytes)
    }
}
