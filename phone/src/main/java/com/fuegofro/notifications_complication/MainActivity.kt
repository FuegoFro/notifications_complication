package com.fuegofro.notifications_complication

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.fuegofro.notifications_complication.ui.MainScreen
import com.fuegofro.notifications_complication.ui.NotificationsDebugScreen
import com.fuegofro.notifications_complication.ui.PackageSelectionScreen
import com.fuegofro.notifications_complication.ui.theme.NotificationsComplicationTheme
import kotlinx.coroutines.flow.MutableStateFlow

private data object NavScreens {
    const val MAIN = "MAIN"
    const val PACKAGES_SELECTION = "PACKAGES_SELECTION"
    const val NOTIFICATIONS_DEBUG = "NOTIFICATIONS_DEBUG"
}

class MainActivity : ComponentActivity() {
    private val serviceBinderFlow =
        MutableStateFlow<NotificationListener.NotificationListenerBinder?>(null)

    private val serviceConnection =
        object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                // Log.e("ServiceConnection", "onServiceConnected")
                serviceBinderFlow.value = service as NotificationListener.NotificationListenerBinder
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                // Log.e("ServiceConnection", "onServiceDisconnected")
                serviceBinderFlow.value = null
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
                            },
                            onNavigateToNotificationsDebug = {
                                navController.navigate(NavScreens.NOTIFICATIONS_DEBUG)
                            }
                        )
                    }
                    composable(NavScreens.PACKAGES_SELECTION) {
                        PackageSelectionScreen(
                            onNavigateUp = navController::navigateUp,
                            notificationListenerBinderFlow = serviceBinderFlow,
                        )
                    }
                    composable(NavScreens.NOTIFICATIONS_DEBUG) {
                        NotificationsDebugScreen(
                            onNavigateUp = navController::navigateUp,
                            notificationListenerBinderFlow = serviceBinderFlow,
                        )
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
