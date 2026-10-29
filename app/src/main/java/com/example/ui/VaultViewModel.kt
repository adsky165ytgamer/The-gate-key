package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.crypto.CipherAlgorithm
import com.example.crypto.ContainerConstants
import com.example.crypto.DecryptionResult
import com.example.crypto.HardwareSecurityInfo
import com.example.crypto.KeystoreManager
import com.example.crypto.ParsedContainerHeader
import com.example.crypto.RecoveryKeyManager
import com.example.crypto.SignatureMode
import com.example.crypto.SkyVaultEngine
import com.example.data.AppDatabase
import com.example.data.AuditLogEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

enum class VaultScreen {
    HOME,
    SECURITY_AND_ENCRYPTION,
    ENCRYPT,
    DECRYPT,
    BATCH,
    RECOVERY_KEY,
    DEVICE_SECURITY,
    ABOUT
}

enum class SecurityTab(val title: String) {
    ENCRYPT("Encrypt"),
    DECRYPT("Decrypt"),
    BATCH("Batch"),
    RECOVERY("Recovery"),
    MIGRATION("Migration"),
    AUDIT("Audit & Device")
}

enum class DecryptAuthMode {
    DEVICE_KEYSTORE,
    CUSTOM_CREDENTIAL,
    RECOVERY_SECRET
}

class VaultViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    val auditLogs: StateFlow<List<AuditLogEntry>> = db.auditLogDao().getAllLogs()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Navigation
    private val _currentScreen = MutableStateFlow(VaultScreen.HOME)
    val currentScreen: StateFlow<VaultScreen> = _currentScreen.asStateFlow()

    // Processing & Progress
    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _progressFraction = MutableStateFlow(0f)
    val progressFraction: StateFlow<Float> = _progressFraction.asStateFlow()

    private val _statusMessage = MutableStateFlow("")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    private val _bytesProcessed = MutableStateFlow(0L)
    val bytesProcessed: StateFlow<Long> = _bytesProcessed.asStateFlow()

    private val _totalBytes = MutableStateFlow(0L)
    val totalBytes: StateFlow<Long> = _totalBytes.asStateFlow()

    private val _currentChunk = MutableStateFlow(0)
    val currentChunk: StateFlow<Int> = _currentChunk.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _operationSuccessMessage = MutableStateFlow<String?>(null)
    val operationSuccessMessage: StateFlow<String?> = _operationSuccessMessage.asStateFlow()

    private var activeJob: Job? = null
    private var isCancelledFlag = false

    // Hardware Security
    private val _hardwareSecurity = MutableStateFlow(KeystoreManager.queryHardwareSecurity())
    val hardwareSecurity: StateFlow<HardwareSecurityInfo> = _hardwareSecurity.asStateFlow()

    // --- ENCRYPTION STATE ---
    private val _encryptSourceUri = MutableStateFlow<Uri?>(null)
    val encryptSourceUri: StateFlow<Uri?> = _encryptSourceUri.asStateFlow()

    private val _encryptSourceInfo = MutableStateFlow<Pair<String, Long>?>(null)
    val encryptSourceInfo: StateFlow<Pair<String, Long>?> = _encryptSourceInfo.asStateFlow()

    private val _selectedAlgorithm = MutableStateFlow(CipherAlgorithm.AES_256_GCM)
    val selectedAlgorithm: StateFlow<CipherAlgorithm> = _selectedAlgorithm.asStateFlow()

    private val _customCredentialInput = MutableStateFlow("")
    val customCredentialInput: StateFlow<String> = _customCredentialInput.asStateFlow()

    private val _enableDeviceKeystore = MutableStateFlow(true)
    val enableDeviceKeystore: StateFlow<Boolean> = _enableDeviceKeystore.asStateFlow()

    private val _enableRecoverySecret = MutableStateFlow(true)
    val enableRecoverySecret: StateFlow<Boolean> = _enableRecoverySecret.asStateFlow()

    private val _signatureMode = MutableStateFlow(SignatureMode.DEFAULT_HIDDEN)
    val signatureMode: StateFlow<SignatureMode> = _signatureMode.asStateFlow()

    private val _customSignatureLabel = MutableStateFlow("SECVAULT")
    val customSignatureLabel: StateFlow<String> = _customSignatureLabel.asStateFlow()

    private val _encryptedResultFile = MutableStateFlow<File?>(null)
    val encryptedResultFile: StateFlow<File?> = _encryptedResultFile.asStateFlow()

    // --- DECRYPTION STATE ---
    private val _decryptContainerUri = MutableStateFlow<Uri?>(null)
    val decryptContainerUri: StateFlow<Uri?> = _decryptContainerUri.asStateFlow()

    private val _inspectedHeader = MutableStateFlow<ParsedContainerHeader?>(null)
    val inspectedHeader: StateFlow<ParsedContainerHeader?> = _inspectedHeader.asStateFlow()

    private val _inspectedEnvelopes = MutableStateFlow<List<String>>(emptyList())
    val inspectedEnvelopes: StateFlow<List<String>> = _inspectedEnvelopes.asStateFlow()

    private val _decryptAuthMode = MutableStateFlow(DecryptAuthMode.DEVICE_KEYSTORE)
    val decryptAuthMode: StateFlow<DecryptAuthMode> = _decryptAuthMode.asStateFlow()

    private val _decryptCredentialInput = MutableStateFlow("")
    val decryptCredentialInput: StateFlow<String> = _decryptCredentialInput.asStateFlow()

    private val _decryptRecoverySecretInput = MutableStateFlow("")
    val decryptRecoverySecretInput: StateFlow<String> = _decryptRecoverySecretInput.asStateFlow()

    private val _decryptedResult = MutableStateFlow<DecryptionResult?>(null)
    val decryptedResult: StateFlow<DecryptionResult?> = _decryptedResult.asStateFlow()

    // --- RECOVERY KEY SCREEN STATE ---
    private val _isRecoverySecretRevealed = MutableStateFlow(false)
    val isRecoverySecretRevealed: StateFlow<Boolean> = _isRecoverySecretRevealed.asStateFlow()

    private val _revealedRecoverySecret = MutableStateFlow<String?>(null)
    val revealedRecoverySecret: StateFlow<String?> = _revealedRecoverySecret.asStateFlow()

    // --- BATCH STATE ---
    private val _batchUris = MutableStateFlow<List<Uri>>(emptyList())
    val batchUris: StateFlow<List<Uri>> = _batchUris.asStateFlow()

    private val _batchCompletedCount = MutableStateFlow(0)
    val batchCompletedCount: StateFlow<Int> = _batchCompletedCount.asStateFlow()

    // --- SECURITY & ENCRYPTION TAB STATE ---
    private val _selectedSecurityTab = MutableStateFlow(SecurityTab.ENCRYPT)
    val selectedSecurityTab: StateFlow<SecurityTab> = _selectedSecurityTab.asStateFlow()

    fun setSelectedSecurityTab(tab: SecurityTab) {
        _selectedSecurityTab.value = tab
    }

    // --- CONTAINER MIGRATION STATE ---
    private val _migrationContainerUri = MutableStateFlow<Uri?>(null)
    val migrationContainerUri: StateFlow<Uri?> = _migrationContainerUri.asStateFlow()

    private val _migrationHeader = MutableStateFlow<ParsedContainerHeader?>(null)
    val migrationHeader: StateFlow<ParsedContainerHeader?> = _migrationHeader.asStateFlow()

    private val _migrationEnvelopes = MutableStateFlow<List<String>>(emptyList())
    val migrationEnvelopes: StateFlow<List<String>> = _migrationEnvelopes.asStateFlow()

    private val _migrationAuthMode = MutableStateFlow(DecryptAuthMode.DEVICE_KEYSTORE)
    val migrationAuthMode: StateFlow<DecryptAuthMode> = _migrationAuthMode.asStateFlow()

    private val _migrationAuthCredential = MutableStateFlow("")
    val migrationAuthCredential: StateFlow<String> = _migrationAuthCredential.asStateFlow()

    private val _migrationAuthRecoverySecret = MutableStateFlow("")
    val migrationAuthRecoverySecret: StateFlow<String> = _migrationAuthRecoverySecret.asStateFlow()

    private val _migrationTargetAlgorithm = MutableStateFlow(CipherAlgorithm.AES_256_GCM)
    val migrationTargetAlgorithm: StateFlow<CipherAlgorithm> = _migrationTargetAlgorithm.asStateFlow()

    private val _migrationNewCredential = MutableStateFlow("")
    val migrationNewCredential: StateFlow<String> = _migrationNewCredential.asStateFlow()

    private val _migrationRebindKeystore = MutableStateFlow(true)
    val migrationRebindKeystore: StateFlow<Boolean> = _migrationRebindKeystore.asStateFlow()

    private val _migrationEnableRecovery = MutableStateFlow(true)
    val migrationEnableRecovery: StateFlow<Boolean> = _migrationEnableRecovery.asStateFlow()

    private val _migrationResultFile = MutableStateFlow<File?>(null)
    val migrationResultFile: StateFlow<File?> = _migrationResultFile.asStateFlow()

    init {
        refreshHardwareSecurity()
    }

    fun navigateTo(screen: VaultScreen) {
        when (screen) {
            VaultScreen.ENCRYPT -> {
                _selectedSecurityTab.value = SecurityTab.ENCRYPT
                _currentScreen.value = VaultScreen.SECURITY_AND_ENCRYPTION
            }
            VaultScreen.DECRYPT -> {
                _selectedSecurityTab.value = SecurityTab.DECRYPT
                _currentScreen.value = VaultScreen.SECURITY_AND_ENCRYPTION
            }
            VaultScreen.BATCH -> {
                _selectedSecurityTab.value = SecurityTab.BATCH
                _currentScreen.value = VaultScreen.SECURITY_AND_ENCRYPTION
            }
            VaultScreen.RECOVERY_KEY -> {
                _selectedSecurityTab.value = SecurityTab.RECOVERY
                _currentScreen.value = VaultScreen.SECURITY_AND_ENCRYPTION
            }
            VaultScreen.DEVICE_SECURITY -> {
                _selectedSecurityTab.value = SecurityTab.AUDIT
                _currentScreen.value = VaultScreen.SECURITY_AND_ENCRYPTION
            }
            else -> {
                _currentScreen.value = screen
            }
        }
        _errorMessage.value = null
        _operationSuccessMessage.value = null
    }

    fun refreshHardwareSecurity() {
        _hardwareSecurity.value = KeystoreManager.queryHardwareSecurity()
    }

    // Setters for Encrypt Screen
    fun setEncryptSourceUri(uri: Uri?) {
        _encryptSourceUri.value = uri
        if (uri != null) {
            _encryptSourceInfo.value = SkyVaultEngine.getFileInfo(getApplication(), uri)
        } else {
            _encryptSourceInfo.value = null
        }
        _encryptedResultFile.value = null
    }

    fun setAlgorithm(algorithm: CipherAlgorithm) {
        _selectedAlgorithm.value = algorithm
    }

    fun setCustomCredentialInput(input: String) {
        _customCredentialInput.value = input
    }

    fun setEnableDeviceKeystore(enable: Boolean) {
        _enableDeviceKeystore.value = enable
    }

    fun setEnableRecoverySecret(enable: Boolean) {
        _enableRecoverySecret.value = enable
    }

    fun setSignatureMode(mode: SignatureMode) {
        _signatureMode.value = mode
    }

    fun setCustomSignatureLabel(label: String) {
        // Enforce max 8 alphabetic chars uppercase
        val filtered = label.filter { it.isLetter() }.take(8).uppercase()
        _customSignatureLabel.value = filtered
    }

    // Setters for Decrypt Screen
    fun setDecryptContainerUri(uri: Uri?) {
        _decryptContainerUri.value = uri
        _decryptedResult.value = null
        if (uri != null) {
            try {
                val (header, envs) = SkyVaultEngine.inspectContainerHeader(getApplication(), uri)
                _inspectedHeader.value = header
                _inspectedEnvelopes.value = envs
                // Default auth mode to device keystore if present, else credential or recovery
                val hasDevice = header.envelopes.any { it.envelopeType == ContainerConstants.ENVELOPE_DEVICE_KEYSTORE }
                val hasCred = header.envelopes.any { it.envelopeType == ContainerConstants.ENVELOPE_CUSTOM_CREDENTIAL }
                _decryptAuthMode.value = when {
                    hasDevice -> DecryptAuthMode.DEVICE_KEYSTORE
                    hasCred -> DecryptAuthMode.CUSTOM_CREDENTIAL
                    else -> DecryptAuthMode.RECOVERY_SECRET
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to inspect container header: ${e.message}"
                _inspectedHeader.value = null
                _inspectedEnvelopes.value = emptyList()
            }
        } else {
            _inspectedHeader.value = null
            _inspectedEnvelopes.value = emptyList()
        }
    }

    fun setDecryptAuthMode(mode: DecryptAuthMode) {
        _decryptAuthMode.value = mode
    }

    fun setDecryptCredentialInput(input: String) {
        _decryptCredentialInput.value = input
    }

    fun setDecryptRecoverySecretInput(input: String) {
        _decryptRecoverySecretInput.value = input
    }

    fun revealRecoverySecret() {
        val secret = RecoveryKeyManager.getOrCreateInstallationRecoverySecret(getApplication())
        _revealedRecoverySecret.value = secret
        _isRecoverySecretRevealed.value = true
        viewModelScope.launch(Dispatchers.IO) {
            db.auditLogDao().insertLog(
                AuditLogEntry(
                    operation = "KEY_VIEW",
                    filename = "N/A",
                    algorithm = "N/A",
                    envelopeUsed = "Recovery Keymaster",
                    status = "SUCCESS",
                    details = "Master recovery secret viewed by user after confirmation."
                )
            )
        }
    }

    fun hideRecoverySecret() {
        _isRecoverySecretRevealed.value = false
        _revealedRecoverySecret.value = null
    }

    fun cancelOperation() {
        isCancelledFlag = true
        activeJob?.cancel()
        _isProcessing.value = false
        _statusMessage.value = "Operation cancelled."
    }

    fun clearMessages() {
        _errorMessage.value = null
        _operationSuccessMessage.value = null
    }

    fun startEncryption() {
        val uri = _encryptSourceUri.value ?: return
        _isProcessing.value = true
        _progressFraction.value = 0f
        _statusMessage.value = "Initializing cryptographic envelopes..."
        _errorMessage.value = null
        _operationSuccessMessage.value = null
        isCancelledFlag = false

        activeJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                val credChars = if (_customCredentialInput.value.isNotEmpty()) {
                    _customCredentialInput.value.toCharArray()
                } else null

                val resultFile = SkyVaultEngine.encryptFile(
                    context = getApplication(),
                    sourceUri = uri,
                    algorithm = _selectedAlgorithm.value,
                    customCredential = credChars,
                    enableDeviceKeystore = _enableDeviceKeystore.value,
                    enableRecoverySecret = _enableRecoverySecret.value,
                    signatureMode = _signatureMode.value,
                    customIdentityLabel = _customSignatureLabel.value,
                    progressCallback = { bytes, total, chunk, msg ->
                        _bytesProcessed.value = bytes
                        _totalBytes.value = total
                        _currentChunk.value = chunk
                        _statusMessage.value = msg
                        if (total > 0) {
                            _progressFraction.value = (bytes.toFloat() / total.toFloat()).coerceIn(0f, 1f)
                        }
                    },
                    isCancelled = { isCancelledFlag }
                )

                _encryptedResultFile.value = resultFile
                _operationSuccessMessage.value = "File encrypted & verified successfully! (${resultFile.length() / 1024} KB container created)"
            } catch (e: Exception) {
                if (e !is InterruptedException) {
                    _errorMessage.value = "Encryption failed: ${e.message}"
                }
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun startDecryption() {
        val uri = _decryptContainerUri.value ?: return
        _isProcessing.value = true
        _progressFraction.value = 0f
        _statusMessage.value = "Authenticating container & envelopes..."
        _errorMessage.value = null
        _operationSuccessMessage.value = null
        isCancelledFlag = false

        activeJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                val credChars = if (_decryptAuthMode.value == DecryptAuthMode.CUSTOM_CREDENTIAL) {
                    _decryptCredentialInput.value.toCharArray()
                } else null

                val recSecret = if (_decryptAuthMode.value == DecryptAuthMode.RECOVERY_SECRET) {
                    _decryptRecoverySecretInput.value
                } else null

                val result = SkyVaultEngine.decryptFile(
                    context = getApplication(),
                    containerUri = uri,
                    customCredential = credChars,
                    recoverySecretString = recSecret,
                    progressCallback = { bytes, total, chunk, msg ->
                        _bytesProcessed.value = bytes
                        _totalBytes.value = total
                        _currentChunk.value = chunk
                        _statusMessage.value = msg
                        if (total > 0) {
                            _progressFraction.value = (bytes.toFloat() / total.toFloat()).coerceIn(0f, 1f)
                        }
                    },
                    isCancelled = { isCancelledFlag }
                )

                _decryptedResult.value = result
                _operationSuccessMessage.value = "Decryption & integrity verification succeeded for '${result.metadata.originalFilename}'!"
            } catch (e: Exception) {
                if (e !is InterruptedException) {
                    _errorMessage.value = "Decryption failed: ${e.message}"
                }
            } finally {
                _isProcessing.value = false
            }
        }
    }

    /**
     * Saves temporary encrypted or decrypted file to destination chosen by user via SAF.
     */
    fun saveResultToDestinationUri(sourceFile: File, destinationUri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                getApplication<Application>().contentResolver.openOutputStream(destinationUri)?.use { out ->
                    FileInputStream(sourceFile).use { input ->
                        input.copyTo(out)
                    }
                }
                _operationSuccessMessage.value = "File saved successfully to destination storage."
            } catch (e: Exception) {
                _errorMessage.value = "Failed to save file: ${e.message}"
            }
        }
    }

    // --- BATCH PROCESSING ---
    fun setBatchUris(uris: List<Uri>) {
        _batchUris.value = uris
        _batchCompletedCount.value = 0
    }

    fun startBatchEncryption(destinationDirSaver: (File, String) -> Unit) {
        val uris = _batchUris.value
        if (uris.isEmpty()) return

        _isProcessing.value = true
        _batchCompletedCount.value = 0
        _statusMessage.value = "Starting batch encryption of ${uris.size} files..."
        _errorMessage.value = null
        isCancelledFlag = false

        activeJob = viewModelScope.launch(Dispatchers.IO) {
            var successCount = 0
            val credChars = if (_customCredentialInput.value.isNotEmpty()) _customCredentialInput.value.toCharArray() else null

            for ((index, uri) in uris.withIndex()) {
                if (isCancelledFlag) break
                try {
                    val info = SkyVaultEngine.getFileInfo(getApplication(), uri)
                    _statusMessage.value = "Encrypting file ${index + 1}/${uris.size}: ${info.first}"
                    val resultFile = SkyVaultEngine.encryptFile(
                        context = getApplication(),
                        sourceUri = uri,
                        algorithm = _selectedAlgorithm.value,
                        customCredential = credChars,
                        enableDeviceKeystore = _enableDeviceKeystore.value,
                        enableRecoverySecret = _enableRecoverySecret.value,
                        signatureMode = _signatureMode.value,
                        customIdentityLabel = _customSignatureLabel.value,
                        progressCallback = { bytes, total, _, _ ->
                            val fileFraction = if (total > 0) bytes.toFloat() / total.toFloat() else 0f
                            _progressFraction.value = (index + fileFraction) / uris.size.toFloat()
                        },
                        isCancelled = { isCancelledFlag }
                    )
                    destinationDirSaver(resultFile, "${info.first}.adsky")
                    successCount++
                    _batchCompletedCount.value = successCount
                } catch (e: Exception) {
                    if (e is InterruptedException) break
                }
            }
            _isProcessing.value = false
            _operationSuccessMessage.value = "Batch encryption finished. Processed $successCount of ${uris.size} files."
        }
    }

    fun clearAuditLogs() {
        viewModelScope.launch(Dispatchers.IO) {
            db.auditLogDao().clearAllLogs()
        }
    }

    // --- MIGRATION METHODS ---
    fun setMigrationContainerUri(uri: Uri?) {
        _migrationContainerUri.value = uri
        _migrationResultFile.value = null
        if (uri != null) {
            try {
                val (header, envs) = SkyVaultEngine.inspectContainerHeader(getApplication(), uri)
                _migrationHeader.value = header
                _migrationEnvelopes.value = envs
                val hasDevice = header.envelopes.any { it.envelopeType == ContainerConstants.ENVELOPE_DEVICE_KEYSTORE }
                val hasCred = header.envelopes.any { it.envelopeType == ContainerConstants.ENVELOPE_CUSTOM_CREDENTIAL }
                _migrationAuthMode.value = when {
                    hasDevice -> DecryptAuthMode.DEVICE_KEYSTORE
                    hasCred -> DecryptAuthMode.CUSTOM_CREDENTIAL
                    else -> DecryptAuthMode.RECOVERY_SECRET
                }
                _migrationTargetAlgorithm.value = header.algorithm
            } catch (e: Exception) {
                _errorMessage.value = "Failed to inspect container for migration: ${e.message}"
                _migrationHeader.value = null
                _migrationEnvelopes.value = emptyList()
            }
        } else {
            _migrationHeader.value = null
            _migrationEnvelopes.value = emptyList()
        }
    }

    fun setMigrationAuthMode(mode: DecryptAuthMode) {
        _migrationAuthMode.value = mode
    }

    fun setMigrationAuthCredential(cred: String) {
        _migrationAuthCredential.value = cred
    }

    fun setMigrationAuthRecoverySecret(secret: String) {
        _migrationAuthRecoverySecret.value = secret
    }

    fun setMigrationTargetAlgorithm(algo: CipherAlgorithm) {
        _migrationTargetAlgorithm.value = algo
    }

    fun setMigrationNewCredential(cred: String) {
        _migrationNewCredential.value = cred
    }

    fun setMigrationRebindKeystore(rebind: Boolean) {
        _migrationRebindKeystore.value = rebind
    }

    fun setMigrationEnableRecovery(enable: Boolean) {
        _migrationEnableRecovery.value = enable
    }

    fun startMigration() {
        val uri = _migrationContainerUri.value ?: return
        _isProcessing.value = true
        _progressFraction.value = 0f
        _statusMessage.value = "Authenticating container for migration..."
        _errorMessage.value = null
        _operationSuccessMessage.value = null
        _migrationResultFile.value = null
        isCancelledFlag = false

        activeJob = viewModelScope.launch(Dispatchers.IO) {
            var tempDecryptedFile: File? = null
            try {
                val authCred = if (_migrationAuthMode.value == DecryptAuthMode.CUSTOM_CREDENTIAL) {
                    _migrationAuthCredential.value.toCharArray()
                } else null

                val authRecSecret = if (_migrationAuthMode.value == DecryptAuthMode.RECOVERY_SECRET) {
                    _migrationAuthRecoverySecret.value
                } else null

                val decryptResult = SkyVaultEngine.decryptFile(
                    context = getApplication(),
                    containerUri = uri,
                    customCredential = authCred,
                    recoverySecretString = authRecSecret,
                    progressCallback = { bytes, total, chunk, _ ->
                        _statusMessage.value = "Decrypting container for migration ($chunk)..."
                        if (total > 0) _progressFraction.value = (bytes.toFloat() / total.toFloat()) * 0.5f
                    },
                    isCancelled = { isCancelledFlag }
                )
                tempDecryptedFile = decryptResult.decryptedTempFile

                _statusMessage.value = "Re-encrypting and binding to generation ${decryptResult.generationNumber + 1}..."
                val newCred = if (_migrationNewCredential.value.isNotEmpty()) {
                    _migrationNewCredential.value.toCharArray()
                } else null

                val migratedFile = SkyVaultEngine.migrateContainer(
                    context = getApplication(),
                    decryptedFile = tempDecryptedFile,
                    originalMetadata = decryptResult.metadata,
                    newAlgorithm = _migrationTargetAlgorithm.value,
                    newCustomCredential = newCred,
                    enableDeviceKeystore = _migrationRebindKeystore.value,
                    enableRecoverySecret = _migrationEnableRecovery.value,
                    signatureMode = SignatureMode.DEFAULT_HIDDEN,
                    customIdentityLabel = null,
                    progressCallback = { bytes, total, chunk, _ ->
                        _statusMessage.value = "Migrating to new container envelope ($chunk)..."
                        if (total > 0) _progressFraction.value = 0.5f + (bytes.toFloat() / total.toFloat()) * 0.5f
                    },
                    isCancelled = { isCancelledFlag }
                )

                _migrationResultFile.value = migratedFile
                _operationSuccessMessage.value = "Container successfully migrated to Generation ${decryptResult.generationNumber + 1} (${migratedFile.length() / 1024} KB)!"

                db.auditLogDao().insertLog(
                    AuditLogEntry(
                        operation = "MIGRATE",
                        filename = decryptResult.metadata.originalFilename,
                        algorithm = _migrationTargetAlgorithm.value.displayName,
                        envelopeUsed = "Migration Pipeline",
                        status = "SUCCESS",
                        details = "Upgraded from Gen ${decryptResult.generationNumber} to Gen ${decryptResult.generationNumber + 1}"
                    )
                )
            } catch (e: Exception) {
                if (e !is InterruptedException) {
                    _errorMessage.value = "Migration failed: ${e.message}"
                }
            } finally {
                tempDecryptedFile?.delete()
                _isProcessing.value = false
            }
        }
    }
}
