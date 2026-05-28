package dev.ahmedmohamed.hayaitts.data.download

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import co.touchlab.kermit.Logger
import dev.ahmedmohamed.hayaitts.core.dispatchers.DispatcherProvider
import dev.ahmedmohamed.hayaitts.data.db.dao.DownloadStateDao
import dev.ahmedmohamed.hayaitts.domain.model.DownloadState
import dev.ahmedmohamed.hayaitts.domain.model.VoiceCard
import dev.ahmedmohamed.hayaitts.domain.repo.DownloadRepository
import dev.ahmedmohamed.hayaitts.domain.repo.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * Façade over [WorkManager] that:
 *   - Persists "this voice is queued" in Room so the UI can show progress
 *     even before the worker actually starts.
 *   - Reads `SettingsRepository.wifiOnly` to pick the network constraint.
 *   - Maps the Room state row back to the in-memory [DownloadState] sealed
 *     class so consumers do not parse status strings themselves.
 */
class DownloadRepositoryImpl(
    private val context: Context,
    private val downloadStateDao: DownloadStateDao,
    private val settings: SettingsRepository,
    private val appScope: CoroutineScope,
    private val dispatchers: DispatcherProvider,
) : DownloadRepository {

    private val log = Logger.withTag("DownloadRepository")

    override val states: Flow<Map<String, DownloadState>> =
        downloadStateDao.getAll().map { rows ->
            rows.associate { row ->
                row.voiceId to when (row.status) {
                    DownloadState.STATUS_QUEUED -> DownloadState.Queued
                    DownloadState.STATUS_RUNNING -> {
                        val pct = if (row.totalBytes > 0) {
                            row.progressBytes.toFloat() / row.totalBytes.toFloat()
                        } else 0f
                        DownloadState.Running(
                            pct = pct.coerceIn(0f, 1f),
                            downloadedBytes = row.progressBytes,
                            totalBytes = row.totalBytes,
                        )
                    }
                    DownloadState.STATUS_EXTRACTING -> {
                        val pct = if (row.totalBytes > 0) {
                            row.progressBytes.toFloat() / row.totalBytes.toFloat()
                        } else 0f
                        DownloadState.Extracting(pct = pct.coerceIn(0f, 1f))
                    }
                    DownloadState.STATUS_DONE -> DownloadState.Done
                    DownloadState.STATUS_FAILED -> DownloadState.Failed(row.errorMessage ?: "Unknown error")
                    DownloadState.STATUS_CANCELLED -> DownloadState.Cancelled
                    else -> DownloadState.Idle
                }
            }
        }.stateIn(appScope, SharingStarted.Eagerly, emptyMap())

    override fun enqueue(voiceCard: VoiceCard) {
        // Resolve the wifi-only constraint synchronously off the DataStore.
        // It's a single key read, and the alternative — kicking off the
        // enqueue from a coroutine — bleeds async lifecycle into UI callers.
        val wifiOnly = runBlocking(dispatchers.io) { settings.wifiOnly.first() }
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED)
            .build()

        // Pass only the id + title — WorkManager's Data is capped at 10 KB
        // and voice cards with large speaker lists (Kokoro: hundreds of
        // speakers) blow past that as JSON. The worker resolves the full
        // VoiceCard from CatalogRepository at start-of-work.
        val input = Data.Builder()
            .putString(VoiceDownloadWorker.KEY_VOICE_ID, voiceCard.id)
            .putString(VoiceDownloadWorker.KEY_TITLE, voiceCard.title)
            .build()

        val builder = OneTimeWorkRequestBuilder<VoiceDownloadWorker>()
            .setConstraints(constraints)
            .setInputData(input)
            // Exponential 10s, 20s, 40s, 80s... — caps at the WorkManager
            // default (5h). The worker returns Result.retry() only for
            // transient failures (5xx / IOException); hard errors (404,
            // checksum mismatch) return Result.failure() and exit instantly.
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            builder.setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
        }

        // Optimistically write a "queued" row so the UI can react before the
        // worker actually picks up.
        appScope.launch(dispatchers.io) {
            downloadStateDao.upsert(
                dev.ahmedmohamed.hayaitts.data.db.entities.DownloadStateEntity(
                    voiceId = voiceCard.id,
                    status = DownloadState.STATUS_QUEUED,
                    progressBytes = 0L,
                    totalBytes = 0L,
                    errorMessage = null,
                    updatedAt = System.currentTimeMillis(),
                ),
            )
        }

        WorkManager.getInstance(context).enqueueUniqueWork(
            VoiceDownloadWorker.uniqueName(voiceCard.id),
            ExistingWorkPolicy.KEEP,
            builder.build(),
        )
        log.i { "Enqueued download for ${voiceCard.id} (wifiOnly=$wifiOnly)" }
    }

    override fun cancel(voiceId: String) {
        WorkManager.getInstance(context).cancelUniqueWork(VoiceDownloadWorker.uniqueName(voiceId))
        appScope.launch(dispatchers.io) {
            downloadStateDao.upsert(
                dev.ahmedmohamed.hayaitts.data.db.entities.DownloadStateEntity(
                    voiceId = voiceId,
                    status = DownloadState.STATUS_CANCELLED,
                    progressBytes = 0L,
                    totalBytes = 0L,
                    errorMessage = null,
                    updatedAt = System.currentTimeMillis(),
                ),
            )
        }
    }

    override suspend fun clearOne(voiceId: String) {
        withContext(dispatchers.io) { downloadStateDao.deleteById(voiceId) }
    }

    override suspend fun clearCompleted() {
        withContext(dispatchers.io) {
            val terminalStatuses = setOf(
                DownloadState.STATUS_DONE,
                DownloadState.STATUS_FAILED,
                DownloadState.STATUS_CANCELLED,
            )
            val rows = downloadStateDao.getAll().first()
            rows.asSequence()
                .filter { it.status in terminalStatuses }
                .forEach { downloadStateDao.deleteById(it.voiceId) }
        }
    }
}
