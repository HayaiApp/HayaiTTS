package dev.ahmedmohamed.hayaitts.data.download

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import dev.ahmedmohamed.hayaitts.R

/**
 * Shared notification plumbing for [VoiceDownloadWorker]. Lives outside the
 * worker so [dev.ahmedmohamed.hayaitts.app.HayaiTtsApplication] can prime the
 * channel on app start (channels created from a worker on a cold start lose
 * the race against the system's foreground-service watchdog).
 */
object DownloadNotifications {
    const val CHANNEL_ID = "downloads"
    const val NOTIFICATION_ID = 1001

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService<NotificationManager>() ?: return
        if (nm.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.download_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = context.getString(R.string.download_channel_description)
            setShowBadge(false)
        }
        nm.createNotificationChannel(channel)
    }

    fun buildProgressNotification(
        context: Context,
        title: String,
        progressPct: Int,
        indeterminate: Boolean,
    ): android.app.Notification = NotificationCompat.Builder(context, CHANNEL_ID)
        .setContentTitle(title)
        .setContentText(context.getString(R.string.download_in_progress))
        .setSmallIcon(R.drawable.ic_hayai_monochrome_launcher)
        .setProgress(100, progressPct.coerceIn(0, 100), indeterminate)
        .setOngoing(true)
        .setOnlyAlertOnce(true)
        .build()
}
