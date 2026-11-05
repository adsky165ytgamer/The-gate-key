package com.example.remote

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteConfigValidatorTest {
    private fun valid(version: Long = 1L) = RemoteConfig(
        schemaVersion = 1,
        configVersion = version,
        minimumSupportedAppVersionCode = 6,
        maintenanceMode = false,
        features = mapOf(
            RemoteFeatureKeys.SECURITY to RemoteFeatureConfig("enabled", true),
            RemoteFeatureKeys.LOCATION to RemoteFeatureConfig("beta", true)
        ),
        interfaceConfig = RemoteInterfaceConfig(false, true)
    )

    @Test
    fun acceptsValidConfiguration() {
        assertTrue(RemoteConfigValidator.validate(valid(), appVersionCode = 6).isSuccess)
    }

    @Test
    fun rejectsUnsupportedSchema() {
        assertFalse(RemoteConfigValidator.validate(valid().copy(schemaVersion = 2), 6).isSuccess)
    }

    @Test
    fun rejectsUnknownFeatureIdentifier() {
        val config = valid().copy(features = valid().features + ("security.unknown" to RemoteFeatureConfig("enabled", true)))
        assertFalse(RemoteConfigValidator.validate(config, 6).isSuccess)
    }

    @Test
    fun rejectsUnsupportedState() {
        val config = valid().copy(features = mapOf(RemoteFeatureKeys.SECURITY to RemoteFeatureConfig("launch_code", true)))
        assertFalse(RemoteConfigValidator.validate(config, 6).isSuccess)
    }

    @Test
    fun rejectsRollback() {
        assertFalse(RemoteConfigValidator.validate(valid(version = 4), 6, previousConfigVersion = 5).isSuccess)
    }

    @Test
    fun rejectsNewerMinimumAppVersion() {
        assertFalse(RemoteConfigValidator.validate(valid().copy(minimumSupportedAppVersionCode = 7), 6).isSuccess)
    }

    @Test
    fun allowsMissingFeatureEntriesWithSafeDefaults() {
        val config = valid().copy(features = emptyMap())
        assertTrue(RemoteConfigValidator.validate(config, 6).isSuccess)
    }
}
