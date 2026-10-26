# AD Sky Vault — Technical Architecture & Deployment Manual

## 1. Overview & Security Model
**AD Sky Vault** is a 100% offline-first Android file encryption, verification, and migration system. It enforces:
- **Zero Network Footprint**: The application manifest requests no `android.permission.INTERNET`. No remote servers, analytics, cloud databases, or telemetry exist.
- **Envelope Encryption**: Every encrypted container receives a fresh, cryptographically secure 256-bit Data Encryption Key (DEK). The DEK is never stored in plaintext and is wrapped inside independent authorization envelopes.
- **Authenticated Binary Containers (`.adsky`)**: Bounded binary format featuring canonical header authentication, streaming chunked payloads, authenticated final integrity records, and digital signature records.
- **Hardware Keystore Binding**: Original-device envelope wraps the file DEK with a 256-bit AES key residing inside `AndroidKeyStore`. Probes hardware security level (StrongBox / TEE vs Software sandbox).
- **Argon2id Key Derivation**: Custom user credentials and recovery keys derive wrapping keys via Argon2id (iterations=3, memory=64MB, parallelism=2, random 16-byte salt).
- **Disaster Recovery & Migration**: Master installation recovery secret (`ADSKY-XXXX-XXXX-...`) allows decrypting and re-encrypting containers across devices without depending on Device A's hardware Keystore.

---

## 2. Cryptographic Specifications

| Component | Specification | Implementation |
|---|---|---|
| **Payload Algorithm 1** | AES-256-GCM (12-byte nonce, 128-bit auth tag) | Google Tink Public AEAD (`AesGcmKey`, `KeysetHandle`) |
| **Payload Algorithm 2** | XChaCha20-Poly1305 (24-byte nonce, 128-bit MAC) | Google Tink Public AEAD (`XChaCha20Poly1305Key`, `KeysetHandle`) |
| **Key Derivation** | Argon2id (RFC 9106, version 0x13) | Bouncy Castle (`Argon2BytesGenerator`) |
| **Device Binding** | Hardware-backed AES-256-GCM | `AndroidKeyStore` (`adsky_vault_device_master_key`) |
| **Digital Signing** | ECDSA P-256 (secp256r1) with SHA-256 | `AndroidKeyStore` (`adsky_vault_signing_key_*`) |
| **Storage Access** | Android Storage Access Framework (SAF) | `ACTION_OPEN_DOCUMENT`, `ACTION_CREATE_DOCUMENT` |

---

## 3. Custom `.adsky` Binary Container Layout

```
+-------------------------------------------------------------------+
| MAGIC: "ADSKY-VAULT" (11 bytes ASCII)                             |
| Format Version: 0x0001 (uint16)                                   |
| Algorithm ID: 0x01 (AES-GCM) or 0x02 (XChaCha20-Poly1305) (uint8) |
| Container Flags: Bitmask (uint16)                                 |
| Generation Number: Monotonic uint32                              |
| Envelopes Section Length: uint32                                  |
|   - Envelope Count: uint8                                         |
|   - For each envelope: Type, KDF Type, Salt, Params, IV, WrappedDEK|
| Encrypted Private Metadata:                                       |
|   - IV Length (uint8) + IV + Ciphertext Length (uint32) + EncBytes|
+-------------------------------------------------------------------+
| Payload Chunks (Streaming, nominal 1 MiB):                        |
|   - Record Type: 0x10 (uint8)                                     |
|   - Chunk Index: Monotonic uint32 (0, 1, 2, ...)                  |
|   - Nonce Length (uint8) + Nonce (12B or 24B)                     |
|   - Ciphertext Length (uint32) + Ciphertext + Auth Tag            |
|   - AAD = MAGIC + Version + AlgoID + ChunkIndex + RecordType      |
+-------------------------------------------------------------------+
| Authenticated Final Record:                                       |
|   - Record Type: 0xFF (uint8)                                     |
|   - Total Chunks: uint32                                          |
|   - Total Plaintext Byte Count: uint64                            |
|   - IV + Encrypted Verification Token ("ADSKY-PAYLOAD-COMPLETE")  |
+-------------------------------------------------------------------+
| Digital Signature Record (Optional / Required per flag):          |
|   - Public Key Length (uint16) + X.509 EC Public Key Bytes        |
|   - Signature Length (uint16) + ECDSA P-256 Signature Bytes       |
|   (Signature covers SHA-256 digest from byte 0 to Final Record)   |
+-------------------------------------------------------------------+
```

---

## 4. Build & Installation Instructions

### Prerequisites
- JDK 17 or JDK 21
- Android SDK Platform 35 / 36
- Gradle 8.11+ / AGP 9.1.1

### Build APK
To compile and assemble the debug APK:
```bash
gradle assembleDebug
```
The compiled APK is generated at:
`releases/adsky-vault-v1.0.0.apk`

### Run Automated Unit Tests
```bash
gradle :app:testDebugUnitTest
```

### Install onto Device via ADB
```bash
adb install -r releases/adsky-vault-v1.0.0.apk
```
