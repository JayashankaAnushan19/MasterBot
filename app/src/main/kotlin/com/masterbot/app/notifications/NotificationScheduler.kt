package com.masterbot.app.notifications

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime

/** Schedules/cancels the daily reminder check (see [ReminderWorker]). */
object NotificationScheduler {

    private const val WORK_NAME = "daily_reminder_check"
    private val TARGET_TIME: LocalTime = LocalTime.of(20, 0)

    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<ReminderWorker>(Duration.ofHours(24))
            .setInitialDelay(delayUntilNextTarget())
            .build()

        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    private fun delayUntilNextTarget(): Duration {
        val now = LocalDateTime.now()
        var target = now.toLocalDate().atTime(TARGET_TIME)
        if (!target.isAfter(now)) {
            target = target.plusDays(1)
        }
        return Duration.between(now, target)
    }
}
