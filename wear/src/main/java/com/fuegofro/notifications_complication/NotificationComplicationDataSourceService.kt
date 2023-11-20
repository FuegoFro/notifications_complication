package com.fuegofro.notifications_complication

import android.graphics.drawable.Icon
import android.util.Log
import androidx.core.graphics.drawable.toIcon
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationText
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.LongTextComplicationData
import androidx.wear.watchface.complications.data.MonochromaticImage
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.data.SmallImage
import androidx.wear.watchface.complications.data.SmallImageComplicationData
import androidx.wear.watchface.complications.data.SmallImageType
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import com.fuegofro.notifications_complication.common.NotificationInfo
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.tasks.await

class NotificationComplicationDataSourceService : SuspendingComplicationDataSourceService() {
    private val currentNotificationUriDataStore by lazy { CurrentNotificationUriDataStore(this) }
    private val dataClient by lazy { Wearable.getDataClient(this) }

    override fun getPreviewData(type: ComplicationType): ComplicationData {
        return when (type) {
            ComplicationType.SHORT_TEXT ->
                ShortTextComplicationData.Builder(
                        "Title…".toComplicationText(),
                        contentDescription = "Notification title".toComplicationText()
                    )
                    .setMonochromaticImage(MonochromaticImage.PLACEHOLDER)
                    .build()
            ComplicationType.LONG_TEXT ->
                LongTextComplicationData.Builder(
                        "Notification body…".toComplicationText(),
                        contentDescription = "Notification preview".toComplicationText()
                    )
                    .setTitle("Title…".toComplicationText())
                    .setSmallImage(SmallImage.PLACEHOLDER)
                    .build()
            ComplicationType.SMALL_IMAGE ->
                SmallImageComplicationData.Builder(
                        SmallImage.PLACEHOLDER,
                        contentDescription = "Notification image".toComplicationText()
                    )
                    .build()
            else -> throw AssertionError("System provided unknown complication type $type")
        }
    }

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData {
        Log.e("DataSourceServ", "Requesting for type ${request.complicationType}")

        val notificationInfo =
            currentNotificationUriDataStore.getUri()?.let { uri ->
                NotificationInfo.fromBytes(dataClient.getDataItem(uri).await().data)
            }

        Log.e(
            "DataSourceServ",
            "Got notification info title=${notificationInfo?.title} body=${notificationInfo?.body}"
        )

        return when (request.complicationType) {
            ComplicationType.SHORT_TEXT -> {
                if (notificationInfo == null) {
                    ShortTextComplicationData.Builder(
                            "None".toComplicationText(),
                            contentDescription = "No notifications".toComplicationText()
                        )
                        .build()
                } else {
                    ShortTextComplicationData.Builder(
                            notificationInfo.title.subSequence(0, 7).toComplicationText(),
                            contentDescription = "Notification title".toComplicationText()
                        )
                        .setMonochromaticImage(
                            notificationInfo.smallIconBitmap()?.toIcon()?.toMonochromaticImage()
                        )
                        .build()
                }
            }
            ComplicationType.LONG_TEXT -> {
                if (notificationInfo == null) {
                    LongTextComplicationData.Builder(
                            "No notifications".toComplicationText(),
                            contentDescription = "No notifications".toComplicationText()
                        )
                        .build()
                } else {
                    LongTextComplicationData.Builder(
                            notificationInfo.body.toComplicationText(),
                            contentDescription = "Notification preview".toComplicationText()
                        )
                        .setTitle(notificationInfo.title.toComplicationText())
                        .setSmallImage(notificationInfo.largeIconBitmap()?.toIcon()?.toSmallImage())
                        .build()
                }
            }
            ComplicationType.SMALL_IMAGE -> {
                notificationInfo?.largeIconBitmap()?.toIcon()?.toSmallImage()?.let { smallImage ->
                    SmallImageComplicationData.Builder(
                            smallImage,
                            contentDescription = "Notification image".toComplicationText()
                        )
                        .build()
                }
                    ?: SmallImageComplicationData.Builder(
                            SmallImage.PLACEHOLDER,
                            contentDescription = "No notifications".toComplicationText()
                        )
                        .build()
            }
            else ->
                throw AssertionError(
                    "System provided unknown complication type ${request.complicationType}"
                )
        }
    }
}

private fun CharSequence.toComplicationText(): ComplicationText =
    PlainComplicationText.Builder(this).build()

private fun Icon.toMonochromaticImage(): MonochromaticImage =
    MonochromaticImage.Builder(this).build()

private fun Icon.toSmallImage(type: SmallImageType = SmallImageType.PHOTO): SmallImage =
    SmallImage.Builder(this, type).build()
