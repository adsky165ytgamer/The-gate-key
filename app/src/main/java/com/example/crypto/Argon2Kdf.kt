package com.example.crypto

import org.bouncycastle.crypto.generators.Argon2BytesGenerator
import org.bouncycastle.crypto.params.Argon2Parameters
import java.security.SecureRandom
import java.util.Arrays

object Argon2Kdf {

    // Default parameters for custom user credential derivation:
    // Memory: 64 MB (64 * 1024 KiB)
    // Iterations: 3
    // Parallelism: 2
    // Output key length: 32 bytes (256 bits)
    const val DEFAULT_ITERATIONS = 3
    const val DEFAULT_MEMORY_KB = 64 * 1024
    const val DEFAULT_PARALLELISM = 2
    const val OUTPUT_KEY_LENGTH = 32

    fun generateRandomSalt(size: Int = 16): ByteArray {
        val salt = ByteArray(size)
        SecureRandom().nextBytes(salt)
        return salt
    }

    /**
     * Derives a 256-bit key from password chars using Argon2id.
     * Clears intermediate password byte representations securely.
     */
    fun deriveKey(
        password: CharArray,
        salt: ByteArray,
        iterations: Int = DEFAULT_ITERATIONS,
        memoryCostKb: Int = DEFAULT_MEMORY_KB,
        parallelism: Int = DEFAULT_PARALLELISM
    ): ByteArray {
        val passwordBytes = charArrayToUtf8Bytes(password)
        try {
            val params = Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
                .withVersion(Argon2Parameters.ARGON2_VERSION_13)
                .withIterations(iterations)
                .withMemoryAsKB(memoryCostKb)
                .withParallelism(parallelism)
                .withSalt(salt)
                .build()

            val generator = Argon2BytesGenerator()
            generator.init(params)

            val resultKey = ByteArray(OUTPUT_KEY_LENGTH)
            generator.generateBytes(passwordBytes, resultKey, 0, resultKey.size)
            return resultKey
        } finally {
            Arrays.fill(passwordBytes, 0.toByte())
        }
    }

    /**
     * Derives a 256-bit key from raw recovery secret bytes using Argon2id.
     */
    fun deriveKeyFromBytes(
        secretBytes: ByteArray,
        salt: ByteArray,
        iterations: Int = DEFAULT_ITERATIONS,
        memoryCostKb: Int = DEFAULT_MEMORY_KB,
        parallelism: Int = DEFAULT_PARALLELISM
    ): ByteArray {
        val params = Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
            .withVersion(Argon2Parameters.ARGON2_VERSION_13)
            .withIterations(iterations)
            .withMemoryAsKB(memoryCostKb)
            .withParallelism(parallelism)
            .withSalt(salt)
            .build()

        val generator = Argon2BytesGenerator()
        generator.init(params)

        val resultKey = ByteArray(OUTPUT_KEY_LENGTH)
        generator.generateBytes(secretBytes, resultKey, 0, resultKey.size)
        return resultKey
    }

    private fun charArrayToUtf8Bytes(chars: CharArray): ByteArray {
        val charBuffer = java.nio.CharBuffer.wrap(chars)
        val byteBuffer = java.nio.charset.StandardCharsets.UTF_8.encode(charBuffer)
        val bytes = ByteArray(byteBuffer.remaining())
        byteBuffer.get(bytes)
        return bytes
    }
}
