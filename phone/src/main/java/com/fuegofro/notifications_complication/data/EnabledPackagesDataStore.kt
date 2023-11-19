package com.fuegofro.notifications_complication.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context._enabledPackagesDataStore by preferencesDataStore("enabledPackages")

/** Wrapper to help access the enabledPackagesDataStore singleton */
class EnabledPackagesDataStore(context: Context) {
    companion object {
        val Context.enabledPackagesDataStore get() = EnabledPackagesDataStore(this)
    }

    private val dataStore = context._enabledPackagesDataStore

    private fun preferencesToSet(preferences: Preferences): Set<String> =
        preferences.asMap().keys.map { it.name }.toSet()

    val enabledPackages get(): Flow<Set<String>> =
        dataStore.data.map { preferencesToSet(it) }

    suspend fun setPackageEnabled(packageId: String, enabled: Boolean) {
        val key = booleanPreferencesKey(packageId)
        dataStore.edit {
            if (enabled) {
                it[key] = true
            } else {
                it.remove(key)
            }
        }
    }
    suspend fun enableAll(packageNamesToEnable: Sequence<String>) {
        dataStore.edit { dataStore ->
            packageNamesToEnable.forEach { name ->
                dataStore[booleanPreferencesKey(name)] = true
            }
        }
    }

    suspend fun disableAll() {
        dataStore.edit { it.clear() }
    }
}

