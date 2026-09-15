package com.example.util

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

enum class BiometricAvailability(val userTitleEn: String, val userTitleHi: String, val canPrompt: Boolean) {
    AVAILABLE("Fingerprint / Face Unlock Ready", "बायोमेट्रिक (फिंगरप्रिंट / फेस) अनलॉक तैयार है", true),
    NOT_ENROLLED("Biometrics Not Enrolled in Device Settings", "डिवाइस सेटिंग्स में बायोमेट्रिक दर्ज नहीं है", false),
    NO_HARDWARE("No Biometric Sensor Found on Device", "डिवाइस में बायोमेट्रिक सेंसर नहीं मिला", false),
    HW_UNAVAILABLE("Biometric Sensor Temporarily Busy/Unavailable", "बायोमेट्रिक सेंसर अस्थायी रूप से अनुपलब्ध है", false),
    SECURITY_UPDATE_REQUIRED("Biometric Security Update Required", "बायोमेट्रिक सुरक्षा अपडेट की आवश्यकता है", false),
    UNSUPPORTED("Biometric Authentication Unsupported", "बायोमेट्रिक प्रमाणीकरण असमर्थित है", false);

    val userTitle: String get() = userTitleEn

    fun localizedTitle(isHindi: Boolean): String = if (isHindi) userTitleHi else userTitleEn
}

object BiometricAuthManager {

    fun checkBiometricAvailability(context: Context): BiometricAvailability {
        return try {
            val biometricManager = BiometricManager.from(context)
            val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK
            when (biometricManager.canAuthenticate(authenticators)) {
                BiometricManager.BIOMETRIC_SUCCESS -> BiometricAvailability.AVAILABLE
                BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricAvailability.NOT_ENROLLED
                BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricAvailability.NO_HARDWARE
                BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricAvailability.HW_UNAVAILABLE
                BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> BiometricAvailability.SECURITY_UPDATE_REQUIRED
                else -> BiometricAvailability.UNSUPPORTED
            }
        } catch (e: Throwable) {
            BiometricAvailability.UNSUPPORTED
        }
    }

    fun promptBiometric(
        activity: FragmentActivity,
        title: String = "Biometric Vault Authentication",
        subtitle: String = "Scan fingerprint or face to decrypt sensitive data",
        description: String = "Military-grade on-device cryptographic protection for family assets.",
        negativeButtonText: String = "Use Master MPIN",
        onSuccess: (BiometricPrompt.AuthenticationResult) -> Unit,
        onError: (errorCode: Int, errString: CharSequence) -> Unit,
        onFailed: () -> Unit
    ) {
        if (activity.isFinishing || activity.isDestroyed) {
            onError(-1, "Activity is finishing or destroyed")
            return
        }
        try {
            val executor = ContextCompat.getMainExecutor(activity)
            val callback = object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess(result)
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    onError(errorCode, errString)
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    onFailed()
                }
            }

            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setDescription(description)
                .setNegativeButtonText(negativeButtonText)
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK)
                .build()

            val biometricPrompt = BiometricPrompt(activity, executor, callback)
            biometricPrompt.authenticate(promptInfo)
        } catch (e: Throwable) {
            android.util.Log.e("BiometricAuthManager", "Error executing biometric prompt", e)
            onError(-99, e.message ?: "Biometric prompt error")
        }
    }
}
