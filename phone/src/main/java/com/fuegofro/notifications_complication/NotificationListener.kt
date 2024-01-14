@file:OptIn(ExperimentalStdlibApi::class)

package com.fuegofro.notifications_complication

import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.lifecycle.lifecycleScope
import com.fuegofro.notifications_complication.common.NotificationInfo
import com.fuegofro.notifications_complication.common.NotificationInfo.Companion.matchesStatusBarNotification
import com.fuegofro.notifications_complication.common.NotificationInfo.Companion.toBytes
import com.fuegofro.notifications_complication.common.NotificationInfo.Companion.toNotificationInfo
import com.fuegofro.notifications_complication.data.EnabledPackagesDataStore
import com.fuegofro.notifications_complication.data.flagsToString
import com.fuegofro.notifications_complication.data.notificationFlagNames
import com.google.android.gms.wearable.PutDataRequest
import com.google.android.gms.wearable.Wearable
import java.lang.Exception
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class NotificationDebugInfo(val info: NotificationInfo, val extras: Map<String, String>)

class NotificationListener : NotificationListenerLifecycleService() {
    private val enabledPackagesDataStore by lazy { EnabledPackagesDataStore(this) }
    private val dataClient by lazy { Wearable.getDataClient(this) }

    inner class NotificationListenerBinder : Binder() {
        suspend fun getAllNotificationInfos(): List<NotificationDebugInfo> {
            // Wait until we're connected
            // Log.e(TAG, "getAllNotificationInfos waiting for connected")
            // Log.e(TAG, "getAllNotificationInfos initially ${connectedFlow.first()}")
            connectedFlow.first { it }
            // Log.e(TAG, "getAllNotificationInfos passed connected")

            return getActiveNotifications(currentRanking.orderedKeys)
                .asSequence()
                .map { sbn ->
                    val extras = sbn.notification.extras
                    val extrasElements =
                        extras.keySet().map { key ->
                            @Suppress("DEPRECATION")
                            key to extras.get(key).toString()
                        } +
                            sequenceOf(
                                "FLAGS" to
                                    flagsToString(sbn.notification.flags, notificationFlagNames),
                                "FLAGS_HEX" to sbn.notification.flags.toHexString()
                            )
                    NotificationDebugInfo(
                        sbn.toNotificationInfo(this@NotificationListener),
                        extrasElements.toMap(),
                    )
                }
                .toList()
        }

        suspend fun forceRefresh() {
            // Wait until we're connected
            connectedFlow.first { it }

            doUpdate("forceRefresh", currentRanking, true)
        }
    }

    private val connectedFlow = MutableStateFlow(false)

    companion object {
        const val TAG = "NLServ"
    }

    private fun doUpdate(
        name: String,
        rankingMap: RankingMap,
        forceUpdate: Boolean = false,
    ) {
        lifecycleScope.launch { doUpdateInner(name, rankingMap, forceUpdate) }
    }

    private suspend fun doUpdateInner(
        name: String,
        rankingMap: RankingMap,
        forceUpdate: Boolean,
    ) {
        // Log.e(TAG, "$name (start), new=${newStatusBarNotification.logString}")
        // Find the first valid StatusBarNotification.

        // TODO - If necessary for performance, use the existing (current) SBN or the provided new
        //   one if possible, otherwise fetch the SBN from the OS.
        val enabledPackages = enabledPackagesDataStore.enabledPackages.first()
        val firstStatusBarNotification =
            getActiveNotifications(rankingMap.orderedKeys).firstOrNull { statusBarNotification ->
                // TODO - Other filters based on notification?
                // val flags = statusBarNotification.notification.flags
                enabledPackages.contains(statusBarNotification.packageName) &&
                    statusBarNotification.notification.extras.getString("android.template") !=
                        "android.app.Notification\$InboxStyle"
                    // // Rather than getting the summary, get an individual one
                    // flags.isNotSet(Notification.FLAG_GROUP_SUMMARY)
                // Not filtering local since some things (like Messages) are local, but we
                // want them
            }

        // If this is different from our current, update and notify. Handles nulling out or updating
        // to new non-null
        val dataItems = dataClient.dataItems.await()
        val currentNotification = NotificationInfo.fromBytes(dataItems.firstOrNull()?.data)
        dataItems.release()
        if (
            !currentNotification.matchesStatusBarNotification(firstStatusBarNotification) ||
                forceUpdate
        ) {
            Log.e(TAG, "Updating!!! force=$forceUpdate")
            val notificationInfo = firstStatusBarNotification?.toNotificationInfo(this)
            val bytes = notificationInfo.toBytes()
            // Log.e(TAG, "encoded notification size=${bytes.size} title=${notificationInfo?.title}")
            try {
                dataClient.putDataItem(
                    PutDataRequest.create(NotificationInfo.DATA_LAYER_PATH).setData(bytes).setUrgent()
                ).await()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to put data: $e")
            }
        }

        Log.e(TAG, "$name, current=${currentNotification?.key}")
    }

    override fun onBind(intent: Intent?): IBinder? {
        // Log.e(TAG, "onBind intent=$intent")
        return if (intent?.action == null) {
            // Log.e(TAG, "Returning my binder")
            NotificationListenerBinder()
        } else {
            // Log.e(TAG, "Returning their binder")
            super.onBind(intent)
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        connectedFlow.value = true
        doUpdate("onListenerConnected", rankingMap = this.currentRanking)
    }

    override fun onListenerDisconnected() {
        connectedFlow.value = false
        super.onListenerDisconnected()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification, rankingMap: RankingMap) {
        super.onNotificationPosted(sbn, rankingMap)
        doUpdate("onNotificationPosted", rankingMap)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification, rankingMap: RankingMap) {
        super.onNotificationRemoved(sbn, rankingMap)
        doUpdate("onNotificationRemoved", rankingMap)
    }

    override fun onNotificationRankingUpdate(rankingMap: RankingMap) {
        super.onNotificationRankingUpdate(rankingMap)
        doUpdate("onNotificationRankingUpdate", rankingMap = rankingMap)
    }
}

fun Int.isSet(mask: Int): Boolean = this.and(mask) != 0

fun Int.isNotSet(mask: Int): Boolean = !this.isSet(mask)
