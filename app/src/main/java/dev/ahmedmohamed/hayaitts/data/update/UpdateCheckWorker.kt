package dev.ahmedmohamed.hayaitts.data.update

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import co.touchlab.kermit.Logger
import dev.ahmedmohamed.hayaitts.R
import org.koin.core.context.GlobalContext
import java.util.concurrent.TimeUnit

/**
 * Periodic worker that polls GitHub Releases via [UpdateChecker] and posts a
 * notification when a newer build is available for the user's current channel.
 *
 * Scheduled by [HayaiTtsApplication] on every cold start with
 * [ExistingPeriodicWorkPolicy.KEEP] so we don't spam Work history. Inside the
 * 6h debounce in [UpdateChecker] no-ops the network call, so the worker is
 * cheap on the wire.
 */
class UpdateCheckWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    private val log = Logger.withTag("UpdateCheckWorker")

    override suspend fun doWork(): Result {
        val checker = runCatching {
            GlobalContext.get().get<UpdateChecker>()
        }.getOrElse {
            log.w(it) { "UpdateChecker not yet available; backing off" }
            return Result.retry()
        }
        return when (val status = checker.check(force = false)) {
            is UpdateStatus.Available -> {
                notify(applicationContext, status.tag, status.universalApkUrl)
                Result.success()
            }
            is UpdateStatus.UpToDate -> Result.success()
            is UpdateStatus.Failed -> Result.retry()
            else -> Result.success()
        }
    }

    private fun notify(ctx: Context, tag: String, url: String) {
        val nm = ctx.getSystemService<NotificationManager>() ?: return
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                ctx.getString(R.string.updates_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = ctx.getString(R.string.updates_channel_description)
            }
            nm.createNotificationChannel(channel)
        }
        val launchIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        val pi = PendingIntent.getActivity(
            ctx, 0, launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val n = NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("HayaiTTS $tag available")
            .setContentText("Tap to view the release notes.")
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()
        nm.notify(NOTIFICATION_ID, n)
    }

    companion object {
        const val WORK_NAME = "hayaitts_update_check"
        const val CHANNEL_ID = "hayai_tts_updates"
        const val NOTIFICATION_ID = 4243

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val request = PeriodicWorkRequestBuilder<UpdateCheckWorker>(
                12, TimeUnit.HOURS,
            )
                .setConstraints(constraints)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }
    }
}
