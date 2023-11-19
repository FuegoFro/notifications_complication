package com.fuegofro.notifications_complication.data

import android.content.Context
import androidx.datastore.dataStore
import com.fuegofro.notifications_complication.common.NotificationInfo
import com.fuegofro.notifications_complication.common.NotificationInfoSerializer
import kotlinx.coroutines.flow.Flow

private val Context._currentNotificationDataStore by
    dataStore("currentNotification", NotificationInfoSerializer)

class CurrentNotificationDataStore(context: Context) {
    companion object {
        val Context.currentNotificationDataStore
            get() = CurrentNotificationDataStore(this)
    }

    private val dataStore = context._currentNotificationDataStore

    fun currentNotification(): Flow<NotificationInfo?> = dataStore.data

    suspend fun setNotification(notificationInfo: NotificationInfo?) {
        dataStore.updateData { notificationInfo }
    }
}

