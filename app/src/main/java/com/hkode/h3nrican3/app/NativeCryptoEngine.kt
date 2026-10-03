package com.hkode.h3nrican3.app

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * 100% Offline Native Encryption/Decryption Engine.
 * Replicates the PHP AES-256-GCM / PBKDF2-SHA256 implementation natively in Kotlin.
 * Works without network connection, zero latency, maximum security.
 */
object NativeCryptoEngine {

    private const val SYSTEM_SECRET_KEY = "CHANGE-THIS-TO-A-LONG-RANDOM-SECRET-STRING-0000"
    private const val STATIC_SYSTEM_SALT = "static-system-salt-v1"
    private const val PBKDF2_ITERATIONS = 100000
    private const val KEY_LENGTH_BITS = 256
    private const val SALT_LENGTH_BYTES = 16
    private const val IV_LENGTH_BYTES = 12
    private const val TAG_LENGTH_BITS = 128

    private val secureRandom = SecureRandom()

    private fun deriveKey(password: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return factory.generateSecret(spec).encoded
    }

    private fun getSystemKey(): ByteArray {
        return deriveKey(SYSTEM_SECRET_KEY, STATIC_SYSTEM_SALT.toByteArray(Charsets.UTF_8))
    }

    fun encryptPassword(plaintext: String, password: String): String {
        val salt = ByteArray(SALT_LENGTH_BYTES)
        secureRandom.nextBytes(salt)

        val keyBytes = deriveKey(password, salt)
        val secretKey = SecretKeySpec(keyBytes, "AES")

        val iv = ByteArray(IV_LENGTH_BYTES)
        secureRandom.nextBytes(iv)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val gcmSpec = GCMParameterSpec(TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec)

        val cipherOutput = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))

        val packaged = ByteArray(salt.size + iv.size + cipherOutput.size)
        System.arraycopy(salt, 0, packaged, 0, salt.size)
        System.arraycopy(iv, 0, packaged, salt.size, iv.size)
        System.arraycopy(cipherOutput, 0, packaged, salt.size + iv.size, cipherOutput.size)

        return Base64.encodeToString(packaged, Base64.NO_WRAP)
    }

    fun decryptPassword(packagedBase64: String, password: String): String {
        val raw = Base64.decode(packagedBase64.trim(), Base64.DEFAULT)
        val minLen = SALT_LENGTH_BYTES + IV_LENGTH_BYTES + (TAG_LENGTH_BITS / 8)
        if (raw.size <= minLen) {
            throw IllegalArgumentException("Invalid encrypted message format.")
        }

        val salt = raw.copyOfRange(0, SALT_LENGTH_BYTES)
        val iv = raw.copyOfRange(SALT_LENGTH_BYTES, SALT_LENGTH_BYTES + IV_LENGTH_BYTES)
        val cipherOutput = raw.copyOfRange(SALT_LENGTH_BYTES + IV_LENGTH_BYTES, raw.size)

        val keyBytes = deriveKey(password, salt)
        val secretKey = SecretKeySpec(keyBytes, "AES")

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val gcmSpec = GCMParameterSpec(TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)

        val decryptedBytes = cipher.doFinal(cipherOutput)
        return String(decryptedBytes, Charsets.UTF_8)
    }

    fun encryptSystem(plaintext: String): String {
        val keyBytes = getSystemKey()
        val secretKey = SecretKeySpec(keyBytes, "AES")

        val iv = ByteArray(IV_LENGTH_BYTES)
        secureRandom.nextBytes(iv)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val gcmSpec = GCMParameterSpec(TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec)

        val cipherOutput = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))

        val packaged = ByteArray(iv.size + cipherOutput.size)
        System.arraycopy(iv, 0, packaged, 0, iv.size)
        System.arraycopy(cipherOutput, 0, packaged, iv.size, cipherOutput.size)

        return Base64.encodeToString(packaged, Base64.NO_WRAP)
    }

    fun decryptSystem(packagedBase64: String): String {
        val raw = Base64.decode(packagedBase64.trim(), Base64.DEFAULT)
        val minLen = IV_LENGTH_BYTES + (TAG_LENGTH_BITS / 8)
        if (raw.size <= minLen) {
            throw IllegalArgumentException("Invalid encrypted message format.")
        }

        val iv = raw.copyOfRange(0, IV_LENGTH_BYTES)
        val cipherOutput = raw.copyOfRange(IV_LENGTH_BYTES, raw.size)

        val keyBytes = getSystemKey()
        val secretKey = SecretKeySpec(keyBytes, "AES")

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val gcmSpec = GCMParameterSpec(TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)

        val decryptedBytes = cipher.doFinal(cipherOutput)
        return String(decryptedBytes, Charsets.UTF_8)
    }
}
