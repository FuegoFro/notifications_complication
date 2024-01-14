package com.fuegofro.notifications_complication.common

import android.annotation.SuppressLint
import android.app.Notification
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
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
    fun smallIconBitmap(): Bitmap? = byteArraysToBitmap(smallIcon, largeIcon)

    fun largeIconBitmap(): Bitmap? = byteArraysToBitmap(largeIcon, smallIcon)

    companion object {
        private const val TAG = "NotificationInfo"
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

            return drawableToBitmapByteArray(drawable)
        }

        private const val MAX_DIMEN: Int = 128

        private fun drawableToBitmapByteArray(drawable: Drawable): ByteArray {
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
                            // Scale down if too big
                            var width = drawable.intrinsicWidth
                            var height = drawable.intrinsicHeight
                            val largest = Integer.max(width, height)
                            if (largest > MAX_DIMEN) {
                                val factor = (MAX_DIMEN.toFloat()) / (largest.toFloat())
                                width = (width.toFloat() * factor).toInt()
                                height = (height.toFloat() * factor).toInt()
                            }
                            Bitmap.createBitmap(
                                width,
                                height,
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

        @OptIn(ExperimentalStdlibApi::class)
        fun StatusBarNotification.toNotificationInfo(context: Context): NotificationInfo {
            @SuppressLint("RestrictedApi")
            val style = NotificationCompat.Style.extractStyleFromNotification(notification)
            val smallIcon =
                iconToBitmapByteArray(
                    context,
                    // TODO - Handle black notification tint
                    notification.smallIcon?.apply { setTint(notification.color) }
                )
            var largeIcon = iconToBitmapByteArray(context, notification.getLargeIcon())
            // Log.e(TAG,"smallIcon=$smallIcon, largeIcon=$largeIcon, color=${notification.color.toHexString()}")

            val titleAndText =
                when (style) {
                    is NotificationCompat.MessagingStyle -> {
                        if (largeIcon.isEmpty()) {
                            largeIcon =
                                iconToBitmapByteArray(context, style.user.icon?.toIcon(context))
                        }

                        val message = style.messages.last()
                        if (message != null && style.conversationTitle != null) {
                            val prefix = message.person?.name?.let { "$it: " } ?: ""
                            Pair(style.conversationTitle.toString(), "$prefix${message.text ?: ""}")
                        } else {
                            null
                        }
                    }
                    is NotificationCompat.BigPictureStyle -> {
                        if (largeIcon.isEmpty()) {
                            notification.extras
                                .getParcelableExtra("android.picture", Bitmap::class.java)
                                ?.let {
                                    largeIcon =
                                        drawableToBitmapByteArray(
                                            BitmapDrawable(context.resources, it)
                                        )
                                    Log.e(TAG, "updating largeIcon=$largeIcon")
                                }
                        }
                        null
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

fun <T> Bundle.getParcelableExtra(name: String, clazz: Class<T>): T? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelable(name, clazz)
    } else {
        @Suppress("DEPRECATION") clazz.cast(getParcelable(name))
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
