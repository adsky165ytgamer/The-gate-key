package com.example.crypto

import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets

data class EnvelopeDescriptor(
    val envelopeType: Byte,
    val kdfType: Byte,
    val salt: ByteArray,
    val iterations: Int,
    val memoryCostKb: Int,
    val parallelism: Int,
    val iv: ByteArray,
    val wrappedDek: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as EnvelopeDescriptor
        return envelopeType == other.envelopeType &&
                kdfType == other.kdfType &&
                salt.contentEquals(other.salt) &&
                iterations == other.iterations &&
                memoryCostKb == other.memoryCostKb &&
                parallelism == other.parallelism &&
                iv.contentEquals(other.iv) &&
                wrappedDek.contentEquals(other.wrappedDek)
    }

    override fun hashCode(): Int {
        var result = envelopeType.toInt()
        result = 31 * result + kdfType.toInt()
        result = 31 * result + salt.contentHashCode()
        result = 31 * result + iterations
        result = 31 * result + memoryCostKb
        result = 31 * result + parallelism
        result = 31 * result + iv.contentHashCode()
        result = 31 * result + wrappedDek.contentHashCode()
        return result
    }
}

data class PrivateMetadata(
    val originalFilename: String,
    val mimeType: String,
    val originalByteCount: Long,
    val creationTimestamp: Long,
    val generationNumber: Int,
    val signatureIdentity: String,
    val signerFingerprint: String,
    val deviceModelInfo: String,
    val deviceKeyAlias: String
) {
    fun toJsonBytes(): ByteArray {
        val json = JSONObject()
        json.put("fn", originalFilename)
        json.put("mt", mimeType)
        json.put("sz", originalByteCount)
        json.put("ts", creationTimestamp)
        json.put("gen", generationNumber)
        json.put("sigId", signatureIdentity)
        json.put("fp", signerFingerprint)
        json.put("dev", deviceModelInfo)
        json.put("alias", deviceKeyAlias)
        return json.toString().toByteArray(StandardCharsets.UTF_8)
    }

    companion object {
        fun fromJsonBytes(bytes: ByteArray): PrivateMetadata {
            val json = JSONObject(String(bytes, StandardCharsets.UTF_8))
            return PrivateMetadata(
                originalFilename = json.optString("fn", "decrypted_file"),
                mimeType = json.optString("mt", "application/octet-stream"),
                originalByteCount = json.optLong("sz", 0L),
                creationTimestamp = json.optLong("ts", System.currentTimeMillis()),
                generationNumber = json.optInt("gen", 1),
                signatureIdentity = json.optString("sigId", "DEFAULT_HIDDEN"),
                signerFingerprint = json.optString("fp", ""),
                deviceModelInfo = json.optString("dev", "Android Device"),
                deviceKeyAlias = json.optString("alias", "")
            )
        }
    }
}

data class ParsedContainerHeader(
    val formatVersion: Short,
    val algorithm: CipherAlgorithm,
    val flags: Short,
    val generationNumber: Int,
    val envelopes: List<EnvelopeDescriptor>,
    val encryptedMetadataIv: ByteArray,
    val encryptedMetadataCiphertext: ByteArray,
    val canonicalHeaderAad: ByteArray
)

object ContainerFormat {

