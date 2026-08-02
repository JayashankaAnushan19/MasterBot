package com.masterbot.app.notifications

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.masterbot.app.MainActivity
import com.masterbot.app.MasterBotApplication
import com.masterbot.app.data.db.AppDatabase
import java.time.LocalDate

/** Daily check: if today's review goal hasn't been met yet, nudge with a local notification. */
class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val progress = AppDatabase.get(applicationContext).dao().userProgress()
        val today = LocalDate.now().toEpochDay()
        val goalMetToday = progress?.lastGoalMetEpochDay == today
        if (!goalMetToday) {
            postReminder()
        }
        return Result.success()
    }

    private fun postReminder() {
        val context = applicationContext
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val openAppIntent = PendingIntent.getActivity(
            context,
            0,
            android.content.Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, MasterBotApplication.REMINDER_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Keep your streak alive 🔥")
            .setContentText("You haven't hit today's review goal yet.")
            .setContentIntent(openAppIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(REMINDER_NOTIFICATION_ID, notification)
    }

    companion object {
        private const val REMINDER_NOTIFICATION_ID = 1001
    }
}
