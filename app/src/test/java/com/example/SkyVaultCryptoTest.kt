package com.example

import com.example.crypto.AesGcmCipher
import com.example.crypto.Argon2Kdf
import com.example.crypto.CipherAlgorithm
import com.example.crypto.ContainerConstants
import com.example.crypto.ContainerFormat
import com.example.crypto.EnvelopeDescriptor
import com.example.crypto.KeystoreManager
import com.example.crypto.RecoveryKeyManager
import com.example.crypto.XChaCha20Cipher
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.security.SecureRandom

class SkyVaultCryptoTest {

    @Test
    fun testAesGcmEncryptionAndDecryption() {
        val dek = ByteArray(32).apply { SecureRandom().nextBytes(this) }
        val cipher = AesGcmCipher(dek)

        val plaintext = "Arbitrary sensitive data inside AD Sky Vault".toByteArray(StandardCharsets.UTF_8)
        val aad = "CANONICAL-AAD-BYTES".toByteArray(StandardCharsets.UTF_8)

        val (nonce, ciphertext) = cipher.encryptWithNonce(plaintext, aad)
        assertEquals(12, nonce.size)
        assertTrue(ciphertext.size >= plaintext.size + 16) // 16-byte GCM tag

        val decrypted = cipher.decryptWithNonce(nonce, ciphertext, aad)
        assertArrayEquals(plaintext, decrypted)
    }

    @Test(expected = Exception::class)
    fun testAesGcmRejectsTamperedCiphertext() {
        val dek = ByteArray(32).apply { SecureRandom().nextBytes(this) }
        val cipher = AesGcmCipher(dek)
        val plaintext = "Hello World".toByteArray()
        val aad = "AAD".toByteArray()

        val (nonce, ciphertext) = cipher.encryptWithNonce(plaintext, aad)
        // Flip one bit
        ciphertext[0] = (ciphertext[0].toInt() xor 0x01).toByte()
        cipher.decryptWithNonce(nonce, ciphertext, aad)
    }

    @Test(expected = Exception::class)
    fun testAesGcmRejectsTamperedAad() {
        val dek = ByteArray(32).apply { SecureRandom().nextBytes(this) }
        val cipher = AesGcmCipher(dek)
        val plaintext = "Hello World".toByteArray()
        val aad = "VALID-AAD".toByteArray()
        val tamperedAad = "TAMPERED-AAD".toByteArray()

        val (nonce, ciphertext) = cipher.encryptWithNonce(plaintext, aad)
        cipher.decryptWithNonce(nonce, ciphertext, tamperedAad)
    }

    @Test
    fun testXChaCha20EncryptionAndDecryption() {
        val dek = ByteArray(32).apply { SecureRandom().nextBytes(this) }
        val cipher = XChaCha20Cipher(dek)

        val plaintext = "High entropy stream payload encrypted with XChaCha20-Poly1305".toByteArray(StandardCharsets.UTF_8)
        val aad = "AAD-XCHACHA-TEST".toByteArray(StandardCharsets.UTF_8)

        val (nonce, ciphertext) = cipher.encryptWithNonce(plaintext, aad)
        assertEquals(24, nonce.size) // 24-byte extended nonce
        assertTrue(ciphertext.size >= plaintext.size + 16)

        val decrypted = cipher.decryptWithNonce(nonce, ciphertext, aad)
        assertArrayEquals(plaintext, decrypted)
    }

    @Test(expected = Exception::class)
    fun testXChaCha20RejectsTamperedCiphertext() {
        val dek = ByteArray(32).apply { SecureRandom().nextBytes(this) }
        val cipher = XChaCha20Cipher(dek)
        val plaintext = "Sensitive XChaCha Payload".toByteArray()
        val aad = "AAD-TEST".toByteArray()

        val (nonce, ciphertext) = cipher.encryptWithNonce(plaintext, aad)
        ciphertext[5] = (ciphertext[5].toInt() xor 0x01).toByte()
        cipher.decryptWithNonce(nonce, ciphertext, aad)
    }

    @Test(expected = Exception::class)
    fun testXChaCha20RejectsTamperedAad() {
        val dek = ByteArray(32).apply { SecureRandom().nextBytes(this) }
        val cipher = XChaCha20Cipher(dek)
        val plaintext = "Sensitive XChaCha Payload".toByteArray()
        val aad = "AUTHENTIC-HEADER-AAD".toByteArray()
        val badAad = "CORRUPTED-HEADER-AAD".toByteArray()

        val (nonce, ciphertext) = cipher.encryptWithNonce(plaintext, aad)
        cipher.decryptWithNonce(nonce, ciphertext, badAad)
    }

    @Test
    fun testArgon2idKeyDerivation() {
        val password = "StrongPassphrase123!".toCharArray()
        val salt = Argon2Kdf.generateRandomSalt(16)

        val key1 = Argon2Kdf.deriveKey(password, salt, iterations = 1, memoryCostKb = 1024, parallelism = 1)
        val key2 = Argon2Kdf.deriveKey(password, salt, iterations = 1, memoryCostKb = 1024, parallelism = 1)

        assertEquals(32, key1.size)
        assertArrayEquals(key1, key2)

        val differentSalt = Argon2Kdf.generateRandomSalt(16)
        val key3 = Argon2Kdf.deriveKey(password, differentSalt, iterations = 1, memoryCostKb = 1024, parallelism = 1)
        assertFalse(key1.contentEquals(key3))

        val wrongPassword = "WrongPassphrase!".toCharArray()
        val keyWrong = Argon2Kdf.deriveKey(wrongPassword, salt, iterations = 1, memoryCostKb = 1024, parallelism = 1)
        assertFalse(key1.contentEquals(keyWrong))
    }

