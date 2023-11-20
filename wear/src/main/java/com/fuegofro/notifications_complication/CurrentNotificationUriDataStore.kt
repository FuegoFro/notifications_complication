package com.fuegofro.notifications_complication

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map


private val Context._currentNotificationUriDataStore by preferencesDataStore("currentNotificationUriDataStore")
private val uriKey = stringPreferencesKey("URI_KEY")

class CurrentNotificationUriDataStore(context: Context) {
    private val dataStore = context._currentNotificationUriDataStore

    suspend fun getUri(): Uri? {
        return dataStore.data.map { it[uriKey]?.toUri() }.first()
    }

    suspend fun setUri(uri: Uri?) {
        dataStore.edit {
            if (uri == null) {
                it.remove(uriKey)
            } else {
                it[uriKey] = uri.toString()
            }
        }
    }
}
