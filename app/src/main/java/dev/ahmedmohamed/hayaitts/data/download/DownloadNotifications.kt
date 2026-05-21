package dev.ahmedmohamed.hayaitts.data.download

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import dev.ahmedmohamed.hayaitts.R
import dev.ahmedmohamed.hayaitts.domain.model.DownloadState
import kotlin.math.roundToInt

/**
 * Shared notification plumbing for [VoiceDownloadWorker]. Two channels:
 *
 *  - [CHANNEL_ACTIVE] - high-importance foreground channel hosting the
 *    "Downloading..." notification. Tapping launches the in-app Downloads
 *    screen via a deep link; the Cancel action broadcasts to
 *    [DownloadActionReceiver].
 *  - [CHANNEL_COMPLETE] - low-importance, one-shot notification posted on
 *    install / failure. Failure has a Retry action that re-enqueues the voice.
 *
 * All PendingIntents use FLAG_IMMUTABLE.
 *
 * Lives outside the worker so HayaiTtsApplication can prime channels on cold
 * start; creating them from inside the worker on a cold-boot foreground-
 * service promotion loses the race against the system watchdog.
 */
object DownloadNotifications {
    const val CHANNEL_ACTIVE = "downloads_active"
    const val CHANNEL_COMPLETE = "downloads_complete"
    const val NOTIFICATION_ID_ACTIVE = 1001
    private const val NOTIFICATION_ID_COMPLETE_BASE = 2000
    const val DEEP_LINK_DOWNLOADS = "hayaitts://downloads"

    /** Back-compat aliases - the existing worker imports these by name. */
    const val CHANNEL_ID = CHANNEL_ACTIVE
    const val NOTIFICATION_ID = NOTIFICATION_ID_ACTIVE

