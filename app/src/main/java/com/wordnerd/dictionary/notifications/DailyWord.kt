package com.wordnerd.dictionary.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.wordnerd.dictionary.MainActivity
import com.wordnerd.dictionary.R
import com.wordnerd.dictionary.data.HistoryRepository
import com.wordnerd.dictionary.data.SettingsRepository
import com.wordnerd.dictionary.data.Word
import com.wordnerd.dictionary.data.wordOfTheDay
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

object DailyWordNotifier {
    private const val CHANNEL_ID = "daily_word"
    private const val WORK_NAME = "daily_word"
    private const val NOTIFICATION_ID = 1
    const val EXTRA_WORD_ID = "word_id"

    /**
     * Schedules the next notification at [hour]:[minute]. Uses a one-off job that re-arms itself
     * after each run, so it fires at the chosen time every day instead of drifting the way a
     * 24-hour periodic job can. WorkManager keeps the job across reboots.
     */
    fun schedule(context: Context, hour: Int, minute: Int, replace: Boolean = true) {
        val now = LocalDateTime.now()
        var next = now.with(LocalTime.of(hour, minute))
        if (!next.isAfter(now)) next = next.plusDays(1)
        val delay = Duration.between(now, next).toMillis()

        val request = OneTimeWorkRequestBuilder<DailyWordWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            WORK_NAME,
            if (replace) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP,
            request,
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    fun canPost(context: Context): Boolean =
        (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED) &&
            NotificationManagerCompat.from(context).areNotificationsEnabled()

    fun show(context: Context, word: Word = wordOfTheDay()) {
        if (!canPost(context)) return
        ensureChannel(context)

        val open = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_WORD_ID, word.id)
        }
        val pending = PendingIntent.getActivity(
            context, 0, open,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val body = "${word.partOfSpeech} · ${word.definition}"
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Word of the day: ${word.word}")
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$body\n\n“${word.example}”"))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // Permission revoked between the check and the post; nothing to do.
        }
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Word of the day", NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = "A new obscure word every day" },
        )
    }
}

class DailyWordWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val settings = SettingsRepository(applicationContext).current()
        if (!settings.enabled) return Result.success()
        HistoryRepository(applicationContext).recordThrough()
        DailyWordNotifier.show(applicationContext)
        DailyWordNotifier.schedule(applicationContext, settings.hour, settings.minute)
        return Result.success()
    }
}