    /**
     * Serializes public header and encrypted metadata into the output stream,
     * returning the canonical AAD bytes that authenticate all public header fields.
     */
    fun writeContainerHeader(
        out: OutputStream,
        algorithm: CipherAlgorithm,
        flags: Short,
        generationNumber: Int,
        envelopes: List<EnvelopeDescriptor>,
        encryptedMetadataIv: ByteArray,
        encryptedMetadataCiphertext: ByteArray
    ): ByteArray {
        val dos = DataOutputStream(out)

        // 1. Magic (11 bytes)
        dos.write(ContainerConstants.MAGIC_BYTES)

        // 2. Format version (2 bytes, big-endian)
        dos.writeShort(ContainerConstants.FORMAT_VERSION.toInt())

        // 3. Algorithm ID (1 byte)
        dos.writeByte(algorithm.id.toInt())

        // 4. Flags (2 bytes)
        dos.writeShort(flags.toInt())

        // 5. Generation number (4 bytes)
        dos.writeInt(generationNumber)

        // 6. Envelopes section
        val envelopesBaos = ByteArrayOutputStream()
        val envDos = DataOutputStream(envelopesBaos)
        envDos.writeByte(envelopes.size)
        for (env in envelopes) {
            envDos.writeByte(env.envelopeType.toInt())
            envDos.writeByte(env.kdfType.toInt())
            envDos.writeByte(env.salt.size)
            envDos.write(env.salt)
            envDos.writeInt(env.iterations)
            envDos.writeInt(env.memoryCostKb)
            envDos.writeByte(env.parallelism)
            envDos.writeByte(env.iv.size)
            envDos.write(env.iv)
            envDos.writeShort(env.wrappedDek.size)
            envDos.write(env.wrappedDek)
        }
        val envelopesBytes = envelopesBaos.toByteArray()

        dos.writeInt(envelopesBytes.size)
        dos.write(envelopesBytes)

        // 7. Encrypted Private Metadata section
        dos.writeByte(encryptedMetadataIv.size)
        dos.write(encryptedMetadataIv)
        dos.writeInt(encryptedMetadataCiphertext.size)
        dos.write(encryptedMetadataCiphertext)

        dos.flush()

        // Canonical AAD binds magic, version, algorithm, flags, generation, and envelopes
        val aadBaos = ByteArrayOutputStream()
        val aadDos = DataOutputStream(aadBaos)
        aadDos.write(ContainerConstants.MAGIC_BYTES)
        aadDos.writeShort(ContainerConstants.FORMAT_VERSION.toInt())
        aadDos.writeByte(algorithm.id.toInt())
        aadDos.writeShort(flags.toInt())
        aadDos.writeInt(generationNumber)
        aadDos.writeInt(envelopesBytes.size)
        aadDos.write(envelopesBytes)
        return aadBaos.toByteArray()
    }

