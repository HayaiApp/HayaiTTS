package dev.ahmedmohamed.hayaitts.tts

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import dev.ahmedmohamed.hayaitts.R

/**
 * "HayaiTTS is speaking" foreground nudge. The [HayaiTtsService] starts this
 * when synthesis kicks off on behalf of an external app (e.g., Hayai's novel
 * reader); the user sees an ongoing notification with a Stop action so they
 * always have a kill switch even when the host app's controls are hidden.
 *
 * Channel importance is LOW so the nudge never plays a sound or vibrates —
 * it's purely an informational + control surface.
 *
 * Off by default; toggled via Settings → Power-user. The service checks the
 * pref before calling [post].
 */
object HayaiTtsNudge {

    const val CHANNEL_ID = "hayai_tts_nudge"
    const val NOTIFICATION_ID = 4242
    const val ACTION_STOP = "dev.ahmedmohamed.hayaitts.action.STOP_TTS"

    fun ensureChannel(context: Context) {
        val nm = context.getSystemService<NotificationManager>() ?: return
        if (nm.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.tts_nudge_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = context.getString(R.string.tts_nudge_channel_description)
            setShowBadge(false)
        }
        nm.createNotificationChannel(channel)
    }

    fun build(context: Context): Notification {
        ensureChannel(context)
        val stopIntent = Intent(ACTION_STOP).apply {
            `package` = context.packageName
        }
        val stopPi = PendingIntent.getBroadcast(
            context, 0, stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(context.getString(R.string.tts_nudge_title))
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(
                0,
                context.getString(R.string.tts_nudge_action_stop),
                stopPi,
            )
            .build()
    }

    fun post(context: Context) {
        val nm = context.getSystemService<NotificationManager>() ?: return
        nm.notify(NOTIFICATION_ID, build(context))
    }

    fun cancel(context: Context) {
        val nm = context.getSystemService<NotificationManager>() ?: return
        nm.cancel(NOTIFICATION_ID)
    }
}
