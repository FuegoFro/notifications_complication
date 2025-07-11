package com.fuegofro.notifications_complication.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context._enabledPackagesDataStore by preferencesDataStore("enabledPackages")

/** Wrapper to help access the enabledPackagesDataStore singleton */
class EnabledPackagesDataStore(context: Context) {
    companion object {
        val Context.enabledPackagesDataStore get() = EnabledPackagesDataStore(this)
    }

    private val dataStore = context._enabledPackagesDataStore

    private val json = Json { ignoreUnknownKeys = true }

    private fun preferencesToPackageSettingsMap(preferences: Preferences): Map<String, PackageSettings> {
        return preferences.asMap().mapNotNull { (key, value) ->
            try {
                val settings = json.decodeFromString<PackageSettings>(value as String)
                key.name to settings
            } catch (e: Exception) {
                // Handle migration from old boolean format
                if (value is Boolean && value) {
                    key.name to PackageSettings(enabled = true)
                } else {
                    null
                }
            }
        }.toMap()
    }

    val packageSettings get(): Flow<Map<String, PackageSettings>> =
        dataStore.data.map { preferencesToPackageSettingsMap(it) }

    val enabledPackages get(): Flow<Set<String>> =
        packageSettings.map { settings ->
            settings.filterValues { it.enabled }.keys
        }

    suspend fun getPackageSettings(packageId: String): PackageSettings? {
        return packageSettings.map { it[packageId] }.firstOrNull()
    }

    suspend fun setPackageSettings(packageId: String, settings: PackageSettings) {
        val key = stringPreferencesKey(packageId)
        dataStore.edit {
            if (settings == PackageSettings.DEFAULT) {
                it.remove(key)
            } else {
                it[key] = json.encodeToString(settings)
            }
        }
    }

    suspend fun setPackageEnabled(packageId: String, enabled: Boolean) {
        val currentSettings = getPackageSettings(packageId) ?: PackageSettings.DEFAULT
        setPackageSettings(packageId, currentSettings.copy(enabled = enabled))
    }
    suspend fun enableAll(packageNamesToEnable: Sequence<String>) {
        dataStore.edit { preferences ->
            packageNamesToEnable.forEach { name ->
                val key = stringPreferencesKey(name)
                val settings = PackageSettings(enabled = true)
                preferences[key] = json.encodeToString(settings)
            }
        }
    }

    suspend fun disableAll() {
        val currentSettings = packageSettings.firstOrNull() ?: emptyMap()
        dataStore.edit { preferences ->
            currentSettings.forEach { (packageId, settings) ->
                val key = stringPreferencesKey(packageId)
                preferences[key] = json.encodeToString(settings.copy(enabled = false))
            }
        }
    }
}

