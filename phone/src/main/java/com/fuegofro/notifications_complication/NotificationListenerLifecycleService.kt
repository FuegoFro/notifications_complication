package com.fuegofro.notifications_complication

import android.content.Intent
import android.os.IBinder
import android.service.notification.NotificationListenerService
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ServiceLifecycleDispatcher

/**
 * A version of [NotificationListenerService] that also implements [LifecycleOwner]
 */
abstract class NotificationListenerLifecycleService: NotificationListenerService(), LifecycleOwner {
    // The only thing from `this` that should be accessed is the `lifecycle` property, which is
    // fully defined here in a self-referential way.
    @Suppress("LeakingThis")
    private val serviceLifecycleDispatcher = ServiceLifecycleDispatcher(this)

    final override val lifecycle: Lifecycle
        get() = serviceLifecycleDispatcher.lifecycle


    override fun onCreate() {
        serviceLifecycleDispatcher.onServicePreSuperOnCreate()
        super.onCreate()
    }

    override fun onBind(intent: Intent?): IBinder? {
        serviceLifecycleDispatcher.onServicePreSuperOnBind()
        return super.onBind(intent)
    }

    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    override fun onStart(intent: Intent?, startId: Int) {
        serviceLifecycleDispatcher.onServicePreSuperOnStart()
        super.onStart(intent, startId)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        serviceLifecycleDispatcher.onServicePreSuperOnStart()
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        serviceLifecycleDispatcher.onServicePreSuperOnDestroy()
        super.onDestroy()
    }
}
