package com.fuegofro.notifications_complication

import android.content.ComponentName
import android.util.Log
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester
import com.fuegofro.notifications_complication.common.NotificationInfo
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationWearableListenerService : WearableListenerService() {
    private val currentNotificationUriDataStore by lazy { CurrentNotificationUriDataStore(this) }
    private val complicationDataSourceUpdateRequester by lazy {
        ComplicationDataSourceUpdateRequester.create(
            this,
            ComponentName(this, NotificationComplicationDataSourceService::class.java)
        )
    }
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    override fun onDataChanged(p0: DataEventBuffer) {
        Log.e("WearListenerServ", "onDataChanged")
        super.onDataChanged(p0)

        p0.forEach { dataEvent ->
            val uri = dataEvent.dataItem.uri
            Log.e("WearListenerServ", "uri=${uri} type=${dataEvent.type}")
            if (uri.path != NotificationInfo.DATA_LAYER_PATH) {
                return@forEach
            }

            val newUri =
                when (dataEvent.type) {
                    DataEvent.TYPE_CHANGED -> {
                        uri
                    }
                    DataEvent.TYPE_DELETED -> {
                        null
                    }
                    else ->
                        throw AssertionError(
                            "System provided unknown data event type ${dataEvent.type}"
                        )
                }

            coroutineScope.launch {
                currentNotificationUriDataStore.setUri(newUri)
                Log.e("WearListenerServ", "Done setting newUri=$newUri")
                complicationDataSourceUpdateRequester.requestUpdateAll()
                Log.e("WearListenerServ", "Requesting update")
            }
        }
    }
}
