package com.fuegofro.notifications_complication.phone.data

import android.annotation.SuppressLint
import android.app.Notification
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.Icon
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.protobuf.ProtoBuf

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

@Serializable
data class NotificationInfo(
    val key: String,
    val postTime: Long,
    val title: String?,
    val body: String?,
    // Just gotta be very careful not to modify these, I guess 🙃
    private val smallIcon: ByteArray?,
    private val largeIcon: ByteArray?,
    val color: Int,
) {
    override fun toString(): String {
        return "($key, $postTime)"
    }

    fun smallIconBitmap(): Bitmap? =
        smallIcon?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }

    fun largeIconBitmap(): Bitmap? =
        largeIcon?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }

    companion object {

        private fun iconToBitmapByteArray(context: Context, icon: Icon): ByteArray? {
            val drawable = icon.loadDrawable(context) ?: return null

            // Adapted from https://stackoverflow.com/a/10600736/3000133
            Log.e("CNDS", "icon=${icon.javaClass.name} drawable=${drawable.javaClass.name}")
            val bitmap: Bitmap =
                /*(drawable as? BitmapDrawable)?.bitmap
                ?:*/ kotlin.run {
                    Log.e(
                        "CNDS",
                        "intrinsicWidth=${drawable.intrinsicWidth} intrinsicHeight=${drawable.intrinsicHeight}"
                    )
                    // Single color bitmap will be created of 1x1 pixel
                    val bitmap =
                        if (drawable.intrinsicWidth <= 0 || drawable.intrinsicHeight <= 0) {
                            Bitmap.createBitmap(
                                1,
                                1,
                                Bitmap.Config.ARGB_8888,
                            )
                        } else {
                            Bitmap.createBitmap(
                                drawable.intrinsicWidth,
                                drawable.intrinsicHeight,
                                Bitmap.Config.ARGB_8888,
                            )
                        }

                    val canvas = Canvas(bitmap)
                    drawable.setBounds(0, 0, canvas.width, canvas.height)
                    drawable.draw(canvas)
                    bitmap
                }

            // Adapted from https://stackoverflow.com/a/4989543/3000133
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            val byteArray: ByteArray = stream.toByteArray()
            bitmap.recycle()

            return byteArray
        }

        fun NotificationInfo?.matchesStatusBarNotification(
            statusBarNotification: StatusBarNotification?
        ): Boolean =
            this?.key == statusBarNotification?.key &&
                this?.postTime == statusBarNotification?.postTime

        @SuppressLint("RestrictedApi") // for extractStyleFromNotification
        fun StatusBarNotification.toNotificationInfo(context: Context): NotificationInfo {
            val titleAndText =
                when (
                    val style = NotificationCompat.Style.extractStyleFromNotification(notification)
                ) {
                    is NotificationCompat.MessagingStyle -> {
                        style.messages.last()?.let { message ->
                            val prefix = message.person?.name?.let { "$it: " } ?: ""
                            Pair(style.conversationTitle.toString(), "$prefix${message.text ?: ""}")
                        }
                    }
                    else -> null
                }
            val (title, text) =
                titleAndText
                    ?: Pair(
                        notification.extras.getString(Notification.EXTRA_TITLE),
                        notification.extras.getString(Notification.EXTRA_TEXT)
                    )
            return NotificationInfo(
                key,
                postTime,
                title,
                text,
                iconToBitmapByteArray(context, notification.smallIcon),
                iconToBitmapByteArray(context, notification.getLargeIcon()),
                notification.color,
            )
        }
    }
}

@OptIn(ExperimentalSerializationApi::class)
object NotificationInfoSerializer : Serializer<NotificationInfo?> {
    override val defaultValue = null

    override suspend fun readFrom(input: InputStream): NotificationInfo? {
        NotificationInfo.serializer()
        val bytes = input.readBytes()
        if (bytes.isEmpty()) {
            return null
        }
        try {
            return ProtoBuf.decodeFromByteArray(
                NotificationInfo.serializer(),
                bytes,
            )
        } catch (serialization: SerializationException) {
            return null
        }
    }

    override suspend fun writeTo(t: NotificationInfo?, output: OutputStream) {
        // If null just don't write anything
        if (t != null) {
            withContext(Dispatchers.IO) {
                output.write(ProtoBuf.encodeToByteArray(NotificationInfo.serializer(), t))
            }
        }
    }
}
