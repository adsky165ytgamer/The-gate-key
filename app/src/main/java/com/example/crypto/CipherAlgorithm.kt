package com.example.crypto

enum class CipherAlgorithm(
    val id: Byte,
    val displayName: String,
    val description: String,
    val keySizeBits: Int = 256,
    val nonceSizeBytes: Int,
    val tagSizeBytes: Int = 16
) {
    AES_256_GCM(
        id = 0x01,
        displayName = "AES-256-GCM",
        description = "Galois/Counter Mode with 256-bit key, 12-byte unique nonce, and 128-bit authentication tag.",
        nonceSizeBytes = 12
    ),
    XCHACHA20_POLY1305(
        id = 0x02,
        displayName = "XChaCha20-Poly1305",
        description = "Extended-nonce ChaCha20 with 256-bit key, 24-byte extended nonce, and Poly1305 128-bit MAC.",
        nonceSizeBytes = 24
    );

    companion object {
        fun fromId(id: Byte): CipherAlgorithm? {
            return entries.firstOrNull { it.id == id }
        }
    }
}
