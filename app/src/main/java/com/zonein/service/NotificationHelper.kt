package com.zonein.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.zonein.MainActivity

class NotificationHelper(private val context: Context) {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        const val CHANNEL_ID = "ZoneinTimerChannel"
        const val NOTIFICATION_ID = 1
    }

    fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Zonein Timer",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Ongoing timer for Zonein focus sessions"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun buildNotification(time: String, isPaused: Boolean): Notification {
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        // Pause/Resume Action
        val pauseResumeAction = if (isPaused) {
            val resumeIntent = Intent(context, TimerService::class.java).apply { action = TimerService.ACTION_START }
            val resumePendingIntent = PendingIntent.getService(context, 1, resumeIntent, PendingIntent.FLAG_IMMUTABLE)
            NotificationCompat.Action(android.R.drawable.ic_media_play, "Resume", resumePendingIntent)
        } else {
            val pauseIntent = Intent(context, TimerService::class.java).apply { action = TimerService.ACTION_PAUSE }
            val pausePendingIntent = PendingIntent.getService(context, 2, pauseIntent, PendingIntent.FLAG_IMMUTABLE)
            NotificationCompat.Action(android.R.drawable.ic_media_pause, "Pause", pausePendingIntent)
        }

        // Reset Action
        val resetIntent = Intent(context, TimerService::class.java).apply { action = TimerService.ACTION_RESET }
        val resetPendingIntent = PendingIntent.getService(context, 3, resetIntent, PendingIntent.FLAG_IMMUTABLE)
        val resetAction = NotificationCompat.Action(android.R.drawable.ic_menu_close_clear_cancel, "Reset", resetPendingIntent)


        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Zonein Timer")
            .setContentText("Time remaining: $time")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .addAction(pauseResumeAction)
            .addAction(resetAction)
            .build()
    }
}
