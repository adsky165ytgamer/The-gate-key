package com.example.crypto

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyInfo
import android.security.keystore.KeyProperties
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.MessageDigest
import java.security.PublicKey
import java.security.Signature
import java.security.spec.ECGenParameterSpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

data class HardwareSecurityInfo(
    val hasKey: Boolean,
    val isInsideSecureHardware: Boolean,
    val securityLevelName: String,
    val description: String
)

object KeystoreManager {
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    const val DEVICE_MASTER_KEY_ALIAS = "adsky_vault_device_master_key"
    const val DEFAULT_SIGNING_KEY_ALIAS = "adsky_vault_signing_key_default"
    const val CUSTOM_SIGNING_KEY_ALIAS = "adsky_vault_signing_key_custom"

    private val keyStore: KeyStore by lazy {
        KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
    }

    fun getOrCreateDeviceMasterKey(): SecretKey {
        if (!keyStore.containsAlias(DEVICE_MASTER_KEY_ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEYSTORE
            )
            val builder = KeyGenParameterSpec.Builder(
                DEVICE_MASTER_KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setKeySize(256)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)

            keyGenerator.init(builder.build())
            keyGenerator.generateKey()
        }
        val entry = keyStore.getEntry(DEVICE_MASTER_KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
        return entry?.secretKey ?: error("Failed to retrieve Keystore device master key")
    }

    fun queryHardwareSecurity(): HardwareSecurityInfo {
        return try {
            val secretKey = getOrCreateDeviceMasterKey()
            val factory = javax.crypto.SecretKeyFactory.getInstance(
                secretKey.algorithm,
                ANDROID_KEYSTORE
            )
            val keyInfo = factory.getKeySpec(secretKey, KeyInfo::class.java) as KeyInfo
            val isSecure = keyInfo.isInsideSecureHardware

            val (levelName, desc) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                when (keyInfo.securityLevel) {
                    KeyProperties.SECURITY_LEVEL_STRONGBOX ->
                        "StrongBox Keymaster" to "Dedicated hardware security chip (StrongBox) with separate CPU and storage."
                    KeyProperties.SECURITY_LEVEL_TRUSTED_ENVIRONMENT ->
                        "Hardware TEE" to "Hardware-isolated Trusted Execution Environment (ARM TrustZone / TEE)."
                    KeyProperties.SECURITY_LEVEL_SOFTWARE ->
                        "Software Fallback" to "Software-backed emulation. Key material is guarded by Android OS sandbox."
                    else -> if (isSecure) "Hardware Protected" to "Hardware-isolated cryptographic processor."
                    else "Software Emulation" to "Software key storage on this device."
                }
            } else {
                if (isSecure) {
                    "Hardware TEE / Keymaster" to "Hardware-isolated secure world (TEE/Keymaster)."
                } else {
                    "Software Keymaster" to "Android software keymaster implementation."
                }
            }

            HardwareSecurityInfo(
                hasKey = true,
                isInsideSecureHardware = isSecure,
                securityLevelName = levelName,
                description = desc
            )
        } catch (e: Exception) {
            HardwareSecurityInfo(
                hasKey = false,
                isInsideSecureHardware = false,
                securityLevelName = "Unavailable",
                description = "Could not probe Keystore security level: ${e.message}"
            )
        }
    }

    fun wrapDekWithDeviceKey(dek: ByteArray, associatedData: ByteArray): Pair<ByteArray, ByteArray> {
        val secretKey = getOrCreateDeviceMasterKey()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        if (associatedData.isNotEmpty()) {
            cipher.updateAAD(associatedData)
        }
        val wrappedDek = cipher.doFinal(dek)
        val iv = cipher.iv
        return Pair(iv, wrappedDek)
    }

    fun unwrapDekWithDeviceKey(
        iv: ByteArray,
        wrappedDek: ByteArray,
        associatedData: ByteArray
    ): ByteArray {
        val secretKey = getOrCreateDeviceMasterKey()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
        if (associatedData.isNotEmpty()) {
            cipher.updateAAD(associatedData)
        }
        return cipher.doFinal(wrappedDek)
    }

    fun getOrCreateSigningKeyPair(isCustom: Boolean): java.security.KeyPair {
        val alias = if (isCustom) CUSTOM_SIGNING_KEY_ALIAS else DEFAULT_SIGNING_KEY_ALIAS
        if (!keyStore.containsAlias(alias)) {
            val kpg = KeyPairGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_EC,
                ANDROID_KEYSTORE
            )
            val builder = KeyGenParameterSpec.Builder(
                alias,
                KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
            )
                .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
                .setDigests(KeyProperties.DIGEST_SHA256)
                .setUserAuthenticationRequired(false)

            kpg.initialize(builder.build())
            kpg.generateKeyPair()
        }
        val entry = keyStore.getEntry(alias, null) as? KeyStore.PrivateKeyEntry
            ?: error("Signing key entry missing for $alias")
        return java.security.KeyPair(entry.certificate.publicKey, entry.privateKey)
    }

    fun computePublicKeyFingerprint(publicKeyBytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(publicKeyBytes)
        return digest.joinToString("") { "%02X".format(it) }
    }

    fun signData(data: ByteArray, isCustom: Boolean): ByteArray {
        val keyPair = getOrCreateSigningKeyPair(isCustom)
        val signature = Signature.getInstance("SHA256withECDSA")
        signature.initSign(keyPair.private)
        signature.update(data)
        return signature.sign()
    }

    fun verifySignature(publicKeyBytes: ByteArray, data: ByteArray, signatureBytes: ByteArray): Boolean {
        return try {
            val keySpec = X509EncodedKeySpec(publicKeyBytes)
            val kf = KeyFactory.getInstance("EC")
            val publicKey: PublicKey = kf.generatePublic(keySpec)
            val signature = Signature.getInstance("SHA256withECDSA")
            signature.initVerify(publicKey)
            signature.update(data)
            signature.verify(signatureBytes)
        } catch (e: Exception) {
            false
        }
    }

    fun isValidCustomIdentity(label: String): Boolean {
        return label.matches(Regex("^[A-Za-z]{8}$"))
    }
}
