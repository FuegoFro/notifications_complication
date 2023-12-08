package com.fuegofro.notifications_complication.data

import android.app.Notification
import android.content.Context
import androidx.datastore.dataStore
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.fuegofro.notifications_complication.common.NotificationInfo
import com.fuegofro.notifications_complication.common.NotificationInfoSerializer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context._currentNotificationDataStore by
    dataStore("currentNotification", NotificationInfoSerializer)
private val Context._currentNotificationExtrasDataStore by
    preferencesDataStore("currentNotificationExtras")

class CurrentNotificationDataStore(context: Context) {
    companion object {
        val Context.currentNotificationDataStore
            get() = CurrentNotificationDataStore(this)
    }

    private val infoDataStore = context._currentNotificationDataStore
    private val extrasDataStore = context._currentNotificationExtrasDataStore

    fun currentNotificationInfo(): Flow<NotificationInfo?> = infoDataStore.data

    suspend fun setNotificationInfo(notificationInfo: NotificationInfo?) {
        infoDataStore.updateData { notificationInfo }
    }

    fun currentNotificationExtras(): Flow<Map<String, String>> {
        return extrasDataStore.data.map { extrasPreferences ->
            extrasPreferences.asMap().map { (k, v) -> k.name to v.toString() }.toMap()
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    suspend fun setNotificationExtras(notification: Notification?) {
        extrasDataStore.edit {
            it.clear()
            if (notification == null) {
                return@edit
            }
            val extras = notification.extras
            extras.keySet().forEach { key ->
                @Suppress("DEPRECATION")
                it[stringPreferencesKey(key)] = extras.get(key).toString()
            }
            it[stringPreferencesKey("FLAGS")] =
                flagsToString(notification.flags, notificationFlagNames)
            it[stringPreferencesKey("FLAGS_HEX")] =
                notification.flags.toHexString()
        }
    }
}

val notificationFlagNames: Array<String> =
    arrayOf(
        "FLAG_SHOW_LIGHTS",
        "FLAG_ONGOING_EVENT",
        "FLAG_INSISTENT",
        "FLAG_ONLY_ALERT_ONCE",
        "FLAG_AUTO_CANCEL",
        "FLAG_NO_CLEAR",
        "FLAG_FOREGROUND_SERVICE",
        "FLAG_HIGH_PRIORITY",
        "FLAG_LOCAL_ONLY",
        "FLAG_GROUP_SUMMARY",
        "FLAG_AUTOGROUP_SUMMARY",
        "FLAG_CAN_COLORIZE",
        "FLAG_BUBBLE",
        "FLAG_NO_DISMISS",
        "FLAG_FSI_REQUESTED_BUT_DENIED",
        "FLAG_USER_INITIATED_JOB",
    )

fun flagsToString(flags: Int, flagNames: Array<String>): String {
    var flagsRemaining = flags
    val result = mutableListOf<String>()
    var idx = 0
    while (flagsRemaining > 0) {
        if (flagsRemaining.mod(2) != 0) {
            result.add(flagNames.getOrElse(idx) { "<unknown ${1.shl(idx)}>" })
        }
        flagsRemaining = flagsRemaining.ushr(1)
        idx += 1
    }
    return if (result.isEmpty()) {
        "<none>"
    } else {
        result.joinToString("|")
    }
}
