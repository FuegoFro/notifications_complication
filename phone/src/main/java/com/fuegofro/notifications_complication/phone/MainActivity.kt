package com.fuegofro.notifications_complication.phone

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.fuegofro.notifications_complication.phone.ui.MainScreen
import com.fuegofro.notifications_complication.phone.ui.PackageSelectionScreen
import com.fuegofro.notifications_complication.phone.ui.theme.NotificationsComplicationTheme

private data object NavScreens {
    const val MAIN = "main"
    const val PACKAGES_SELECTION = "PACKAGES_SELECTION"
}

class MainActivity : ComponentActivity() {

    private val serviceConnection =
        object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                Log.e("ServiceConnection", "onServiceConnected")
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                Log.e("ServiceConnection", "onServiceDisconnected")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        bindService(Intent(this, NotificationListener::class.java), serviceConnection, 0)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            val navController = rememberNavController()

            NotificationsComplicationTheme {
                NavHost(navController = navController, startDestination = NavScreens.MAIN) {
                    composable(NavScreens.MAIN) {
                        MainScreen(
                            onNavigateToPackagesSelection = {
                                navController.navigate(NavScreens.PACKAGES_SELECTION)
                            }
                        )
                    }
                    composable(NavScreens.PACKAGES_SELECTION) {
                        PackageSelectionScreen(onNavigateUp = navController::navigateUp)
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(serviceConnection)
    }
}
