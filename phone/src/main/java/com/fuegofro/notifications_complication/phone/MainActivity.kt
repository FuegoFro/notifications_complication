package com.fuegofro.notifications_complication.phone

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.core.view.WindowCompat
import androidx.datastore.preferences.preferencesDataStore
import com.fuegofro.notifications_complication.phone.ui.PackageSelectionScreen
import com.fuegofro.notifications_complication.phone.ui.theme.NotificationsComplicationTheme

val Context.enabledPackagesDataStore by preferencesDataStore("enabledPackages")

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent { NotificationsComplicationTheme { PackageSelectionScreen() } }
    }
}
