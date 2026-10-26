package com.example.crypto

import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import com.example.data.AppDatabase
import com.example.data.AuditLogEntry
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.security.DigestOutputStream
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Arrays
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

enum class SignatureMode {
    NONE,
    DEFAULT_HIDDEN,
    CUSTOM_8_LETTER
}

data class DecryptionResult(
    val decryptedTempFile: File,
    val metadata: PrivateMetadata,
    val signatureVerified: Boolean,
    val isCustomIdentity: Boolean,
    val signerIdentity: String,
    val algorithm: CipherAlgorithm,
    val generationNumber: Int
)

object SkyVaultEngine {

    /**
     * Resolves file metadata from Android Storage Access Framework Uri.
     */
    fun getFileInfo(context: Context, uri: Uri): Pair<String, Long> {
        var name = "encrypted_payload"
        var size = 0L
        if (uri.scheme == "content") {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1) name = cursor.getString(nameIndex) ?: name
                    if (sizeIndex != -1) size = cursor.getLong(sizeIndex)
                }
            }
        } else if (uri.scheme == "file") {
            uri.path?.let {
                val f = File(it)
                name = f.name
                size = f.length()
            }
        }
        return Pair(name, size)
    }

    /**
     * Encrypts a source file into a temporary .adsky container file,
     * fully verifying it after completion.
     */
    suspend fun encryptFile(
        context: Context,
        sourceUri: Uri,
        algorithm: CipherAlgorithm,
        customCredential: CharArray?,
        enableDeviceKeystore: Boolean,
        enableRecoverySecret: Boolean,
        signatureMode: SignatureMode,
        customIdentityLabel: String?,
        generationNumber: Int = 1,
        progressCallback: (bytesProcessed: Long, totalBytes: Long, currentChunk: Int, message: String) -> Unit,
        isCancelled: () -> Boolean
    ): File {
        val (originalFilename, originalSize) = getFileInfo(context, sourceUri)
        val mimeType = context.contentResolver.getType(sourceUri) ?: "application/octet-stream"

        if (signatureMode == SignatureMode.CUSTOM_8_LETTER) {
            require(!customIdentityLabel.isNullOrBlank() && KeystoreManager.isValidCustomIdentity(customIdentityLabel)) {
                "Custom signature identity must be exactly 8 alphabetic letters (A-Z)."
            }
        }

        val tempOutputFile = File.createTempFile("vault_enc_", ".adsky", context.cacheDir)
        var success = false

        try {
            // 1. Generate fresh 256-bit cryptographically secure DEK
            val dek = ByteArray(32).apply {
                SecureRandom().nextBytes(this)
            }

            // 2. Prepare Envelopes
            val envelopes = mutableListOf<EnvelopeDescriptor>()
            var flags: Short = 0

            // Custom Credential Envelope
            if (customCredential != null && customCredential.isNotEmpty()) {
                val salt = Argon2Kdf.generateRandomSalt(16)
                val kek = Argon2Kdf.deriveKey(
                    customCredential,
                    salt,
                    Argon2Kdf.DEFAULT_ITERATIONS,
                    Argon2Kdf.DEFAULT_MEMORY_KB,
                    Argon2Kdf.DEFAULT_PARALLELISM
                )
                try {
                    val (iv, wrappedDek) = wrapDekWithAesGcm(kek, dek, "ENVELOPE_CREDENTIAL".toByteArray(StandardCharsets.UTF_8))
                    envelopes.add(
                        EnvelopeDescriptor(
                            envelopeType = ContainerConstants.ENVELOPE_CUSTOM_CREDENTIAL,
                            kdfType = ContainerConstants.KDF_ARGON2ID,
                            salt = salt,
                            iterations = Argon2Kdf.DEFAULT_ITERATIONS,
                            memoryCostKb = Argon2Kdf.DEFAULT_MEMORY_KB,
                            parallelism = Argon2Kdf.DEFAULT_PARALLELISM,
                            iv = iv,
                            wrappedDek = wrappedDek
                        )
                    )
                    flags = (flags.toInt() or ContainerConstants.FLAG_HAS_CUSTOM_CREDENTIAL.toInt()).toShort()
                } finally {
                    Arrays.fill(kek, 0.toByte())
                }
            }

            // Device Keystore Envelope
            if (enableDeviceKeystore) {
                try {
                    val (iv, wrappedDek) = KeystoreManager.wrapDekWithDeviceKey(
                        dek,
                        "ENVELOPE_DEVICE_KEYSTORE".toByteArray(StandardCharsets.UTF_8)
                    )
                    envelopes.add(
                        EnvelopeDescriptor(
                            envelopeType = ContainerConstants.ENVELOPE_DEVICE_KEYSTORE,
                            kdfType = ContainerConstants.KDF_KEYSTORE_DIRECT,
                            salt = ByteArray(0),
                            iterations = 0,
                            memoryCostKb = 0,
                            parallelism = 0,
                            iv = iv,
                            wrappedDek = wrappedDek
                        )
                    )
                    flags = (flags.toInt() or ContainerConstants.FLAG_HAS_DEVICE_KEYSTORE.toInt()).toShort()
                } catch (e: Exception) {
                    throw SecurityException("Failed to wrap DEK with Android Keystore: ${e.message}", e)
                }
            }

            // Recovery Secret Envelope
            if (enableRecoverySecret) {
                val recoverySecretStr = RecoveryKeyManager.getOrCreateInstallationRecoverySecret(context)
                val recoverySecretBytes = RecoveryKeyManager.parseSecretToBytes(recoverySecretStr)
                val salt = Argon2Kdf.generateRandomSalt(16)
                val recoveryWrappingKey = RecoveryKeyManager.deriveRecoveryWrappingKey(recoverySecretBytes, salt)
                try {
                    val (iv, wrappedDek) = wrapDekWithAesGcm(
                        recoveryWrappingKey,
                        dek,
                        "ENVELOPE_RECOVERY_SECRET".toByteArray(StandardCharsets.UTF_8)
                    )
                    envelopes.add(
                        EnvelopeDescriptor(
                            envelopeType = ContainerConstants.ENVELOPE_RECOVERY_SECRET,
                            kdfType = ContainerConstants.KDF_RECOVERY_ARGON2ID,
                            salt = salt,
                            iterations = Argon2Kdf.DEFAULT_ITERATIONS,
                            memoryCostKb = Argon2Kdf.DEFAULT_MEMORY_KB,
                            parallelism = Argon2Kdf.DEFAULT_PARALLELISM,
                            iv = iv,
                            wrappedDek = wrappedDek
                        )
                    )
                    flags = (flags.toInt() or ContainerConstants.FLAG_HAS_RECOVERY_SECRET.toInt()).toShort()
                } finally {
                    Arrays.fill(recoverySecretBytes, 0.toByte())
                    Arrays.fill(recoveryWrappingKey, 0.toByte())
                }
            }

            if (envelopes.isEmpty()) {
                throw IllegalArgumentException("At least one access envelope (Device Keystore, Custom Credential, or Recovery Secret) must be enabled.")
            }

            // Signature configuration
            val isSigned = signatureMode != SignatureMode.NONE
            val isCustomSig = signatureMode == SignatureMode.CUSTOM_8_LETTER
            if (isSigned) {
                flags = (flags.toInt() or ContainerConstants.FLAG_IS_SIGNED.toInt()).toShort()
                if (isCustomSig) {
                    flags = (flags.toInt() or ContainerConstants.FLAG_CUSTOM_SIGNATURE_IDENTITY.toInt()).toShort()
                }
            }

            // Signing identity & fingerprint
            val signingKey = if (isSigned) KeystoreManager.getOrCreateSigningKeyPair(isCustomSig) else null
            val signerFingerprint = signingKey?.public?.encoded?.let { KeystoreManager.computePublicKeyFingerprint(it) } ?: ""
            val signatureIdentity = when (signatureMode) {
                SignatureMode.CUSTOM_8_LETTER -> customIdentityLabel?.uppercase() ?: "CUSTOMID"
                SignatureMode.DEFAULT_HIDDEN -> "DEFAULT_HIDDEN"
                SignatureMode.NONE -> "UNSIGNED"
            }

            val privateMetadata = PrivateMetadata(
                originalFilename = originalFilename,
                mimeType = mimeType,
                originalByteCount = originalSize,
                creationTimestamp = System.currentTimeMillis(),
                generationNumber = generationNumber,
                signatureIdentity = signatureIdentity,
                signerFingerprint = signerFingerprint,
                deviceModelInfo = "${Build.MANUFACTURER} ${Build.MODEL}",
                deviceKeyAlias = if (enableDeviceKeystore) KeystoreManager.DEVICE_MASTER_KEY_ALIAS else ""
            )

            // 3. Encrypt Private Metadata under DEK
            val cipher: AeadCipher = when (algorithm) {
                CipherAlgorithm.AES_256_GCM -> AesGcmCipher(dek)
                CipherAlgorithm.XCHACHA20_POLY1305 -> XChaCha20Cipher(dek)
            }

            // Temporary buffer to calculate canonical header AAD
            val dummyHeaderBaos = ByteArrayOutputStream()
            val canonicalAad = ContainerFormat.writeContainerHeader(
                dummyHeaderBaos,
                algorithm,
                flags,
                generationNumber,
                envelopes,
                ByteArray(12), // Placeholder for metadata IV
                ByteArray(0)
            )

            val (metaIv, encryptedMeta) = cipher.encryptWithNonce(
                privateMetadata.toJsonBytes(),
                canonicalAad
            )

            // Setup DigestOutputStream to compute container signature hash over all written data
            val containerSha256 = MessageDigest.getInstance("SHA-256")
            val fos = FileOutputStream(tempOutputFile)
            val bos = BufferedOutputStream(fos)
            val digestOut = DigestOutputStream(bos, containerSha256)

            // Write actual header to output
            ContainerFormat.writeContainerHeader(
                digestOut,
                algorithm,
                flags,
                generationNumber,
                envelopes,
                metaIv,
                encryptedMeta
            )

            // 4. Stream Source File Chunks
            val buffer = ByteArray(ContainerConstants.CHUNK_SIZE)
            var chunkIndex = 0
            var bytesProcessed = 0L
            val inputStream = context.contentResolver.openInputStream(sourceUri)
                ?: error("Unable to open source file stream for $sourceUri")

            BufferedInputStream(inputStream).use { bis ->
                val dos = DataOutputStream(digestOut)

                while (true) {
                    if (isCancelled()) {
                        throw InterruptedException("Encryption cancelled by user.")
                    }

                    var chunkLen = 0
                    while (chunkLen < ContainerConstants.CHUNK_SIZE) {
                        val read = bis.read(buffer, chunkLen, ContainerConstants.CHUNK_SIZE - chunkLen)
                        if (read == -1) break
                        chunkLen += read
                    }
                    if (chunkLen == 0) break

                    val chunkPlaintext = buffer.copyOfRange(0, chunkLen)
                    val chunkAad = ContainerFormat.buildChunkAad(
                        ContainerConstants.FORMAT_VERSION,
                        algorithm.id,
                        chunkIndex,
                        ContainerConstants.RECORD_PAYLOAD_CHUNK
                    )

                    // Encrypt chunk
                    val (chunkNonce, chunkCiphertext) = cipher.encryptWithNonce(chunkPlaintext, chunkAad)

                    // Write Chunk Record
                    dos.writeByte(ContainerConstants.RECORD_PAYLOAD_CHUNK.toInt())
                    dos.writeInt(chunkIndex)
                    dos.writeByte(chunkNonce.size)
                    dos.write(chunkNonce)
                    dos.writeInt(chunkCiphertext.size)
                    dos.write(chunkCiphertext)

                    chunkIndex++
                    bytesProcessed += chunkLen
                    progressCallback(
                        bytesProcessed,
                        originalSize,
                        chunkIndex,
                        "Encrypting chunk $chunkIndex (${bytesProcessed / 1024} KB)..."
                    )
                }

                // 5. Authenticated Final Record
                val finalAad = ContainerFormat.buildFinalRecordAad(chunkIndex, bytesProcessed)
                val finalVerificationToken = "ADSKY-PAYLOAD-COMPLETE".toByteArray(StandardCharsets.US_ASCII)
                val (finalIv, encryptedFinalToken) = cipher.encryptWithNonce(finalVerificationToken, finalAad)

                dos.writeByte(ContainerConstants.RECORD_FINAL.toInt())
                dos.writeInt(chunkIndex)
                dos.writeLong(bytesProcessed)
                dos.writeByte(finalIv.size)
                dos.write(finalIv)
                dos.writeInt(encryptedFinalToken.size)
                dos.write(encryptedFinalToken)

                dos.flush()
            }

            // 6. Digital Signature Record (if signed)
            if (isSigned && signingKey != null) {
                digestOut.on(false) // Stop digesting to sign the container hash computed so far
                val containerDigest = containerSha256.digest()
                val signatureBytes = KeystoreManager.signData(containerDigest, isCustomSig)
                val publicKeyBytes = signingKey.public.encoded

                val dos = DataOutputStream(digestOut)
                dos.writeShort(publicKeyBytes.size)
                dos.write(publicKeyBytes)
                dos.writeShort(signatureBytes.size)
                dos.write(signatureBytes)
                dos.flush()
            }

            digestOut.flush()
            fos.fd.sync()
            digestOut.close()

            // 7. Verify the output file immediately by reading and validating header & DEK
            verifyEncryptedContainer(tempOutputFile)

            progressCallback(bytesProcessed, originalSize, chunkIndex, "Encryption and container verification complete!")
            success = true

            // Log security audit entry
            AppDatabase.getInstance(context).auditLogDao().insertLog(
                AuditLogEntry(
                    operation = "ENCRYPT",
                    filename = originalFilename,
                    algorithm = algorithm.displayName,
                    envelopeUsed = envelopes.joinToString(", ") {
                        when (it.envelopeType) {
                            ContainerConstants.ENVELOPE_DEVICE_KEYSTORE -> "Device Keystore"
                            ContainerConstants.ENVELOPE_CUSTOM_CREDENTIAL -> "Custom PIN/Password"
                            ContainerConstants.ENVELOPE_RECOVERY_SECRET -> "Recovery Secret"
                            else -> "Envelope ${it.envelopeType}"
                        }
                    },
                    status = "SUCCESS",
                    details = "Encrypted $bytesProcessed bytes in $chunkIndex chunks. Sig: $signatureIdentity."
                )
            )

            return tempOutputFile
        } catch (e: Exception) {
            AppDatabase.getInstance(context).auditLogDao().insertLog(
                AuditLogEntry(
                    operation = "ENCRYPT",
                    filename = originalFilename,
                    algorithm = algorithm.displayName,
                    envelopeUsed = "N/A",
                    status = if (e is InterruptedException) "CANCELLED" else "FAILURE",
                    details = e.message ?: "Unknown error"
                )
            )
            tempOutputFile.delete()
            throw e
        }
    }

    /**
     * Decrypts an .adsky file into a verified temporary output file.
     */
    suspend fun decryptFile(
        context: Context,
        containerUri: Uri,
        customCredential: CharArray?,
        recoverySecretString: String?,
        progressCallback: (bytesProcessed: Long, totalBytes: Long, currentChunk: Int, message: String) -> Unit,
        isCancelled: () -> Boolean
    ): DecryptionResult {
        val (containerFilename, _) = getFileInfo(context, containerUri)
        val tempOutputFile = File.createTempFile("vault_dec_", ".tmp", context.cacheDir)
        var success = false

        try {
            val containerSha256 = MessageDigest.getInstance("SHA-256")
            val inputStream = context.contentResolver.openInputStream(containerUri)
                ?: error("Unable to open input stream for container: $containerUri")

            val bis = BufferedInputStream(inputStream)
            val digestIn = java.security.DigestInputStream(bis, containerSha256)
            val dis = DataInputStream(digestIn)

            // 1. Read and validate public container header
            val header = ContainerFormat.readContainerHeader(digestIn)

            // 2. Unwrap DEK using available credentials / envelopes
            var unwrappedDek: ByteArray? = null
            var usedEnvelopeName = "Unknown"

            // Try Custom Credential if provided
            if (customCredential != null && customCredential.isNotEmpty()) {
                val credEnv = header.envelopes.firstOrNull { it.envelopeType == ContainerConstants.ENVELOPE_CUSTOM_CREDENTIAL }
                if (credEnv != null) {
                    val kek = Argon2Kdf.deriveKey(
                        customCredential,
                        credEnv.salt,
                        credEnv.iterations,
                        credEnv.memoryCostKb,
                        credEnv.parallelism
                    )
                    try {
                        unwrappedDek = unwrapDekWithAesGcm(kek, credEnv.iv, credEnv.wrappedDek, "ENVELOPE_CREDENTIAL".toByteArray(StandardCharsets.UTF_8))
                        usedEnvelopeName = "Custom PIN/Password"
                    } catch (e: Exception) {
                        // Credential invalid
                    } finally {
                        Arrays.fill(kek, 0.toByte())
                    }
                }
            }

            // Try Recovery Secret if provided
            if (unwrappedDek == null && !recoverySecretString.isNullOrBlank()) {
                val recEnv = header.envelopes.firstOrNull { it.envelopeType == ContainerConstants.ENVELOPE_RECOVERY_SECRET }
                if (recEnv != null) {
                    val recoveryBytes = RecoveryKeyManager.parseSecretToBytes(recoverySecretString)
                    val recKek = RecoveryKeyManager.deriveRecoveryWrappingKey(
                        recoveryBytes,
                        recEnv.salt,
                        recEnv.iterations,
                        recEnv.memoryCostKb,
                        recEnv.parallelism
                    )
                    try {
                        unwrappedDek = unwrapDekWithAesGcm(recKek, recEnv.iv, recEnv.wrappedDek, "ENVELOPE_RECOVERY_SECRET".toByteArray(StandardCharsets.UTF_8))
                        usedEnvelopeName = "Recovery Secret"
                    } catch (e: Exception) {
                        // Recovery secret invalid
                    } finally {
                        Arrays.fill(recoveryBytes, 0.toByte())
                        Arrays.fill(recKek, 0.toByte())
                    }
                }
            }

            // Try Original Device Keystore (if no password/recovery provided or fallback)
            if (unwrappedDek == null) {
                val deviceEnv = header.envelopes.firstOrNull { it.envelopeType == ContainerConstants.ENVELOPE_DEVICE_KEYSTORE }
                if (deviceEnv != null) {
                    try {
                        unwrappedDek = KeystoreManager.unwrapDekWithDeviceKey(
                            deviceEnv.iv,
                            deviceEnv.wrappedDek,
                            "ENVELOPE_DEVICE_KEYSTORE".toByteArray(StandardCharsets.UTF_8)
                        )
                        usedEnvelopeName = "Device Keystore"
                    } catch (e: Exception) {
                        // Device keystore not matching (e.g. on another device)
                    }
                }
            }

            if (unwrappedDek == null) {
                val availableEnvelopes = header.envelopes.map {
                    when (it.envelopeType) {
                        ContainerConstants.ENVELOPE_DEVICE_KEYSTORE -> "Original Device Keystore"
                        ContainerConstants.ENVELOPE_CUSTOM_CREDENTIAL -> "Custom PIN/Password"
                        ContainerConstants.ENVELOPE_RECOVERY_SECRET -> "Recovery Secret"
                        else -> "Unknown"
                    }
                }
                throw SecurityException(
                    "Authorization failed: Could not unwrap DEK. Available envelopes: ${availableEnvelopes.joinToString(", ")}. " +
                            "Please enter the correct PIN/Password or Recovery Secret."
                )
            }

            // 3. Decrypt Private Metadata
            val cipher: AeadCipher = when (header.algorithm) {
                CipherAlgorithm.AES_256_GCM -> AesGcmCipher(unwrappedDek)
                CipherAlgorithm.XCHACHA20_POLY1305 -> XChaCha20Cipher(unwrappedDek)
            }

            val metadataJsonBytes = cipher.decryptWithNonce(
                header.encryptedMetadataIv,
                header.encryptedMetadataCiphertext,
                header.canonicalHeaderAad
            )
            val metadata = PrivateMetadata.fromJsonBytes(metadataJsonBytes)

            // 4. Stream and Decrypt Chunks
            val fos = FileOutputStream(tempOutputFile)
            val bos = BufferedOutputStream(fos)
            var expectedChunkIndex = 0
            var totalBytesDecrypted = 0L

            while (true) {
                if (isCancelled()) {
                    throw InterruptedException("Decryption cancelled by user.")
                }

                val recordType = dis.readByte()
                if (recordType == ContainerConstants.RECORD_FINAL) {
                    // Final record reached!
                    val totalChunks = dis.readInt()
                    val totalPlaintextBytes = dis.readLong()

                    val finalIvLen = dis.readByte().toInt() and 0xFF
                    val finalIv = ByteArray(finalIvLen)
                    dis.readFully(finalIv)

                    val finalCiphertextLen = dis.readInt()
                    val finalCiphertext = ByteArray(finalCiphertextLen)
                    dis.readFully(finalCiphertext)

                    // Authenticate final record
                    val finalAad = ContainerFormat.buildFinalRecordAad(totalChunks, totalPlaintextBytes)
                    val decryptedToken = cipher.decryptWithNonce(finalIv, finalCiphertext, finalAad)
                    val expectedToken = "ADSKY-PAYLOAD-COMPLETE".toByteArray(StandardCharsets.US_ASCII)
                    if (!decryptedToken.contentEquals(expectedToken)) {
                        throw SecurityException("Final integrity authentication record mismatch.")
                    }

                    if (expectedChunkIndex != totalChunks) {
                        throw SecurityException("Chunk count mismatch: expected $totalChunks, got $expectedChunkIndex")
                    }
                    if (totalBytesDecrypted != totalPlaintextBytes) {
                        throw SecurityException("Byte count mismatch: expected $totalPlaintextBytes bytes, got $totalBytesDecrypted")
                    }
                    break
                } else if (recordType == ContainerConstants.RECORD_PAYLOAD_CHUNK) {
                    val chunkIndex = dis.readInt()
                    if (chunkIndex != expectedChunkIndex) {
                        throw SecurityException("Chunk sequence violation: expected chunk $expectedChunkIndex, received $chunkIndex")
                    }

                    val nonceLen = dis.readByte().toInt() and 0xFF
                    if (nonceLen > ContainerConstants.MAX_IV_LENGTH) {
                        throw SecurityException("Invalid nonce length: $nonceLen")
                    }
                    val nonce = ByteArray(nonceLen)
                    dis.readFully(nonce)

                    val ciphertextLen = dis.readInt()
                    if (ciphertextLen <= 0 || ciphertextLen > ContainerConstants.MAX_CHUNK_PAYLOAD_SIZE) {
                        throw SecurityException("Invalid chunk payload length: $ciphertextLen")
                    }
                    val ciphertext = ByteArray(ciphertextLen)
                    dis.readFully(ciphertext)

                    val chunkAad = ContainerFormat.buildChunkAad(
                        header.formatVersion,
                        header.algorithm.id,
                        chunkIndex,
                        ContainerConstants.RECORD_PAYLOAD_CHUNK
                    )

                    val plaintext = cipher.decryptWithNonce(nonce, ciphertext, chunkAad)
                    bos.write(plaintext)
                    totalBytesDecrypted += plaintext.size
                    expectedChunkIndex++

                    progressCallback(
                        totalBytesDecrypted,
                        metadata.originalByteCount,
                        expectedChunkIndex,
                        "Decrypted chunk $expectedChunkIndex (${totalBytesDecrypted / 1024} KB)..."
                    )
                } else {
                    throw SecurityException("Unknown container record type: 0x%02X".format(recordType))
                }
            }

            bos.flush()
            fos.fd.sync()
            bos.close()

            // 5. Signature verification (if signed)
            var signatureVerified = false
            val isSigned = (header.flags.toInt() and ContainerConstants.FLAG_IS_SIGNED.toInt()) != 0
            val isCustomSig = (header.flags.toInt() and ContainerConstants.FLAG_CUSTOM_SIGNATURE_IDENTITY.toInt()) != 0

            if (isSigned) {
                digestIn.on(false) // Turn off digest to read signature records
                val containerDigest = containerSha256.digest()

                val pubKeyLen = dis.readShort().toInt() and 0xFFFF
                if (pubKeyLen > ContainerConstants.MAX_PUBLIC_KEY_LENGTH) {
                    throw SecurityException("Public key length exceeds bounds: $pubKeyLen")
                }
                val pubKeyBytes = ByteArray(pubKeyLen)
                dis.readFully(pubKeyBytes)

                val sigLen = dis.readShort().toInt() and 0xFFFF
                if (sigLen > ContainerConstants.MAX_SIGNATURE_LENGTH) {
                    throw SecurityException("Signature length exceeds bounds: $sigLen")
                }
                val sigBytes = ByteArray(sigLen)
                dis.readFully(sigBytes)

                val valid = KeystoreManager.verifySignature(pubKeyBytes, containerDigest, sigBytes)
                if (!valid) {
                    throw SecurityException("Digital signature verification failed! The container payload has been tampered with or corrupted.")
                }
                signatureVerified = true
            }

            digestIn.close()
            success = true

            AppDatabase.getInstance(context).auditLogDao().insertLog(
                AuditLogEntry(
                    operation = "DECRYPT",
                    filename = metadata.originalFilename,
                    algorithm = header.algorithm.displayName,
                    envelopeUsed = usedEnvelopeName,
                    status = "SUCCESS",
                    details = "Decrypted $totalBytesDecrypted bytes. SigVerified: $signatureVerified."
                )
            )

            return DecryptionResult(
                decryptedTempFile = tempOutputFile,
                metadata = metadata,
                signatureVerified = signatureVerified,
                isCustomIdentity = isCustomSig,
                signerIdentity = metadata.signatureIdentity,
                algorithm = header.algorithm,
                generationNumber = header.generationNumber
            )
        } catch (e: Exception) {
            AppDatabase.getInstance(context).auditLogDao().insertLog(
                AuditLogEntry(
                    operation = "DECRYPT",
                    filename = containerFilename,
                    algorithm = "N/A",
                    envelopeUsed = "N/A",
                    status = if (e is InterruptedException) "CANCELLED" else "FAILURE",
                    details = e.message ?: "Decryption error"
                )
            )
            tempOutputFile.delete()
            throw e
        }
    }

    /**
     * Inspects a container's public header without decrypting payload.
     */
    fun inspectContainerHeader(context: Context, containerUri: Uri): Pair<ParsedContainerHeader, List<String>> {
        context.contentResolver.openInputStream(containerUri)?.use { inputStream ->
            val header = ContainerFormat.readContainerHeader(inputStream)
            val envelopeNames = header.envelopes.map {
                when (it.envelopeType) {
                    ContainerConstants.ENVELOPE_DEVICE_KEYSTORE -> "Original Device Keystore"
                    ContainerConstants.ENVELOPE_CUSTOM_CREDENTIAL -> "Custom PIN/Password"
                    ContainerConstants.ENVELOPE_RECOVERY_SECRET -> "Recovery Secret"
                    else -> "Envelope 0x%02X".format(it.envelopeType)
                }
            }
            return Pair(header, envelopeNames)
        } ?: error("Unable to open stream for $containerUri")
    }

    /**
     * Re-encrypts a decrypted result into a new container bound to this device,
     * with incremented generation number.
     */
    suspend fun migrateContainer(
        context: Context,
        decryptedFile: File,
        originalMetadata: PrivateMetadata,
        newAlgorithm: CipherAlgorithm,
        newCustomCredential: CharArray?,
        enableDeviceKeystore: Boolean,
        enableRecoverySecret: Boolean,
        signatureMode: SignatureMode,
        customIdentityLabel: String?,
        progressCallback: (bytesProcessed: Long, totalBytes: Long, currentChunk: Int, message: String) -> Unit,
        isCancelled: () -> Boolean
    ): File {
        val uri = Uri.fromFile(decryptedFile)
        val newGeneration = originalMetadata.generationNumber + 1
        return encryptFile(
            context = context,
            sourceUri = uri,
            algorithm = newAlgorithm,
            customCredential = newCustomCredential,
            enableDeviceKeystore = enableDeviceKeystore,
            enableRecoverySecret = enableRecoverySecret,
            signatureMode = signatureMode,
            customIdentityLabel = customIdentityLabel,
            generationNumber = newGeneration,
            progressCallback = progressCallback,
            isCancelled = isCancelled
        )
    }

    /**
     * Verifies that an encrypted container file can be opened and parsed cleanly.
     */
    private fun verifyEncryptedContainer(file: File) {
        FileInputStream(file).use { fis ->
            val header = ContainerFormat.readContainerHeader(fis)
            require(header.envelopes.isNotEmpty()) { "Container verification failed: No valid envelopes." }
        }
    }

    private fun wrapDekWithAesGcm(key: ByteArray, dek: ByteArray, aad: ByteArray): Pair<ByteArray, ByteArray> {
        val iv = ByteArray(12).apply { SecureRandom().nextBytes(this) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, iv))
        if (aad.isNotEmpty()) cipher.updateAAD(aad)
        val wrapped = cipher.doFinal(dek)
        return Pair(iv, wrapped)
    }

    private fun unwrapDekWithAesGcm(key: ByteArray, iv: ByteArray, wrapped: ByteArray, aad: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, iv))
        if (aad.isNotEmpty()) cipher.updateAAD(aad)
        return cipher.doFinal(wrapped)
    }
}
