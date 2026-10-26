package com.example.crypto

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object RecoveryKeyManager {
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val RECOVERY_STORAGE_KEY_ALIAS = "adsky_vault_recovery_storage_key"
    private const val PREFS_NAME = "adsky_vault_recovery_prefs"
    private const val KEY_ENCRYPTED_SECRET = "encrypted_recovery_secret"
    private const val KEY_RECOVERY_IV = "recovery_iv"

    private const val CROCKFORD_ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ"

    private fun getOrCreateStorageKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        if (!keyStore.containsAlias(RECOVERY_STORAGE_KEY_ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEYSTORE
            )
            val builder = KeyGenParameterSpec.Builder(
                RECOVERY_STORAGE_KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setKeySize(256)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)

            keyGenerator.init(builder.build())
            keyGenerator.generateKey()
        }
        val entry = keyStore.getEntry(RECOVERY_STORAGE_KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
            ?: error("Missing recovery storage key entry")
        return entry.secretKey
    }

    /**
     * Initializes or returns the existing device installation recovery secret.
     * The recovery secret is 32 bytes (256 bits) of cryptographic entropy,
     * encrypted at rest using Android Keystore.
     */
    @Synchronized
    fun getOrCreateInstallationRecoverySecret(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val encryptedBase64 = prefs.getString(KEY_ENCRYPTED_SECRET, null)
        val ivBase64 = prefs.getString(KEY_RECOVERY_IV, null)

        if (encryptedBase64 != null && ivBase64 != null) {
            try {
                val encrypted = Base64.decode(encryptedBase64, Base64.NO_WRAP)
                val iv = Base64.decode(ivBase64, Base64.NO_WRAP)
                val key = getOrCreateStorageKey()
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
                val rawBytes = cipher.doFinal(encrypted)
                return bytesToFormattedSecret(rawBytes)
            } catch (e: Exception) {
                // If corrupted or Keystore wiped, generate a fresh secret
            }
        }

        // Generate a new 256-bit cryptographically secure secret
        val rawSecretBytes = ByteArray(32)
        SecureRandom().nextBytes(rawSecretBytes)

        val key = getOrCreateStorageKey()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val encrypted = cipher.doFinal(rawSecretBytes)
        val iv = cipher.iv

        prefs.edit()
            .putString(KEY_ENCRYPTED_SECRET, Base64.encodeToString(encrypted, Base64.NO_WRAP))
            .putString(KEY_RECOVERY_IV, Base64.encodeToString(iv, Base64.NO_WRAP))
            .apply()

        return bytesToFormattedSecret(rawSecretBytes)
    }

    /**
     * Converts 32 raw secret bytes to formatted string:
     * ADSKY-XXXX-XXXX-XXXX-XXXX-XXXX-XXXX-XXXX-XXXX
     */
    fun bytesToFormattedSecret(bytes: ByteArray): String {
        val hex = bytes.joinToString("") { "%02X".format(it) }
        val chunks = hex.chunked(8)
        return "ADSKY-" + chunks.joinToString("-")
    }

    /**
     * Parses a user-entered recovery secret string into 32 raw bytes.
     * Accepts formats with or without "ADSKY-" prefix, dashes, spaces, or lower/upper case.
     */
    fun parseSecretToBytes(secretString: String): ByteArray {
        var clean = secretString.trim().uppercase()
        if (clean.startsWith("ADSKY-")) {
            clean = clean.removePrefix("ADSKY-")
        } else if (clean.startsWith("ADSKY")) {
            clean = clean.removePrefix("ADSKY")
        }
        clean = clean.replace("-", "").replace(" ", "")

        require(clean.length == 64) {
            "Invalid recovery secret length: expected 64 hexadecimal characters, got ${clean.length}"
        }
        require(clean.all { it in "0123456789ABCDEF" }) {
            "Invalid characters in recovery secret. Must contain only hex digits (0-9, A-F)."
        }

        val result = ByteArray(32)
        for (i in 0 until 32) {
            val byteStr = clean.substring(i * 2, i * 2 + 2)
            result[i] = byteStr.toInt(16).toByte()
        }
        return result
    }

    /**
     * Derives a dedicated recovery wrapping key using Argon2id with file-specific salt.
     */
    fun deriveRecoveryWrappingKey(
        secretBytes: ByteArray,
        salt: ByteArray,
        iterations: Int = Argon2Kdf.DEFAULT_ITERATIONS,
        memoryKb: Int = Argon2Kdf.DEFAULT_MEMORY_KB,
        parallelism: Int = Argon2Kdf.DEFAULT_PARALLELISM
    ): ByteArray {
        return Argon2Kdf.deriveKeyFromBytes(secretBytes, salt, iterations, memoryKb, parallelism)
    }
}
