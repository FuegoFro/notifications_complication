package com.fuegofro.notifications_complication.phone.ui

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fuegofro.notifications_complication.phone.data.CurrentNotificationDataStore.Companion.currentNotificationDataStore

@Composable
fun MainScreen(onNavigateToPackagesSelection: () -> Unit) {
    Scaffold { paddingValues ->
        Column(Modifier.padding(paddingValues)) {
            val currentNotification =
                LocalContext.current.currentNotificationDataStore
                    .currentNotification()
                    .collectAsStateWithLifecycle(initialValue = null)
                    .value
            if (currentNotification != null) {
                Row {
                    Box {
                        currentNotification.largeIconBitmap()?.let {
                            Image(
                                it.asImageBitmap(),
                                contentDescription = "Large Icon",
                            )
                        }
                        Box(
                            modifier =
                                Modifier
                                    .background(MaterialTheme.colorScheme.background, CircleShape)
                                    .padding(4.dp)
                                    .align(Alignment.BottomEnd)
                        ) {
                            currentNotification.smallIconBitmap()?.let {
                                Icon(
                                    it.asImageBitmap(),
                                    contentDescription = "Small Icon",
                                    tint = Color(currentNotification.color),
                                    modifier = Modifier.size(12.dp).align(Alignment.Center)
                                )
                            }
                        }
                    }
                    Column {
                        currentNotification.title?.let { Text(it) }
                        currentNotification.body?.let { Text(it) }
                    }
                }
            } else {
                Text("No notification")
            }
            Button(onClick = onNavigateToPackagesSelection) { Text("Select Packages") }
        }
    }
}
