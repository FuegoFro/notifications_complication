package com.fuegofro.notifications_complication.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun MainScreen(onNavigateToPackagesSelection: () -> Unit, onNavigateToNotificationsDebug: () -> Unit) {
    Scaffold { paddingValues ->
        Column(Modifier.padding(paddingValues)) {
            Button(onClick = onNavigateToPackagesSelection) { Text("Select Packages") }
            Button(onClick = onNavigateToNotificationsDebug) { Text("Debug notifications") }
        }
    }
}
