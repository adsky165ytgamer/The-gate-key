package com.example.crypto

import java.nio.charset.StandardCharsets

object ContainerConstants {
    val MAGIC_BYTES = "ADSKY-VAULT".toByteArray(StandardCharsets.US_ASCII)
    const val MAGIC_LENGTH = 11
    const val FORMAT_VERSION: Short = 1

    // Envelope Types
    const val ENVELOPE_CUSTOM_CREDENTIAL: Byte = 0x01
    const val ENVELOPE_DEVICE_KEYSTORE: Byte = 0x02
    const val ENVELOPE_RECOVERY_SECRET: Byte = 0x03

    // KDF Algorithm Types
    const val KDF_ARGON2ID: Byte = 0x01
    const val KDF_KEYSTORE_DIRECT: Byte = 0x02
    const val KDF_RECOVERY_ARGON2ID: Byte = 0x03

    // Record Types
    const val RECORD_PAYLOAD_CHUNK: Byte = 0x10
    const val RECORD_FINAL: Byte = 0xFF.toByte()

    // Container Flags (16-bit bitmask)
    const val FLAG_HAS_CUSTOM_CREDENTIAL: Short = 0x0001
    const val FLAG_HAS_DEVICE_KEYSTORE: Short = 0x0002
    const val FLAG_HAS_RECOVERY_SECRET: Short = 0x0004
    const val FLAG_IS_SIGNED: Short = 0x0008
    const val FLAG_CUSTOM_SIGNATURE_IDENTITY: Short = 0x0010

    // Nominal streaming chunk size (1 MiB)
    const val CHUNK_SIZE = 1024 * 1024

    // Bounds for validation (Strict limits to prevent unbounded allocations)
    const val MAX_ENVELOPES_COUNT = 8
    const val MAX_SALT_LENGTH = 128
    const val MAX_IV_LENGTH = 64
    const val MAX_WRAPPED_KEY_LENGTH = 256
    const val MAX_METADATA_LENGTH = 64 * 1024 // 64 KiB
    const val MAX_SIGNATURE_LENGTH = 2048
    const val MAX_PUBLIC_KEY_LENGTH = 2048
    const val MAX_CHUNK_PAYLOAD_SIZE = 2 * 1024 * 1024 // 2 MiB max per chunk
}
