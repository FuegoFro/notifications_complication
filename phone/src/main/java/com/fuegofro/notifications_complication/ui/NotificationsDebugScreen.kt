@file:OptIn(ExperimentalMaterial3Api::class)

package com.fuegofro.notifications_complication.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fuegofro.notifications_complication.NotificationDebugInfo
import com.fuegofro.notifications_complication.NotificationListener
import com.fuegofro.notifications_complication.R
import com.fuegofro.notifications_complication.common.NotificationInfo
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun NotificationsDebugScreen(
    onNavigateUp: () -> Unit,
    notificationListenerBinderFlow: Flow<NotificationListener.NotificationListenerBinder?>,
) {
    val coroutineScope = rememberCoroutineScope()
    val binder =
        notificationListenerBinderFlow.collectAsStateWithLifecycle(initialValue = null).value
    var infos by remember { mutableStateOf(listOf<NotificationDebugInfo>()) }
    suspend fun reFetchNotificationInfos() {
        infos = binder?.getAllNotificationInfos() ?: listOf()
    }

    val dataClient = Wearable.getDataClient(LocalContext.current)
    val (dataLayerNotification, setDataLayerNotification) =
        remember { mutableStateOf<NotificationInfo?>(null) }
    suspend fun reFetchDataLayerNotification() {
        val dataItems = dataClient.dataItems.await()
        val notificationInfo = NotificationInfo.fromBytes(dataItems.firstOrNull()?.data)
        dataItems.release()
        setDataLayerNotification(notificationInfo)
    }

    LaunchedEffect(Unit) { reFetchDataLayerNotification() }
    LaunchedEffect(binder) { reFetchNotificationInfos() }

    Scaffold(
        topBar = {
            TopAppBar(
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                title = { Text("Notification debug info") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.button_up),
                        )
                    }
                },
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            Button(onClick = { coroutineScope.launch { reFetchDataLayerNotification() } }) {
                Text("Re-fetch current notification")
            }
            if (binder == null) {
                Text("Not connected to notification service...")
            } else {
                Button(onClick = { coroutineScope.launch { reFetchNotificationInfos() } }) {
                    Text("Re-fetch notification infos")
                }
                Button(onClick = { coroutineScope.launch { binder.forceRefresh() } }) {
                    Text("Force refresh current notification")
                }
            }

            LazyColumn {
                item {
                    if (dataLayerNotification == null) {
                        Text("No current notification")
                    } else {
                        NotificationDebugRow(NotificationDebugInfo(dataLayerNotification, mapOf()))
                    }
                    HorizontalDivider()
                }
                items(infos, key = { it.info.key }) { NotificationDebugRow(it) }
            }
        }
    }
}

@Composable
fun NotificationDebugRow(notificationDebugInfo: NotificationDebugInfo) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val notificationInfo = notificationDebugInfo.info

    Column {
        Row(modifier = Modifier.clickable { expanded = !expanded }) {
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
                        Image(
                            it.asImageBitmap(),
                            contentDescription = "Small Icon",
                            modifier = Modifier.size(12.dp).align(Alignment.Center),
                        )
                    }
                }
            }
            Column {
                Text(notificationInfo.title)
                Text(notificationInfo.body)
            }
        }
        if (expanded) {
            Column {
                for ((key, value) in notificationDebugInfo.extras) {
                    Text(key, fontWeight = FontWeight.Bold)
                    Text(value)
                }
            }
        }
    }
}
