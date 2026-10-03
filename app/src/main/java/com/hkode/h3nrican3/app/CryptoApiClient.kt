package com.hkode.h3nrican3.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * High-performance 100% offline crypto engine client.
 * Executes AES-256-GCM / PBKDF2 natively inside the app without requiring an internet connection or server.
 */
object CryptoApiClient {

    enum class Mode(val value: String) {
        SYSTEM("system"),
        PASSWORD("password")
    }

    enum class Action(val value: String) {
        ENCRYPT("encrypt"),
        DECRYPT("decrypt")
    }

    suspend fun encrypt(
        message: String,
        mode: Mode,
        password: String? = null
    ): Result<String> = withContext(Dispatchers.Default) {
        if (message.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Please enter a message to encrypt."))
        }
        if (mode == Mode.PASSWORD && password.isNullOrBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Please enter a password for protection."))
        }

        try {
            val result = if (mode == Mode.PASSWORD) {
                NativeCryptoEngine.encryptPassword(message, password!!)
            } else {
                NativeCryptoEngine.encryptSystem(message)
            }
            Result.success(result)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(Exception(e.message ?: "Encryption failed."))
        }
    }

    suspend fun decrypt(
        ciphertext: String,
        mode: Mode,
        password: String? = null
    ): Result<String> = withContext(Dispatchers.Default) {
        val trimmed = ciphertext.trim()
        if (trimmed.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Please enter or paste the encrypted message."))
        }
        if (mode == Mode.PASSWORD && password.isNullOrBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Please enter the password used to encrypt."))
        }

        try {
            val plaintext = if (mode == Mode.PASSWORD) {
                NativeCryptoEngine.decryptPassword(trimmed, password!!)
            } else {
                NativeCryptoEngine.decryptSystem(trimmed)
            }
            Result.success(plaintext)
        } catch (e: Exception) {
            e.printStackTrace()
            val friendlyMsg = if (mode == Mode.PASSWORD) {
                "Decryption failed — Incorrect password or corrupt encrypted message."
            } else {
                "Decryption failed — Invalid encrypted message."
            }
            Result.failure(Exception(friendlyMsg))
        }
    }
}
