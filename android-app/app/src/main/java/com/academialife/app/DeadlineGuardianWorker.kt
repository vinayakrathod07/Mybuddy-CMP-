package com.academialife.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class DeadlineGuardianWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val CHANNEL_DEADLINES = "mybuddy_deadlines_channel"
        const val CHANNEL_CHECKIN = "mybuddy_checkin_channel"
        const val WORK_NAME = "mybuddy_deadline_guardian_work"

        fun schedulePeriodicCheck(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val workRequest = PeriodicWorkRequestBuilder<DeadlineGuardianWorker>(4, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }
    }

    override suspend fun doWork(): Result {
        try {
            createNotificationChannels()

            if (!FirebaseRepository.isUserSignedIn()) return Result.success()
            val userId = FirebaseRepository.getCurrentUserId()
            val tasks = FirebaseRepository.getTasks()
            val pending = tasks.filter { it.status != "completed" }

            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val todayStr = sdf.format(Date())
            val tomorrowStr = sdf.format(Date(System.currentTimeMillis() + 86400000L))

            val dueToday = pending.filter { it.dueDate == todayStr }
            val dueTomorrow = pending.filter { it.dueDate == tomorrowStr }
            val overdue = pending.filter { it.dueDate < todayStr }

            if (dueToday.isNotEmpty() || overdue.isNotEmpty()) {
                val title = when {
                    overdue.isNotEmpty() -> "⚠️ ${overdue.size} Overdue Assignment(s) Need Attention!"
                    dueToday.size == 1 -> "⚡ Due Today: ${dueToday.first().title}"
                    else -> "⚡ ${dueToday.size} Assignments Due Today!"
                }

                val body = when {
                    overdue.isNotEmpty() -> "You have overdue coursework. Tap to open MyBuddy Planner and clear them."
                    else -> dueToday.joinToString(", ") { it.title }.take(100)
                }

                showNotification(
                    id = 1001,
                    title = title,
                    message = body,
                    channelId = CHANNEL_DEADLINES
                )
            } else if (dueTomorrow.isNotEmpty()) {
                showNotification(
                    id = 1002,
                    title = "📅 Heads up: ${dueTomorrow.size} task(s) due tomorrow",
                    message = dueTomorrow.joinToString(", ") { it.title }.take(100),
                    channelId = CHANNEL_DEADLINES
                )
            }

            return Result.success()
        } catch (_: Exception) {
            return Result.retry()
        }
    }

    private fun showNotification(id: Int, title: String, message: String, channelId: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.mybuddy_symbol_mark)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            val manager = NotificationManagerCompat.from(context)
            if (manager.areNotificationsEnabled()) {
                manager.notify(id, notification)
            }
        } catch (_: SecurityException) {}
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val deadlineChannel = NotificationChannel(
                CHANNEL_DEADLINES,
                "Deadlines & Exam Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifies when student coursework, assignments, or examinations are due"
                enableLights(true)
                enableVibration(true)
            }

            val checkinChannel = NotificationChannel(
                CHANNEL_CHECKIN,
                "Daily Check-In & Habits",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Daily check-in reminders and habit encouragement"
            }

            manager.createNotificationChannel(deadlineChannel)
            manager.createNotificationChannel(checkinChannel)
        }
    }
}

