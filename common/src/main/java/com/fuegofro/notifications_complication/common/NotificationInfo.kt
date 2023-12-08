package com.fuegofro.notifications_complication.common

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
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.protobuf.ProtoBuf

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class NotificationInfo(
    val key: String,
    val postTime: Long,
    val title: String,
    val body: String,
    // Just gotta be very careful not to modify these, I guess 🙃
    private val smallIcon: ByteArray,
    private val largeIcon: ByteArray,
    val color: Int,
) {
    fun smallIconBitmap(): Bitmap? =
        byteArraysToBitmap(smallIcon, largeIcon)

    fun largeIconBitmap(): Bitmap? =
        byteArraysToBitmap(largeIcon, smallIcon)

    companion object {
        // Must match manifest value in wear app
        const val DATA_LAYER_PATH = "/current_notification"
        fun NotificationInfo?.toBytes(): ByteArray =
            this?.let { ProtoBuf.encodeToByteArray(serializer(), it) } ?: ByteArray(0)

        fun fromBytes(data: ByteArray?): NotificationInfo? =
            if (data == null || data.isEmpty()) {
                null
            } else {
                ProtoBuf.decodeFromByteArray(serializer(), data)
            }

        private fun iconToBitmapByteArray(context: Context, icon: Icon?): ByteArray {
            val drawable = icon?.loadDrawable(context) ?: return ByteArray(0)

            // Adapted from https://stackoverflow.com/a/10600736/3000133
            // Log.e("CNDS", "icon=${icon.javaClass.name} drawable=${drawable.javaClass.name}")
            val bitmap: Bitmap =
                /*(drawable as? BitmapDrawable)?.bitmap
                ?:*/ kotlin.run {
                    // Log.e(
                    //     "CNDS",
                    //     "intrinsicWidth=${drawable.intrinsicWidth} intrinsicHeight=${drawable.intrinsicHeight}"
                    // )
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

        fun byteArraysToBitmap(vararg priorityByteArrays: ByteArray): Bitmap? {
            val byteArray = priorityByteArrays.firstOrNull { it.isNotEmpty() } ?: ByteArray(0)
            return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
        }

        fun NotificationInfo?.matchesStatusBarNotification(
            statusBarNotification: StatusBarNotification?
        ): Boolean =
            this?.key == statusBarNotification?.key &&
                this?.postTime == statusBarNotification?.postTime

        fun StatusBarNotification.toNotificationInfo(context: Context): NotificationInfo {
            @SuppressLint("RestrictedApi")
            val style = NotificationCompat.Style.extractStyleFromNotification(notification)
            val smallIcon = iconToBitmapByteArray(context, notification.smallIcon?.apply { setTint(notification.color) });
            var largeIcon = iconToBitmapByteArray(context, notification.getLargeIcon())

            val titleAndText =
                when (
                    style
                ) {
                    is NotificationCompat.MessagingStyle -> {
                        if (largeIcon.isEmpty()) {
                            largeIcon = iconToBitmapByteArray(context, style.user.icon?.toIcon(context))
                        }

                        val message = style.messages.last()
                        if (message != null && style.conversationTitle != null) {
                            val prefix = message.person?.name?.let { "$it: " } ?: ""
                            Pair(style.conversationTitle.toString(), "$prefix${message.text ?: ""}")
                        } else {
                            null
                        }
                    }
                    else -> null
                }
            val (title, text) =
                titleAndText
                    ?: Pair(
                        notification.extras.getCharSequence(Notification.EXTRA_TITLE),
                        notification.extras.getCharSequence(Notification.EXTRA_TEXT)
                    )
            return NotificationInfo(
                key,
                postTime,
                (title ?: "").toString(),
                (text ?: "").toString(),
                smallIcon,
                largeIcon,
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
        return try {
            ProtoBuf.decodeFromByteArray(
                NotificationInfo.serializer(),
                bytes,
            )
        } catch (serialization: SerializationException) {
            null
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