    @Test
    fun testRecoverySecretFormattingAndParsing() {
        val rawSecret = ByteArray(32).apply { SecureRandom().nextBytes(this) }
        val formatted = RecoveryKeyManager.bytesToFormattedSecret(rawSecret)

        assertTrue(formatted.startsWith("ADSKY-"))
        assertEquals(9, formatted.split("-").size) // "ADSKY" + 8 chunks

        val parsedBack = RecoveryKeyManager.parseSecretToBytes(formatted)
        assertArrayEquals(rawSecret, parsedBack)

        // Accepts lowercase and without dashes
        val lowerNoDashes = formatted.replace("-", "").lowercase()
        val parsedLower = RecoveryKeyManager.parseSecretToBytes(lowerNoDashes)
        assertArrayEquals(rawSecret, parsedLower)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testRecoverySecretRejectsInvalidCharacters() {
        RecoveryKeyManager.parseSecretToBytes("ADSKY-ZZZZ-NOT-HEX-CHARACTERS")
    }

    @Test(expected = IllegalArgumentException::class)
    fun testRecoverySecretRejectsWrongLength() {
        RecoveryKeyManager.parseSecretToBytes("ADSKY-1234-SHORT")
    }

    @Test
    fun testContainerHeaderSerializationAndParsing() {
        val baos = ByteArrayOutputStream()
        val envelope = EnvelopeDescriptor(
            envelopeType = ContainerConstants.ENVELOPE_CUSTOM_CREDENTIAL,
            kdfType = ContainerConstants.KDF_ARGON2ID,
            salt = ByteArray(16),
            iterations = 3,
            memoryCostKb = 64 * 1024,
            parallelism = 2,
            iv = ByteArray(12),
            wrappedDek = ByteArray(48)
        )

        val fakeMetaIv = ByteArray(12)
        val fakeMetaCiphertext = ByteArray(64)

        val writtenAad = ContainerFormat.writeContainerHeader(
            out = baos,
            algorithm = CipherAlgorithm.AES_256_GCM,
            flags = ContainerConstants.FLAG_HAS_CUSTOM_CREDENTIAL,
            generationNumber = 1,
            envelopes = listOf(envelope),
            encryptedMetadataIv = fakeMetaIv,
            encryptedMetadataCiphertext = fakeMetaCiphertext
        )

        val bais = ByteArrayInputStream(baos.toByteArray())
        val parsed = ContainerFormat.readContainerHeader(bais)

        assertEquals(ContainerConstants.FORMAT_VERSION, parsed.formatVersion)
        assertEquals(CipherAlgorithm.AES_256_GCM, parsed.algorithm)
        assertEquals(1, parsed.generationNumber)
        assertEquals(1, parsed.envelopes.size)
        assertEquals(ContainerConstants.ENVELOPE_CUSTOM_CREDENTIAL, parsed.envelopes[0].envelopeType)
        assertArrayEquals(writtenAad, parsed.canonicalHeaderAad)
    }

    @Test(expected = SecurityException::class)
    fun testContainerHeaderRejectsBadMagic() {
        val badData = "BADMAGICBYTE".toByteArray() + ByteArray(100)
        ContainerFormat.readContainerHeader(ByteArrayInputStream(badData))
    }

    @Test
    fun testChunkAadAndFinalRecordAad() {
        val aadChunk0 = ContainerFormat.buildChunkAad(1, 0x01, 0, ContainerConstants.RECORD_PAYLOAD_CHUNK)
        val aadChunk1 = ContainerFormat.buildChunkAad(1, 0x01, 1, ContainerConstants.RECORD_PAYLOAD_CHUNK)
        assertFalse(aadChunk0.contentEquals(aadChunk1))

        val finalAad = ContainerFormat.buildFinalRecordAad(5, 5242880L)
        val finalAadDifferentBytes = ContainerFormat.buildFinalRecordAad(5, 5242881L)
        assertFalse(finalAad.contentEquals(finalAadDifferentBytes))
    }

    @Test
    fun testCustom8LetterIdentityValidation() {
        assertTrue(KeystoreManager.isValidCustomIdentity("SECVAULT"))
        assertTrue(KeystoreManager.isValidCustomIdentity("ABCDEFGH"))
        assertTrue(KeystoreManager.isValidCustomIdentity("abcdefgh"))

        // Invalid cases
        assertFalse(KeystoreManager.isValidCustomIdentity("SHORT"))
        assertFalse(KeystoreManager.isValidCustomIdentity("TOOLONGNAME"))
        assertFalse(KeystoreManager.isValidCustomIdentity("SEC12345"))
        assertFalse(KeystoreManager.isValidCustomIdentity("SEC_VAUL"))
    }
}
