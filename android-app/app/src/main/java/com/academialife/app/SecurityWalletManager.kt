package com.academialife.app

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

object SecurityWalletManager {

    private const val PREFS_WALLET = "mybuddy_secure_wallet_prefs"
    private var isSessionUnlocked: Boolean = false
    private var sessionUnlockTimestamp: Long = 0L
    private const val SESSION_TIMEOUT_MS = 60_000L // 60 seconds auto-lock

    private fun getSecurePrefs(context: Context): SharedPreferences {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)

        return EncryptedSharedPreferences.create(
            PREFS_WALLET,
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun isUnlocked(): Boolean {
        if (!isSessionUnlocked) return false
        if (System.currentTimeMillis() - sessionUnlockTimestamp > SESSION_TIMEOUT_MS) {
            isSessionUnlocked = false
            return false
        }
        return true
    }

    fun lockWallet() {
        isSessionUnlocked = false
        sessionUnlockTimestamp = 0L
    }

    fun canAuthenticate(context: Context): Boolean {
        val biometricManager = BiometricManager.from(context)
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        return biometricManager.canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun authenticate(
        activity: AppCompatActivity,
        title: String = "Unlock Secure Emergency Wallet",
        subtitle: String = "Verify biometric or device credential to reveal sensitive documents",
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val prompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                isSessionUnlocked = true
                sessionUnlockTimestamp = System.currentTimeMillis()
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                if (errorCode == BiometricPrompt.ERROR_USER_CANCELED || errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                    onError("Verification cancelled")
                } else {
                    onError(errString.toString())
                }
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                onError("Biometric verification failed. Please try again.")
            }
        })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
            .build()

        prompt.authenticate(promptInfo)
    }

    fun saveEncryptedDocument(context: Context, doc: EmergencyDocument): Boolean {
        return try {
            val prefs = getSecurePrefs(context)
            prefs.edit()
                .putString("doc_${doc.id}_title", doc.title)
                .putString("doc_${doc.id}_type", doc.documentType)
                .putString("doc_${doc.id}_number", doc.idNumber)
                .putString("doc_${doc.id}_holder", doc.holderName)
                .putString("doc_${doc.id}_notes", doc.notes)
                .putString("doc_${doc.id}_issue", doc.issueDate)
                .putString("doc_${doc.id}_expiry", doc.expiryDate)
                .putString("doc_${doc.id}_path", doc.fileUriOrPath)
                .putLong("doc_${doc.id}_time", System.currentTimeMillis())
                .apply()
            true
        } catch (e: Exception) {
            android.util.Log.e("SecurityWalletManager", "Encryption save error: ${e.message}")
            false
        }
    }

    fun getEncryptedDocument(context: Context, docId: String): EmergencyDocument? {
        if (!isUnlocked()) return null
        return try {
            val prefs = getSecurePrefs(context)
            val title = prefs.getString("doc_${docId}_title", null) ?: return null
            EmergencyDocument(
                id = docId,
                title = title,
                documentType = prefs.getString("doc_${docId}_type", "college_id") ?: "college_id",
                idNumber = prefs.getString("doc_${docId}_number", "") ?: "",
                holderName = prefs.getString("doc_${docId}_holder", "") ?: "",
                notes = prefs.getString("doc_${docId}_notes", "") ?: "",
                issueDate = prefs.getString("doc_${docId}_issue", "") ?: "",
                expiryDate = prefs.getString("doc_${docId}_expiry", "") ?: "",
                fileUriOrPath = prefs.getString("doc_${docId}_path", "") ?: "",
                isEncrypted = true,
                lastVerifiedAt = prefs.getLong("doc_${docId}_time", 0L)
            )
        } catch (_: Exception) {
            null
        }
    }

    fun maskIdNumber(rawId: String): String {
        if (rawId.length <= 4) return "••••"
        return "•••• •••• " + rawId.takeLast(4)
    }
}

