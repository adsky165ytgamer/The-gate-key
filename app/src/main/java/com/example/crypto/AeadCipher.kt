package com.example.crypto

import com.google.crypto.tink.Aead
import com.google.crypto.tink.InsecureSecretKeyAccess
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.aead.AesGcmKey
import com.google.crypto.tink.aead.AesGcmParameters
import com.google.crypto.tink.aead.XChaCha20Poly1305Key
import com.google.crypto.tink.aead.XChaCha20Poly1305Parameters
import com.google.crypto.tink.util.SecretBytes

interface AeadCipher {
    val algorithm: CipherAlgorithm

    fun encryptWithNonce(
        plaintext: ByteArray,
        associatedData: ByteArray,
        explicitNonce: ByteArray? = null
    ): Pair<ByteArray, ByteArray>

    fun decryptWithNonce(
        nonce: ByteArray,
        ciphertextAndTag: ByteArray,
        associatedData: ByteArray
    ): ByteArray
}

class AesGcmCipher(private val key: ByteArray) : AeadCipher {
    override val algorithm: CipherAlgorithm = CipherAlgorithm.AES_256_GCM
    private val aead: Aead

    init {
        require(key.size == 32) { "AES-256 requires 32-byte key, provided ${key.size} bytes" }
        AeadConfig.register()
        val parameters = AesGcmParameters.builder()
            .setKeySizeBytes(32)
            .setIvSizeBytes(12)
            .setTagSizeBytes(16)
            .setVariant(AesGcmParameters.Variant.NO_PREFIX)
            .build()
        val secretKey = AesGcmKey.builder()
            .setParameters(parameters)
            .setKeyBytes(SecretBytes.copyFrom(key, InsecureSecretKeyAccess.get()))
            .build()
        val handle = KeysetHandle.newBuilder()
            .addEntry(KeysetHandle.importKey(secretKey).makePrimary().withRandomId())
            .build()
        aead = handle.getPrimitive(Aead::class.java)
    }

    override fun encryptWithNonce(
        plaintext: ByteArray,
        associatedData: ByteArray,
        explicitNonce: ByteArray?
    ): Pair<ByteArray, ByteArray> {
        val fullCiphertext = aead.encrypt(plaintext, associatedData)
        val nonce = fullCiphertext.copyOfRange(0, 12)
        val ciphertextAndTag = fullCiphertext.copyOfRange(12, fullCiphertext.size)
        return Pair(nonce, ciphertextAndTag)
    }

    override fun decryptWithNonce(
        nonce: ByteArray,
        ciphertextAndTag: ByteArray,
        associatedData: ByteArray
    ): ByteArray {
        require(nonce.size == 12) { "AES-GCM nonce must be 12 bytes" }
        val fullCiphertext = ByteArray(nonce.size + ciphertextAndTag.size)
        System.arraycopy(nonce, 0, fullCiphertext, 0, nonce.size)
        System.arraycopy(ciphertextAndTag, 0, fullCiphertext, nonce.size, ciphertextAndTag.size)
        return aead.decrypt(fullCiphertext, associatedData)
    }
}

class XChaCha20Cipher(private val key: ByteArray) : AeadCipher {
    override val algorithm: CipherAlgorithm = CipherAlgorithm.XCHACHA20_POLY1305
    private val aead: Aead

    init {
        require(key.size == 32) { "XChaCha20-Poly1305 requires 32-byte key, provided ${key.size} bytes" }
        AeadConfig.register()
        val secretKey = XChaCha20Poly1305Key.create(
            XChaCha20Poly1305Parameters.Variant.NO_PREFIX,
            SecretBytes.copyFrom(key, InsecureSecretKeyAccess.get()),
            null
        )
        val handle = KeysetHandle.newBuilder()
            .addEntry(KeysetHandle.importKey(secretKey).makePrimary().withRandomId())
            .build()
        aead = handle.getPrimitive(Aead::class.java)
    }

    override fun encryptWithNonce(
        plaintext: ByteArray,
        associatedData: ByteArray,
        explicitNonce: ByteArray?
    ): Pair<ByteArray, ByteArray> {
        val fullCiphertext = aead.encrypt(plaintext, associatedData)
        val nonce = fullCiphertext.copyOfRange(0, 24)
        val ciphertextAndTag = fullCiphertext.copyOfRange(24, fullCiphertext.size)
        return Pair(nonce, ciphertextAndTag)
    }

    override fun decryptWithNonce(
        nonce: ByteArray,
        ciphertextAndTag: ByteArray,
        associatedData: ByteArray
    ): ByteArray {
        require(nonce.size == 24) { "XChaCha20-Poly1305 nonce must be 24 bytes" }
        val fullCiphertext = ByteArray(nonce.size + ciphertextAndTag.size)
        System.arraycopy(nonce, 0, fullCiphertext, 0, nonce.size)
        System.arraycopy(ciphertextAndTag, 0, fullCiphertext, nonce.size, ciphertextAndTag.size)
        return aead.decrypt(fullCiphertext, associatedData)
    }
}