    fun ensureChannels(context: Context) {
        val nm = context.getSystemService<NotificationManager>() ?: return
        // Drop the legacy single-channel from <= 0.1.0 so users don't end up
        // with a stale, mis-importance channel hanging around.
        runCatching { nm.deleteNotificationChannel(LEGACY_CHANNEL_ID) }
        if (nm.getNotificationChannel(CHANNEL_ACTIVE) == null) {
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ACTIVE,
                    context.getString(R.string.download_active_channel_name),
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply {
                    description = context.getString(R.string.download_active_channel_description)
                    setShowBadge(false)
                },
            )
        }
        if (nm.getNotificationChannel(CHANNEL_COMPLETE) == null) {
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_COMPLETE,
                    context.getString(R.string.download_complete_channel_name),
                    NotificationManager.IMPORTANCE_LOW,
                ).apply {
                    description = context.getString(R.string.download_complete_channel_description)
                    setShowBadge(true)
                },
            )
        }
    }

    /** Back-compat alias for HayaiTtsApplication.onCreate. */
    fun ensureChannel(context: Context) = ensureChannels(context)

    /**
     * Determinate progress notification when totalBytes > 0; indeterminate
     * otherwise. Carries a Cancel action (broadcast) and a tap intent that
     * deep-links to the Downloads screen.
     */
    fun buildProgressNotification(
        context: Context,
        voiceId: String,
        title: String,
        progressBytes: Long,
        totalBytes: Long,
        status: String = DownloadState.STATUS_RUNNING,
    ): android.app.Notification {
        val determinate = totalBytes > 0
        val pct = if (determinate) ((progressBytes * 100f) / totalBytes).roundToInt().coerceIn(0, 100) else 0
        val bigText = if (determinate) {
            context.getString(R.string.download_notification_big_text, formatMb(progressBytes), formatMb(totalBytes))
        } else {
            context.getString(R.string.download_notification_big_text_indeterminate)
        }
        val contentText = if (status == DownloadState.STATUS_EXTRACTING) {
            context.getString(R.string.download_extracting)
        } else {
            context.getString(R.string.download_in_progress)
        }
        val cancelIntent = pendingBroadcast(context, voiceId, DownloadActionReceiver.ACTION_CANCEL, voiceId.hashCode())
        return NotificationCompat.Builder(context, CHANNEL_ACTIVE)
            .setContentTitle(title)
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setSmallIcon(R.drawable.ic_hayai_monochrome_launcher)
            .setProgress(100, pct, !determinate)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openDownloadsPendingIntent(context))
            .addAction(0, context.getString(R.string.download_notification_action_cancel), cancelIntent)
            .build()
    }

    /**
     * Legacy three-arg signature kept so the existing worker compiles without
     * touching every call site. Always indeterminate; new callers should use
     * the byte-aware overload above.
     */
    fun buildProgressNotification(
        context: Context,
        title: String,
        progressPct: Int,
        indeterminate: Boolean,
    ): android.app.Notification = NotificationCompat.Builder(context, CHANNEL_ACTIVE)
        .setContentTitle(title)
        .setContentText(context.getString(R.string.download_in_progress))
        .setSmallIcon(R.drawable.ic_hayai_monochrome_launcher)
        .setProgress(100, progressPct.coerceIn(0, 100), indeterminate)
        .setOngoing(true)
        .setOnlyAlertOnce(true)
        .setContentIntent(openDownloadsPendingIntent(context))
        .build()

    fun postInstalledNotification(context: Context, voiceId: String, title: String) {
        ensureChannels(context)
        val nm = context.getSystemService<NotificationManager>() ?: return
        val n = NotificationCompat.Builder(context, CHANNEL_COMPLETE)
            .setContentTitle(context.getString(R.string.download_notification_installed_title, title))
            .setContentText(context.getString(R.string.download_notification_installed_text))
            .setSmallIcon(R.drawable.ic_hayai_monochrome_launcher)
            .setAutoCancel(true)
            .setContentIntent(openDownloadsPendingIntent(context))
            .build()
        runCatching { nm.notify(completionId(voiceId), n) }
    }

    fun postFailedNotification(context: Context, voiceId: String, title: String, reason: String) {
        ensureChannels(context)
        val nm = context.getSystemService<NotificationManager>() ?: return
        val retryIntent = pendingBroadcast(context, voiceId, DownloadActionReceiver.ACTION_RETRY, voiceId.hashCode() xor 0x1)
        val n = NotificationCompat.Builder(context, CHANNEL_COMPLETE)
            .setContentTitle(context.getString(R.string.download_notification_failed_title, title))
            .setContentText(context.getString(R.string.download_notification_failed_text, reason))
            .setStyle(NotificationCompat.BigTextStyle().bigText(reason))
            .setSmallIcon(R.drawable.ic_hayai_monochrome_launcher)
            .setAutoCancel(true)
            .setContentIntent(openDownloadsPendingIntent(context))
            .addAction(0, context.getString(R.string.download_notification_action_retry), retryIntent)
            .build()
        runCatching { nm.notify(completionId(voiceId), n) }
    }

    fun cancelActive(context: Context) {
        val nm = context.getSystemService<NotificationManager>() ?: return
        runCatching { nm.cancel(NOTIFICATION_ID_ACTIVE) }
    }

    private fun openDownloadsPendingIntent(context: Context): PendingIntent {
        val intent = Intent(Intent.ACTION_VIEW, DEEP_LINK_DOWNLOADS.toUri())
            .setPackage(context.packageName)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    private fun pendingBroadcast(context: Context, voiceId: String, action: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, DownloadActionReceiver::class.java)
            .setAction(action)
            .setPackage(context.packageName)
            .putExtra(DownloadActionReceiver.EXTRA_VOICE_ID, voiceId)
        return PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    private fun completionId(voiceId: String): Int =
        NOTIFICATION_ID_COMPLETE_BASE + (voiceId.hashCode() and 0x0000_FFFF)

    private fun formatMb(bytes: Long): String {
        if (bytes <= 0L) return "0 MB"
        val mb = bytes.toDouble() / 1024.0 / 1024.0
        return if (mb < 1.0) "${(bytes / 1024L)} KB" else "${mb.roundToInt()} MB"
    }

    private const val LEGACY_CHANNEL_ID = "downloads"
}
