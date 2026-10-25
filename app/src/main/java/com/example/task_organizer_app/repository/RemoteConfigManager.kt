package com.example.task_organizer_app.repository

import android.util.Log
import com.example.task_organizer_app.model.AppUpdateInfo
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import kotlinx.coroutines.tasks.await

class RemoteConfigManager {
    private val TAG = "RemoteConfigManager"

    private val remoteConfig: FirebaseRemoteConfig? by lazy {
        runCatching {
            val config = FirebaseRemoteConfig.getInstance()
            val configSettings = FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(3600)
                .build()
            config.setConfigSettingsAsync(configSettings)
            val defaults = mapOf<String, Any>(
                KEY_MIN_REQUIRED_VERSION to 1,
                KEY_LATEST_VERSION_CODE to 1,
                KEY_LATEST_VERSION_NAME to "1.0.0",
                KEY_UPDATE_URL to "https://play.google.com/store/apps",
                KEY_RELEASE_NOTES to "General performance improvements and bug fixes."
            )
            config.setDefaultsAsync(defaults)
            config
        }.getOrNull()
    }

    suspend fun checkForUpdates(currentVersionCode: Int): AppUpdateInfo {
        val config = remoteConfig
        if (config == null) {
            Log.w(TAG, "Firebase Remote Config unavailable; returning default info.")
            return AppUpdateInfo(
                latestVersionCode = currentVersionCode,
                latestVersionName = "1.0.0",
                minRequiredVersionCode = 1,
                isUpdateAvailable = false,
                isForceUpdate = false
            )
        }

        try {
            config.fetchAndActivate().await()
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching Remote Config", e)
        }

        val minVersion = config.getLong(KEY_MIN_REQUIRED_VERSION).toInt()
        val latestVersion = config.getLong(KEY_LATEST_VERSION_CODE).toInt()
        val latestName = config.getString(KEY_LATEST_VERSION_NAME).ifEmpty { "1.0.0" }
        val updateUrl = config.getString(KEY_UPDATE_URL).ifEmpty { "https://play.google.com/store/apps" }
        val notes = config.getString(KEY_RELEASE_NOTES).ifEmpty { "New feature updates available!" }

        val isAvailable = latestVersion > currentVersionCode
        val isForce = currentVersionCode < minVersion

        return AppUpdateInfo(
            latestVersionCode = latestVersion,
            latestVersionName = latestName,
            minRequiredVersionCode = minVersion,
            updateUrl = updateUrl,
            releaseNotes = notes,
            isUpdateAvailable = isAvailable,
            isForceUpdate = isForce
        )
    }

    companion object {
        private const val KEY_MIN_REQUIRED_VERSION = "min_required_version_code"
        private const val KEY_LATEST_VERSION_CODE = "latest_version_code"
        private const val KEY_LATEST_VERSION_NAME = "latest_version_name"
        private const val KEY_UPDATE_URL = "update_url"
        private const val KEY_RELEASE_NOTES = "release_notes"
    }
}
