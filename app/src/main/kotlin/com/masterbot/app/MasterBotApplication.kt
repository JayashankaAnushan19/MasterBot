package com.masterbot.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.masterbot.app.data.sync.SyncCoordinator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class MasterBotApplication : Application() {

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val syncCoordinator: SyncCoordinator by lazy { SyncCoordinator(this, applicationScope) }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        syncCoordinator // touch the lazy to start the initial sync at launch
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            REMINDER_CHANNEL_ID,
            "Daily review reminder",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Reminds you if today's review goal hasn't been met yet."
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val REMINDER_CHANNEL_ID = "daily_reminder"
    }
}
