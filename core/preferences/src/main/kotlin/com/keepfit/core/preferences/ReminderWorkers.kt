package com.keepfit.core.preferences

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters
import java.util.concurrent.TimeUnit

internal const val WORKOUT_REMINDER_WORK = "workout-reminder-work"
internal const val TRANSFORMATION_REMINDER_WORK = "transformation-reminder-work"
private const val WORKOUT_CHANNEL_ID = "keepfit-workout-reminders"
private const val PROGRESS_CHANNEL_ID = "keepfit-progress-reminders"

class WorkManagerReminderScheduler(
    private val context: Context,
) : ReminderScheduler {
    override suspend fun sync(settings: AppSettings) {
        val workManager = WorkManager.getInstance(context)
        if (settings.workoutReminder.enabled) {
            workManager.enqueueUniquePeriodicWork(
                WORKOUT_REMINDER_WORK,
                ExistingPeriodicWorkPolicy.UPDATE,
                PeriodicWorkRequestBuilder<WorkoutReminderWorker>(1, TimeUnit.DAYS)
                    .setInitialDelay(
                        computeDailyDelay(settings.workoutReminder.hour, settings.workoutReminder.minute),
                    )
                    .build(),
            )
        } else {
            workManager.cancelUniqueWork(WORKOUT_REMINDER_WORK)
        }

        if (settings.transformationReminder.enabled) {
            workManager.enqueueUniquePeriodicWork(
                TRANSFORMATION_REMINDER_WORK,
                ExistingPeriodicWorkPolicy.UPDATE,
                PeriodicWorkRequestBuilder<TransformationReminderWorker>(7, TimeUnit.DAYS)
                    .setInitialDelay(
                        computeWeeklyDelay(
                            settings.transformationReminder.dayOfWeek,
                            settings.transformationReminder.hour,
                            settings.transformationReminder.minute,
                        ),
                    )
                    .build(),
            )
        } else {
            workManager.cancelUniqueWork(TRANSFORMATION_REMINDER_WORK)
        }
    }

    private fun computeDailyDelay(hour: Int, minute: Int): Duration {
        val now = LocalDateTime.now()
        var next = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0)
        if (!next.isAfter(now)) {
            next = next.plusDays(1)
        }
        return Duration.between(now, next)
    }

    private fun computeWeeklyDelay(dayOfWeek: DayOfWeek, hour: Int, minute: Int): Duration {
        val now = LocalDateTime.now()
        var next = now
            .with(TemporalAdjusters.nextOrSame(dayOfWeek))
            .with(LocalTime.of(hour, minute))
            .withSecond(0)
            .withNano(0)
        if (!next.isAfter(now)) {
            next = next.plusWeeks(1)
        }
        return Duration.between(now, next)
    }
}

class WorkoutReminderWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        ReminderNotifications.ensureChannels(applicationContext)
        ReminderNotifications.show(
            context = applicationContext,
            channelId = WORKOUT_CHANNEL_ID,
            notificationId = 1001,
            title = "Workout reminder",
            message = "Open Keepfit and start today's training.",
        )
        return Result.success()
    }
}

class TransformationReminderWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        ReminderNotifications.ensureChannels(applicationContext)
        ReminderNotifications.show(
            context = applicationContext,
            channelId = PROGRESS_CHANNEL_ID,
            notificationId = 1002,
            title = "Progress reminder",
            message = "Log measurements or import this week's transformation photos.",
        )
        return Result.success()
    }
}

object ReminderNotifications {
    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                WORKOUT_CHANNEL_ID,
                "Workout reminders",
                NotificationManager.IMPORTANCE_DEFAULT,
            ),
        )
        manager.createNotificationChannel(
            NotificationChannel(
                PROGRESS_CHANNEL_ID,
                "Progress reminders",
                NotificationManager.IMPORTANCE_DEFAULT,
            ),
        )
    }

    fun show(
        context: Context,
        channelId: String,
        notificationId: Int,
        title: String,
        message: String,
    ) {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?.apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP) }
            ?: Intent().apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }
}
