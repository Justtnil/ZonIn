package com.zonein.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import kotlinx.coroutines.*

class TimerService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var notificationHelper: NotificationHelper
    private lateinit var broadcaster: LocalBroadcastManager
    private var timerJob: Job? = null

    companion object {
        const val ACTION_START = "com.zonein.service.START"
        const val ACTION_PAUSE = "com.zonein.service.PAUSE"
        const val ACTION_RESET = "com.zonein.service.RESET"
        const val EXTRA_TIME_MS = "com.zonein.service.EXTRA_TIME_MS"

        const val BROADCAST_ACTION_TIME_UPDATE = "com.zonein.service.TIME_UPDATE"
        const val BROADCAST_EXTRA_TIME_MS = "com.zonein.service.broadcast.EXTRA_TIME_MS"
        const val BROADCAST_ACTION_TIMER_FINISHED = "com.zonein.service.TIMER_FINISHED"
    }

    override fun onCreate() {
        super.onCreate()
        notificationHelper = NotificationHelper(this)
        notificationHelper.createNotificationChannel()
        broadcaster = LocalBroadcastManager.getInstance(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val timeMs = intent.getLongExtra(EXTRA_TIME_MS, 0)
                startTimer(timeMs)
            }
            ACTION_PAUSE -> pauseTimer()
            ACTION_RESET -> resetTimer()
        }
        return START_STICKY
    }

    private fun startTimer(timeMs: Long) {
        timerJob?.cancel()
        var remainingTime = timeMs
        timerJob = serviceScope.launch {
            while (remainingTime > 0) {
                val notification = notificationHelper.buildNotification(formatTime(remainingTime), false)
                startForeground(NotificationHelper.NOTIFICATION_ID, notification)

                // Broadcast time update
                val intent = Intent(BROADCAST_ACTION_TIME_UPDATE)
                intent.putExtra(BROADCAST_EXTRA_TIME_MS, remainingTime)
                broadcaster.sendBroadcast(intent)

                delay(1000)
                remainingTime -= 1000
            }
            // Broadcast timer finished
            broadcaster.sendBroadcast(Intent(BROADCAST_ACTION_TIMER_FINISHED))
            stopSelf() // Stop service when timer finishes
        }
    }

    private fun pauseTimer() {
        timerJob?.cancel()
        val notification = notificationHelper.buildNotification("Paused", true)
        startForeground(NotificationHelper.NOTIFICATION_ID, notification)
        // We stop the service but don't remove the notification, so it stays as "Paused"
        stopForeground(false)
    }

    private fun resetTimer() {
        timerJob?.cancel()
        stopForeground(true) // true = remove notification
        stopSelf()
    }

    private fun formatTime(ms: Long): String {
        val minutes = (ms / 1000) / 60
        val seconds = (ms / 1000) % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    override fun onDestroy() {
        super.onDestroy()
        timerJob?.cancel()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null // Not a bound service
    }
}