    /**
     * Parses and strictly validates container header from an input stream.
     * Rejects unknown algorithms, malformed envelopes, negative or excessive lengths.
     */
    fun readContainerHeader(inputStream: InputStream): ParsedContainerHeader {
        val dis = DataInputStream(inputStream)

        // 1. Magic bytes
        val magic = ByteArray(ContainerConstants.MAGIC_LENGTH)
        dis.readFully(magic)
        if (!magic.contentEquals(ContainerConstants.MAGIC_BYTES)) {
            throw SecurityException("Invalid file format: Magic bytes do not match ADSKY-VAULT container.")
        }

        // 2. Format version
        val version = dis.readShort()
        if (version != ContainerConstants.FORMAT_VERSION) {
            throw SecurityException("Unsupported container format version: $version (expected ${ContainerConstants.FORMAT_VERSION})")
        }

        // 3. Algorithm ID
        val algoId = dis.readByte()
        val algorithm = CipherAlgorithm.fromId(algoId)
            ?: throw SecurityException("Unsupported payload encryption algorithm ID: 0x%02X".format(algoId))

        // 4. Flags
        val flags = dis.readShort()

        // 5. Generation number
        val generationNumber = dis.readInt()
        if (generationNumber < 1) {
            throw SecurityException("Invalid generation number: $generationNumber (must be positive)")
        }

        // 6. Envelopes
        val envelopesLength = dis.readInt()
        if (envelopesLength <= 0 || envelopesLength > 32 * 1024) {
            throw SecurityException("Invalid envelope payload length: $envelopesLength")
        }
        val envelopesBytes = ByteArray(envelopesLength)
        dis.readFully(envelopesBytes)

        val envDis = DataInputStream(envelopesBytes.inputStream())
        val envelopeCount = envDis.readByte().toInt() and 0xFF
        if (envelopeCount <= 0 || envelopeCount > ContainerConstants.MAX_ENVELOPES_COUNT) {
            throw SecurityException("Invalid envelope count: $envelopeCount")
        }

        val envelopes = mutableListOf<EnvelopeDescriptor>()
        for (i in 0 until envelopeCount) {
            val envType = envDis.readByte()
            val kdfType = envDis.readByte()

            val saltLen = envDis.readByte().toInt() and 0xFF
            if (saltLen > ContainerConstants.MAX_SALT_LENGTH) {
                throw SecurityException("Salt length exceeds bounds: $saltLen")
            }
            val salt = ByteArray(saltLen)
            envDis.readFully(salt)

            val iterations = envDis.readInt()
            val memoryCostKb = envDis.readInt()
            val parallelism = envDis.readByte().toInt() and 0xFF

            val ivLen = envDis.readByte().toInt() and 0xFF
            if (ivLen > ContainerConstants.MAX_IV_LENGTH) {
                throw SecurityException("IV length exceeds bounds: $ivLen")
            }
            val iv = ByteArray(ivLen)
            envDis.readFully(iv)

            val wrappedKeyLen = envDis.readShort().toInt() and 0xFFFF
            if (wrappedKeyLen <= 0 || wrappedKeyLen > ContainerConstants.MAX_WRAPPED_KEY_LENGTH) {
                throw SecurityException("Wrapped key length exceeds bounds: $wrappedKeyLen")
            }
            val wrappedDek = ByteArray(wrappedKeyLen)
            envDis.readFully(wrappedDek)

            envelopes.add(
                EnvelopeDescriptor(
                    envelopeType = envType,
                    kdfType = kdfType,
                    salt = salt,
                    iterations = iterations,
                    memoryCostKb = memoryCostKb,
                    parallelism = parallelism,
                    iv = iv,
                    wrappedDek = wrappedDek
                )
            )
        }

        // 7. Encrypted Private Metadata
        val metaIvLen = dis.readByte().toInt() and 0xFF
        if (metaIvLen <= 0 || metaIvLen > ContainerConstants.MAX_IV_LENGTH) {
            throw SecurityException("Metadata IV length exceeds bounds: $metaIvLen")
        }
        val metaIv = ByteArray(metaIvLen)
        dis.readFully(metaIv)

        val metaCiphertextLen = dis.readInt()
        if (metaCiphertextLen <= 0 || metaCiphertextLen > ContainerConstants.MAX_METADATA_LENGTH) {
            throw SecurityException("Metadata ciphertext length exceeds bounds: $metaCiphertextLen")
        }
        val metaCiphertext = ByteArray(metaCiphertextLen)
        dis.readFully(metaCiphertext)

        // Compute canonical AAD for authentication
        val aadBaos = ByteArrayOutputStream()
        val aadDos = DataOutputStream(aadBaos)
        aadDos.write(ContainerConstants.MAGIC_BYTES)
        aadDos.writeShort(version.toInt())
        aadDos.writeByte(algoId.toInt())
        aadDos.writeShort(flags.toInt())
        aadDos.writeInt(generationNumber)
        aadDos.writeInt(envelopesBytes.size)
        aadDos.write(envelopesBytes)

        return ParsedContainerHeader(
            formatVersion = version,
            algorithm = algorithm,
            flags = flags,
            generationNumber = generationNumber,
            envelopes = envelopes,
            encryptedMetadataIv = metaIv,
            encryptedMetadataCiphertext = metaCiphertext,
            canonicalHeaderAad = aadBaos.toByteArray()
        )
    }

    /**
     * Builds AAD for payload chunks:
     * MAGIC + version + algoId + chunkIndex + recordType
     */
    fun buildChunkAad(
        version: Short,
        algoId: Byte,
        chunkIndex: Int,
        recordType: Byte
    ): ByteArray {
        val buffer = ByteBuffer.allocate(ContainerConstants.MAGIC_LENGTH + 2 + 1 + 4 + 1)
        buffer.order(ByteOrder.BIG_ENDIAN)
        buffer.put(ContainerConstants.MAGIC_BYTES)
        buffer.putShort(version)
        buffer.put(algoId)
        buffer.putInt(chunkIndex)
        buffer.put(recordType)
        return buffer.array()
    }

    /**
     * Builds AAD for the final record:
     * MAGIC + "FINAL" + totalChunks + totalBytes
     */
    fun buildFinalRecordAad(
        totalChunks: Int,
        totalBytes: Long
    ): ByteArray {
        val finalTag = "FINAL".toByteArray(StandardCharsets.US_ASCII)
        val buffer = ByteBuffer.allocate(ContainerConstants.MAGIC_LENGTH + finalTag.size + 4 + 8)
        buffer.order(ByteOrder.BIG_ENDIAN)
        buffer.put(ContainerConstants.MAGIC_BYTES)
        buffer.put(finalTag)
        buffer.putInt(totalChunks)
        buffer.putLong(totalBytes)
        return buffer.array()
    }
}
