package com.hkode.h3nrican3.app

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Base64
import androidx.biometric.BiometricManager
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object AppSecurityManager {

    private const val PREFS_NAME = "hkode_security_prefs"
    private const val KEY_SALT = "passcode_salt"
    private const val KEY_HASH = "passcode_hash"
    private const val KEY_PASSCODE_ENABLED = "passcode_enabled"
    private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
    private const val KEY_FIRST_TIME_DECIDED = "first_time_decided"
    private const val KEY_LOCK_TIMEOUT_SECONDS = "lock_timeout_seconds"
    private const val KEY_SECURITY_Q1 = "security_q1"
    private const val KEY_SECURITY_A1 = "security_a1_hash"
    private const val KEY_SECURITY_Q2 = "security_q2"
    private const val KEY_SECURITY_A2 = "security_a2_hash"

    val DEFAULT_SECURITY_QUESTIONS = listOf(
        "What was the name of your first school?",
        "What city were you born in?",
        "What was the name of your first pet?",
        "What is your mother's maiden name?",
        "What was your favorite subject in school?",
        "What was the name of your favorite childhood teacher?",
        "What was the model of your first phone or car?",
        "What is your favorite book or movie?"
    )

    const val TIMEOUT_IMMEDIATELY = 0
    const val TIMEOUT_60_SECONDS = 60

    private const val PBKDF2_ITERATIONS = 10000
    private const val KEY_LENGTH_BITS = 256
    private const val SALT_BYTES = 16

    // Security timeout: 30 seconds in background triggers app lock
    private const val LOCK_TIMEOUT_MS = 30_000L

    private var isAppLocked: Boolean = true
    private var lastBackgroundTime: Long = 0L
    private val secureRandom = SecureRandom()

    enum class BiometricStatus {
        AVAILABLE,
        NO_HARDWARE,
        NOT_ENROLLED,
        UNAVAILABLE
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun isPasscodeSet(context: Context): Boolean {
        val prefs = getPrefs(context)
        return prefs.contains(KEY_SALT) && prefs.contains(KEY_HASH)
    }

    fun isPasscodeEnabled(context: Context): Boolean {
        val prefs = getPrefs(context)
        return isPasscodeSet(context) && prefs.getBoolean(KEY_PASSCODE_ENABLED, false)
    }

    fun setPasscode(context: Context, pin: String): Boolean {
        if (pin.length != 4 || !pin.all { it.isDigit() }) return false

        try {
            val salt = ByteArray(SALT_BYTES)
            secureRandom.nextBytes(salt)

            val hash = hashPin(pin, salt)

            val saltBase64 = Base64.encodeToString(salt, Base64.NO_WRAP)
            val hashBase64 = Base64.encodeToString(hash, Base64.NO_WRAP)

            getPrefs(context).edit()
                .putString(KEY_SALT, saltBase64)
                .putString(KEY_HASH, hashBase64)
                .putBoolean(KEY_PASSCODE_ENABLED, true)
                .putBoolean(KEY_FIRST_TIME_DECIDED, true)
                .apply()

            isAppLocked = false
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    fun verifyPasscode(context: Context, pin: String): Boolean {
        if (pin.length != 4) return false
        val prefs = getPrefs(context)
        val saltBase64 = prefs.getString(KEY_SALT, null) ?: return false
        val hashBase64 = prefs.getString(KEY_HASH, null) ?: return false

        try {
            val salt = Base64.decode(saltBase64, Base64.DEFAULT)
            val expectedHash = Base64.decode(hashBase64, Base64.DEFAULT)
            val computedHash = hashPin(pin, salt)

            return MessageDigest.isEqual(expectedHash, computedHash)
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    fun disablePasscode(context: Context): Boolean {
        getPrefs(context).edit()
            .putBoolean(KEY_PASSCODE_ENABLED, false)
            .putBoolean(KEY_BIOMETRIC_ENABLED, false)
            .apply()
        isAppLocked = false
        return true
    }

    fun clearPasscode(context: Context): Boolean {
        getPrefs(context).edit()
            .remove(KEY_SALT)
            .remove(KEY_HASH)
            .remove(KEY_SECURITY_Q1)
            .remove(KEY_SECURITY_A1)
            .remove(KEY_SECURITY_Q2)
            .remove(KEY_SECURITY_A2)
            .putBoolean(KEY_PASSCODE_ENABLED, false)
            .putBoolean(KEY_BIOMETRIC_ENABLED, false)
            .apply()
        isAppLocked = false
        return true
    }

    fun isBiometricEnabled(context: Context): Boolean {
        return isPasscodeEnabled(context) &&
                getPrefs(context).getBoolean(KEY_BIOMETRIC_ENABLED, false) &&
                checkBiometricStatus(context) == BiometricStatus.AVAILABLE
    }

    fun setBiometricEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit()
            .putBoolean(KEY_BIOMETRIC_ENABLED, enabled)
            .apply()
    }

    fun checkBiometricStatus(context: Context): BiometricStatus {
        return try {
            val biometricManager = BiometricManager.from(context)
            val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK
            when (biometricManager.canAuthenticate(authenticators)) {
                BiometricManager.BIOMETRIC_SUCCESS -> BiometricStatus.AVAILABLE
                BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricStatus.NO_HARDWARE
                BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricStatus.NOT_ENROLLED
                else -> BiometricStatus.UNAVAILABLE
            }
        } catch (t: Throwable) {
            BiometricStatus.UNAVAILABLE
        }
    }

    fun hasUserDecidedFirstTime(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_FIRST_TIME_DECIDED, false)
    }

    fun setUserDecidedFirstTime(context: Context, decided: Boolean = true) {
        getPrefs(context).edit().putBoolean(KEY_FIRST_TIME_DECIDED, decided).apply()
    }

    fun hasSecurityQuestions(context: Context): Boolean {
        val prefs = getPrefs(context)
        return prefs.contains(KEY_SECURITY_Q1) &&
                prefs.contains(KEY_SECURITY_A1) &&
                prefs.contains(KEY_SECURITY_Q2) &&
                prefs.contains(KEY_SECURITY_A2)
    }

    fun getSecurityQuestions(context: Context): Pair<String, String>? {
        val prefs = getPrefs(context)
        val q1 = prefs.getString(KEY_SECURITY_Q1, null)
        val q2 = prefs.getString(KEY_SECURITY_Q2, null)
        return if (!q1.isNullOrEmpty() && !q2.isNullOrEmpty()) {
            Pair(q1, q2)
        } else {
            null
        }
    }

    private fun normalizeAnswer(answer: String): String {
        return answer.trim().lowercase()
    }

    private fun hashAnswer(answer: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(normalizeAnswer(answer).toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(hash, Base64.NO_WRAP)
    }

    fun saveSecurityQuestions(
        context: Context,
        q1: String,
        a1: String,
        q2: String,
        a2: String
    ): Boolean {
        if (q1.isBlank() || a1.isBlank() || q2.isBlank() || a2.isBlank()) return false
        val a1Hash = hashAnswer(a1)
        val a2Hash = hashAnswer(a2)
        getPrefs(context).edit()
            .putString(KEY_SECURITY_Q1, q1.trim())
            .putString(KEY_SECURITY_A1, a1Hash)
            .putString(KEY_SECURITY_Q2, q2.trim())
            .putString(KEY_SECURITY_A2, a2Hash)
            .apply()
        return true
    }

    fun verifySecurityAnswers(context: Context, a1: String, a2: String): Boolean {
        val prefs = getPrefs(context)
        val expectedA1Hash = prefs.getString(KEY_SECURITY_A1, null) ?: return false
        val expectedA2Hash = prefs.getString(KEY_SECURITY_A2, null) ?: return false
        val inputA1Hash = hashAnswer(a1)
        val inputA2Hash = hashAnswer(a2)
        return expectedA1Hash == inputA1Hash && expectedA2Hash == inputA2Hash
    }

    // Lifecycle Lock Management
    fun isLocked(context: Context): Boolean {
        return isPasscodeEnabled(context) && isAppLocked
    }

    fun unlock() {
        isAppLocked = false
        lastBackgroundTime = 0L
    }

    fun lock() {
        isAppLocked = true
    }

    fun onAppBackgrounded() {
        lastBackgroundTime = System.currentTimeMillis()
    }

    fun getLockTimeoutSeconds(context: Context): Int {
        return getPrefs(context).getInt(KEY_LOCK_TIMEOUT_SECONDS, TIMEOUT_IMMEDIATELY)
    }

    fun setLockTimeoutSeconds(context: Context, seconds: Int) {
        getPrefs(context).edit().putInt(KEY_LOCK_TIMEOUT_SECONDS, seconds).apply()
    }

    fun onAppForegrounded(context: Context) {
        if (!isPasscodeEnabled(context)) {
            isAppLocked = false
            return
        }

        val timeoutSec = getLockTimeoutSeconds(context)
        if (timeoutSec == TIMEOUT_IMMEDIATELY) {
            // Lock immediately when leaving app and returning
            if (lastBackgroundTime > 0L) {
                isAppLocked = true
            }
        } else {
            val timeoutMs = timeoutSec * 1000L
            val elapsed = System.currentTimeMillis() - lastBackgroundTime
            if (lastBackgroundTime > 0L && elapsed >= timeoutMs) {
                isAppLocked = true
            }
        }
    }

    fun shouldEnforceLock(activity: Activity): Boolean {
        if (activity is PasscodeLockActivity) return false
        return isLocked(activity)
    }

    fun launchLockScreen(activity: Activity, targetIntent: Intent? = null) {
        val intent = Intent(activity, PasscodeLockActivity::class.java).apply {
            putExtra("EXTRA_MODE", PasscodeLockActivity.MODE_UNLOCK)
            if (targetIntent != null) {
                putExtra("EXTRA_TARGET_INTENT", targetIntent)
            }
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        activity.startActivity(intent)
    }

    private fun hashPin(pin: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return factory.generateSecret(spec).encoded
    }
}
