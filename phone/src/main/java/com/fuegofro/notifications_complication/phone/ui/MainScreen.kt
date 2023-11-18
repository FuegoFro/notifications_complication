package com.fuegofro.notifications_complication.phone.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
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
                    currentNotification.smallIconBitmap()?.let {
                        Icon(it.asImageBitmap(), contentDescription = "Small Icon")
                    }
                    currentNotification.largeIconBitmap()?.let {
                        Icon(it.asImageBitmap(), contentDescription = "Large Icon")
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
