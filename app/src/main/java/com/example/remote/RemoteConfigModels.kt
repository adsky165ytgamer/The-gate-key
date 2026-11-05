package com.example.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RemoteConfig(
    @Json(name = "schema_version") val schemaVersion: Int,
    @Json(name = "config_version") val configVersion: Long,
    @Json(name = "minimum_supported_app_version_code") val minimumSupportedAppVersionCode: Int,
    @Json(name = "maintenance_mode") val maintenanceMode: Boolean,
    val features: Map<String, RemoteFeatureConfig>,
    @Json(name = "interface") val interfaceConfig: RemoteInterfaceConfig
) {
    fun feature(key: String): RemoteFeatureConfig? = features[key]

    companion object {
        fun defaults(): RemoteConfig = RemoteConfig(
            schemaVersion = 1,
            configVersion = 0,
            minimumSupportedAppVersionCode = 1,
            maintenanceMode = false,
            features = emptyMap(),
            interfaceConfig = RemoteInterfaceConfig(
                showDisabledFeatures = false,
                showBetaBadges = true
            )
        )
    }
}

@JsonClass(generateAdapter = true)
data class RemoteFeatureConfig(
    val state: String,
    val show: Boolean,
    val label: String? = null,
    val description: String? = null
)

@JsonClass(generateAdapter = true)
data class RemoteInterfaceConfig(
    @Json(name = "show_disabled_features") val showDisabledFeatures: Boolean,
    @Json(name = "show_beta_badges") val showBetaBadges: Boolean
)

enum class RemoteFeatureState {
    ENABLED,
    DISABLED,
    BETA,
    MAINTENANCE,
    DEPRECATED;

    companion object {
        fun parse(raw: String): RemoteFeatureState? = when (raw.lowercase()) {
            "enabled" -> ENABLED
            "disabled" -> DISABLED
            "beta" -> BETA
            "maintenance" -> MAINTENANCE
            "deprecated" -> DEPRECATED
            else -> null
        }
    }
}

object RemoteFeatureKeys {
    const val SECURITY = "security"
    const val SECURITY_ENCRYPT = "security.encrypt"
    const val SECURITY_DECRYPT = "security.decrypt"
    const val SECURITY_BATCH = "security.batch"
    const val SECURITY_RECOVERY = "security.recovery"
    const val SECURITY_MIGRATION = "security.migration"
    const val SECURITY_AUDIT = "security.audit"
    const val CAMERA = "camera_tools"
    const val CAMERA_SCANNER = "camera_tools.scanner"
    const val CAMERA_CODES = "camera_tools.codes"
    const val CAMERA_MAGNIFIER = "camera_tools.magnifier"
    const val CAMERA_IMAGE_INFO = "camera_tools.image_info"
    const val LOCATION = "location_kit"
    const val LOCATION_MAP = "location_kit.map"
    const val LOCATION_COMPASS = "location_kit.compass"
    const val LOCATION_GPS = "location_kit.gps"
    const val LOCATION_DISTANCE = "location_kit.distance"

    val all: Set<String> = setOf(
        SECURITY, SECURITY_ENCRYPT, SECURITY_DECRYPT, SECURITY_BATCH,
        SECURITY_RECOVERY, SECURITY_MIGRATION, SECURITY_AUDIT,
        CAMERA, CAMERA_SCANNER, CAMERA_CODES, CAMERA_MAGNIFIER, CAMERA_IMAGE_INFO,
        LOCATION, LOCATION_MAP, LOCATION_COMPASS, LOCATION_GPS, LOCATION_DISTANCE
    )
}

object RemoteConfigValidator {
    private const val MAX_FEATURES = 64
    private const val MAX_LABEL_LENGTH = 80
    private const val MAX_DESCRIPTION_LENGTH = 240

    fun validate(
        config: RemoteConfig,
        appVersionCode: Int,
        previousConfigVersion: Long = -1L
    ): Result<Unit> {
        if (config.schemaVersion != 1) return failure("Unsupported configuration schema version.")
        if (config.configVersion < 1) return failure("Configuration version must be positive.")
        if (config.configVersion < previousConfigVersion) return failure("Configuration rollback rejected.")
        if (config.minimumSupportedAppVersionCode < 1) return failure("Invalid minimum app version.")
        if (config.minimumSupportedAppVersionCode > appVersionCode) {
            return failure("Configuration requires a newer app version.")
        }
        if (config.features.size > MAX_FEATURES) return failure("Too many feature entries.")
        val unknown = config.features.keys - RemoteFeatureKeys.all
        if (unknown.isNotEmpty()) return failure("Unknown feature identifier: ${unknown.first()}.")
        config.features.forEach { (key, feature) ->
            if (RemoteFeatureState.parse(feature.state) == null) {
                return failure("Unsupported state for $key.")
            }
            if (feature.label?.length ?: 0 > MAX_LABEL_LENGTH) {
                return failure("Feature label is too long for $key.")
            }
            if (feature.description?.length ?: 0 > MAX_DESCRIPTION_LENGTH) {
                return failure("Feature description is too long for $key.")
            }
        }
        return Result.success(Unit)
    }

    private fun failure(message: String): Result<Unit> = Result.failure(IllegalArgumentException(message))
}
