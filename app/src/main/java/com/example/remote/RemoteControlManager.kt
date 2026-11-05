package com.example.remote

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.BuildConfig
import com.example.ui.registry.ToolCategory
import com.example.ui.registry.ToolId
import com.squareup.moshi.Moshi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

private val Context.remoteControlDataStore by preferencesDataStore(name = "remote_control")

private const val CONTROL_URL =
    "https://raw.githubusercontent.com/adsky165ytgamer/Gatekey-public-config/main/config/control.json"
private const val MAX_RESPONSE_BYTES = 512 * 1024

sealed interface RemoteControlStatus {
    data object UsingDefaults : RemoteControlStatus
    data object UsingCache : RemoteControlStatus
    data object Refreshing : RemoteControlStatus
    data object Updated : RemoteControlStatus
    data class Offline(val message: String) : RemoteControlStatus
    data class Rejected(val message: String) : RemoteControlStatus
}

class RemoteControlManager private constructor(context: Context) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val refreshMutex = Mutex()
    private val moshi = Moshi.Builder().build()
    private val adapter = moshi.adapter(RemoteConfig::class.java)
    private val _config = MutableStateFlow(RemoteConfig.defaults())
    val config: StateFlow<RemoteConfig> = _config.asStateFlow()
    private val _status = MutableStateFlow<RemoteControlStatus>(RemoteControlStatus.UsingDefaults)
    val status: StateFlow<RemoteControlStatus> = _status.asStateFlow()
    private var started = false

    fun start() {
        if (started) return
        started = true
        scope.launch {
            loadCachedConfiguration()
            refreshConfiguration()
        }
    }

    fun refresh() {
        start()
        scope.launch { refreshConfiguration() }
    }

    fun isVisible(category: ToolCategory): Boolean = isVisible(keyFor(category))

    fun isVisible(tool: ToolId): Boolean = isVisible(keyFor(tool))

    fun isAvailable(category: ToolCategory): Boolean = isAvailable(keyFor(category))

    fun isAvailable(tool: ToolId): Boolean = isAvailable(keyFor(tool))

    fun visibleTools(category: ToolCategory): List<ToolId> =
        category.tools.filter { isVisible(it) }

    fun displayLabel(key: String, fallback: String): String =
        config.value.feature(key)?.label?.takeIf { it.isNotBlank() } ?: fallback

    fun displayDescription(key: String, fallback: String): String =
        config.value.feature(key)?.description?.takeIf { it.isNotBlank() } ?: fallback

    private fun isVisible(key: String): Boolean {
        val feature = config.value.feature(key) ?: return true
        val state = RemoteFeatureState.parse(feature.state) ?: return false
        if (!feature.show) return config.value.interfaceConfig.showDisabledFeatures && state != RemoteFeatureState.DEPRECATED
        return when (state) {
            RemoteFeatureState.DEPRECATED -> config.value.interfaceConfig.showDisabledFeatures
            RemoteFeatureState.DISABLED, RemoteFeatureState.MAINTENANCE ->
                config.value.interfaceConfig.showDisabledFeatures
            RemoteFeatureState.ENABLED, RemoteFeatureState.BETA -> true
        }
    }

    private fun isAvailable(key: String): Boolean {
        if (config.value.maintenanceMode) return false
        val state = RemoteFeatureState.parse(config.value.feature(key)?.state ?: "enabled") ?: return false
        return state == RemoteFeatureState.ENABLED || state == RemoteFeatureState.BETA
    }

    private suspend fun loadCachedConfiguration() {
        val preferences = appContext.remoteControlDataStore.data.first()
        val raw = preferences[CACHED_JSON] ?: return
        try {
            val cached = adapter.fromJson(raw) ?: throw IllegalArgumentException("Empty cached configuration.")
            RemoteConfigValidator.validate(cached, BuildConfig.VERSION_CODE)
                .getOrThrow()
            _config.value = cached
            _status.value = RemoteControlStatus.UsingCache
        } catch (_: Exception) {
            // Corrupt or incompatible cache is ignored; built-in safe defaults remain active.
        }
    }

    private suspend fun refreshConfiguration() {
        refreshMutex.withLock {
            _status.value = RemoteControlStatus.Refreshing
            try {
                val downloaded = download()
                val parsed = adapter.fromJson(downloaded) ?: throw IllegalArgumentException("Empty configuration.")
                RemoteConfigValidator.validate(
                    parsed,
                    BuildConfig.VERSION_CODE,
                    _config.value.configVersion
                ).getOrThrow()
                appContext.remoteControlDataStore.edit {
                    it[CACHED_JSON] = downloaded
                }
                _config.value = parsed
                _status.value = RemoteControlStatus.Updated
            } catch (e: java.io.IOException) {
                _status.value = RemoteControlStatus.Offline(e.message ?: "Configuration is unavailable offline.")
            } catch (e: Exception) {
                _status.value = RemoteControlStatus.Rejected(e.message ?: "Configuration was rejected.")
            }
        }
    }

    private suspend fun download(): String = withContext(Dispatchers.IO) {
        val connection = (URL(CONTROL_URL).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10_000
            readTimeout = 10_000
            instanceFollowRedirects = false
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Cache-Control", "no-cache")
        }
        try {
            if (connection.url.protocol != "https" || connection.url.host != "raw.githubusercontent.com") {
                throw SecurityException("Configuration endpoint is not trusted.")
            }
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw java.io.IOException("Configuration request failed: HTTP ${connection.responseCode}.")
            }
            val bytes = connection.inputStream.use { input ->
                val output = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(8 * 1024)
                var total = 0
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    total += count
                    if (total > MAX_RESPONSE_BYTES) throw SecurityException("Configuration is too large.")
                    output.write(buffer, 0, count)
                }
                output.toByteArray()
            }
            bytes.toString(Charsets.UTF_8)
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        private val CACHED_JSON = stringPreferencesKey("cached_json")
        @Volatile private var instance: RemoteControlManager? = null

        fun get(context: Context): RemoteControlManager =
            instance ?: synchronized(this) {
                instance ?: RemoteControlManager(context).also { instance = it }
            }

        fun keyFor(category: ToolCategory): String = when (category) {
            ToolCategory.SECURITY -> RemoteFeatureKeys.SECURITY
            ToolCategory.CAMERA -> RemoteFeatureKeys.CAMERA
            ToolCategory.LOCATION -> RemoteFeatureKeys.LOCATION
        }

        fun keyFor(tool: ToolId): String = when (tool) {
            ToolId.ENCRYPT -> RemoteFeatureKeys.SECURITY_ENCRYPT
            ToolId.DECRYPT -> RemoteFeatureKeys.SECURITY_DECRYPT
            ToolId.BATCH -> RemoteFeatureKeys.SECURITY_BATCH
            ToolId.RECOVERY -> RemoteFeatureKeys.SECURITY_RECOVERY
            ToolId.MIGRATION -> RemoteFeatureKeys.SECURITY_MIGRATION
            ToolId.AUDIT -> RemoteFeatureKeys.SECURITY_AUDIT
            ToolId.SCANNER -> RemoteFeatureKeys.CAMERA_SCANNER
            ToolId.CODES -> RemoteFeatureKeys.CAMERA_CODES
            ToolId.MAGNIFIER -> RemoteFeatureKeys.CAMERA_MAGNIFIER
            ToolId.IMAGE_INFO -> RemoteFeatureKeys.CAMERA_IMAGE_INFO
            ToolId.MAP -> RemoteFeatureKeys.LOCATION_MAP
            ToolId.COMPASS -> RemoteFeatureKeys.LOCATION_COMPASS
            ToolId.GPS -> RemoteFeatureKeys.LOCATION_GPS
            ToolId.DISTANCE -> RemoteFeatureKeys.LOCATION_DISTANCE
        }
    }
}
