package com.fuegofro.notifications_complication

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester

class SelfUpdatedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.e("SelfUpdatedReceiver", "called with action=${intent.action}")
        if (intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) {
            return
        }

        val complicationDataSourceUpdateRequester =
            ComplicationDataSourceUpdateRequester.create(
                context,
                ComponentName(context, NotificationComplicationDataSourceService::class.java)
            )

        Log.e("SelfUpdatedReceiver", "Requesting update")
        complicationDataSourceUpdateRequester.requestUpdateAll()
        Log.e("SelfUpdatedReceiver", "Done!")
    }
}
