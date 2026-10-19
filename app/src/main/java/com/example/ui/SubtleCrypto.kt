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
}
