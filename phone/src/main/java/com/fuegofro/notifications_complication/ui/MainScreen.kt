package com.fuegofro.notifications_complication.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fuegofro.notifications_complication.data.CurrentNotificationDataStore.Companion.currentNotificationDataStore
import com.fuegofro.notifications_complication.data.NotificationInfo
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.protobuf.ProtoBuf

@Composable
fun MainScreen(onNavigateToPackagesSelection: () -> Unit) {
    val dataClient = Wearable.getDataClient(LocalContext.current)
    val (dataLayerNotification, setDataLayerNotification) =
        remember { mutableStateOf<NotificationInfo?>(null) }
    LaunchedEffect(Unit) {
        val dataItems = dataClient.dataItems.await()
        val data = dataItems.firstOrNull()?.data ?: ByteArray(0)
        val notificationInfo =
            if (data.isEmpty()) {
                null
            } else {
                @OptIn(ExperimentalSerializationApi::class)
                ProtoBuf.decodeFromByteArray(NotificationInfo.serializer(), data)
            }
        dataItems.release()
        setDataLayerNotification(notificationInfo)
    }

    Scaffold { paddingValues ->
        Column(Modifier.padding(paddingValues)) {
            val currentNotification =
                LocalContext.current.currentNotificationDataStore
                    .currentNotification()
                    .collectAsStateWithLifecycle(initialValue = null)
                    .value
            NotificationPreview(currentNotification)
            NotificationPreview(dataLayerNotification)
            Button(onClick = onNavigateToPackagesSelection) { Text("Select Packages") }
        }
    }
}

@Composable
private fun NotificationPreview(notificationInfo: NotificationInfo?) {
    if (notificationInfo != null) {
        Row {
            Box {
                notificationInfo.largeIconBitmap()?.let {
                    Image(
                        it.asImageBitmap(),
                        contentDescription = "Large Icon",
                    )
                }
                Box(
                    modifier =
                        Modifier.background(
                                MaterialTheme.colorScheme.background,
                                CircleShape,
                            )
                            .padding(4.dp)
                            .align(Alignment.BottomEnd),
                ) {
                    notificationInfo.smallIconBitmap()?.let {
                        Icon(
                            it.asImageBitmap(),
                            contentDescription = "Small Icon",
                            tint = Color(notificationInfo.color),
                            modifier = Modifier.size(12.dp).align(Alignment.Center),
                        )
                    }
                }
            }
            Column {
                notificationInfo.title?.let { Text(it) }
                notificationInfo.body?.let { Text(it) }
            }
        }
    } else {
        Text("No notification")
    }
}
