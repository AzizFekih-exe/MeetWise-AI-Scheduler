package com.example.meetwise_ai_scheduler.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.delay

class TranscriptionWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val jobId = inputData.getString("jobId") ?: return Result.failure()
        val meetingId = inputData.getInt("meetingId", -1)

        var isComplete = false
        var attempts = 0
        val maxAttempts = 60 // 10 minutes (10s * 60)

        while (!isComplete && attempts < maxAttempts) {
            val status = pollServer(jobId) 

            when (status) {
                "done" -> {
                    showFinishedNotification("Minutes Ready", "Your meeting minutes have been generated.")
                    isComplete = true
                }
                "failed" -> {
                    showFinishedNotification("Transcription Failed", "There was an error processing your audio.")
                    return Result.failure()
                }
                else -> {
                    attempts++
                    delay(10000) // Wait 10 seconds before next poll
                }
            }
        }

        return if (isComplete) Result.success() else Result.retry()
    }

    private fun pollServer(jobId: String): String {
        return "processing"
    }

    private fun showFinishedNotification(title: String, message: String) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "minutes_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Meeting Minutes", NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
