package com.fuegofro.notifications_complication

import android.app.Notification
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.lifecycle.lifecycleScope
import com.fuegofro.notifications_complication.common.NotificationInfo
import com.fuegofro.notifications_complication.common.NotificationInfo.Companion.matchesStatusBarNotification
import com.fuegofro.notifications_complication.common.NotificationInfo.Companion.toBytes
import com.fuegofro.notifications_complication.common.NotificationInfo.Companion.toNotificationInfo
import com.fuegofro.notifications_complication.data.CurrentNotificationDataStore
import com.fuegofro.notifications_complication.data.EnabledPackagesDataStore
import com.google.android.gms.wearable.PutDataRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class NotificationListener : NotificationListenerLifecycleService() {
    // Store off active key and notification info
    // Ability to on-demand refresh

    private val enabledPackagesDataStore by lazy { EnabledPackagesDataStore(this) }
    private val currentNotificationDataStore by lazy { CurrentNotificationDataStore(this) }
    private val dataClient by lazy { Wearable.getDataClient(this) }

    companion object {
        const val TAG = "NLServ"
    }

    private fun doUpdate(
        name: String,
        newStatusBarNotification: StatusBarNotification?,
        rankingMap: RankingMap
    ) {
        lifecycleScope.launch { doUpdateInner(name, newStatusBarNotification, rankingMap) }
    }

    private val StatusBarNotification?.logString
        get() = this?.apply { "($key, $postTime)" }

    private suspend fun doUpdateInner(
        name: String,
        newStatusBarNotification: StatusBarNotification?,
        rankingMap: RankingMap
    ) {
        Log.e(TAG, "$name (start), new=${newStatusBarNotification.logString}")
        // Find the first valid StatusBarNotification.

        // TODO - If necessary for performance, use the existing (current) SBN or the provided new
        //   one if possible, otherwise fetch the SBN from the OS.
        val enabledPackages = enabledPackagesDataStore.enabledPackages.first()
        val firstStatusBarNotification =
            getActiveNotifications(rankingMap.orderedKeys).firstOrNull { statusBarNotification ->
                // TODO - Other filters based on notification?
                enabledPackages.contains(statusBarNotification.packageName) &&
                    // Rather than getting the "summary" inbox notification, get an individual one
                    !statusBarNotification.notification.extras
                        .getString(Notification.EXTRA_TEMPLATE)
                        .equals(Notification.InboxStyle::class.java.name)
            }

        // If this is different from our current, update and notify. Handles nulling out or updating
        // to new non-null
        val currentNotification = currentNotificationDataStore.currentNotification().first()
        if (!currentNotification.matchesStatusBarNotification(firstStatusBarNotification)) {
            Log.e(TAG, "Updating!!!")
            val notificationInfo = firstStatusBarNotification?.toNotificationInfo(this)
            // TODO - Do we even need to store this here???
            currentNotificationDataStore.setNotification(notificationInfo)
            // TODO - Notify change
            val bytes = notificationInfo.toBytes()
            Log.e(TAG, "encoded notification size=${bytes.size}")
            dataClient.putDataItem(
                PutDataRequest.create(NotificationInfo.DATA_LAYER_PATH).setData(bytes).setUrgent()
            )
        }

        Log.e(TAG, "$name, current=${currentNotification}")
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        doUpdate(
            "onListenerConnected",
            newStatusBarNotification = null,
            rankingMap = this.currentRanking
        )
    }

    override fun onNotificationPosted(sbn: StatusBarNotification, rankingMap: RankingMap) {
        super.onNotificationPosted(sbn, rankingMap)
        doUpdate("onNotificationPosted", sbn, rankingMap)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification, rankingMap: RankingMap) {
        super.onNotificationRemoved(sbn, rankingMap)
        doUpdate("onNotificationRemoved", sbn, rankingMap)
    }

    override fun onNotificationRankingUpdate(rankingMap: RankingMap) {
        super.onNotificationRankingUpdate(rankingMap)
        doUpdate(
            "onNotificationRankingUpdate",
            newStatusBarNotification = null,
            rankingMap = rankingMap
        )
    }
}
