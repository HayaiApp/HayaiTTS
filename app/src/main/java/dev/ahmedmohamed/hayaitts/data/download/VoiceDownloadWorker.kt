package dev.ahmedmohamed.hayaitts.data.download

import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import co.touchlab.kermit.Logger
import dev.ahmedmohamed.hayaitts.data.catalog.catalogJson
import dev.ahmedmohamed.hayaitts.data.db.dao.DownloadStateDao
import dev.ahmedmohamed.hayaitts.data.db.entities.DownloadStateEntity
import dev.ahmedmohamed.hayaitts.domain.model.DownloadState
import dev.ahmedmohamed.hayaitts.domain.model.VoiceCard
import dev.ahmedmohamed.hayaitts.domain.repo.VoiceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import okhttp3.OkHttpClient
import okhttp3.Request
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import org.koin.core.context.GlobalContext
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest

/**
 * Background worker that downloads + extracts one voice bundle.
 *
 * Lifecycle:
 *   1. Mark `running` with totalBytes=0.
 *   2. GET the bundle URL, stream to `cacheDir/downloads/<id>.tar.bz2.part`.
 *   3. Verify sha256 (if present in input data). Mismatch -> fail.
 *   4. Mark `extracting`.
 *   5. Extract to `filesDir/voices/<id>/` via Commons Compress.
 *   6. Validate model.onnx + tokens.txt exist; otherwise fail.
 *   7. Upsert InstalledVoice. Delete cache file.
 *   8. Mark `done`.
 *
 * On any exception we mark `failed` with the exception message and leave any
 * partially-extracted dir in place — Phase 7 will handle cleanup-on-failure.
 */
class VoiceDownloadWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    private val log = Logger.withTag("VoiceDownloadWorker")

    // Resolved lazily from Koin: the worker has no constructor injection and
    // we do not want to spin up a fresh OkHttp on every run.
    private val koin get() = GlobalContext.get()
    private val downloadStateDao: DownloadStateDao by lazy { koin.get() }
    private val voiceRepository: VoiceRepository by lazy { koin.get() }
    private val okHttp: OkHttpClient by lazy { koin.get() }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val title = inputData.getString(KEY_TITLE) ?: "HayaiTTS"
        val notification = DownloadNotifications.buildProgressNotification(
            applicationContext,
            title = title,
            progressPct = 0,
            indeterminate = true,
        )
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                DownloadNotifications.NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
            )
        } else {
            ForegroundInfo(DownloadNotifications.NOTIFICATION_ID, notification)
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val voiceCardJson = inputData.getString(KEY_VOICE_JSON)
            ?: return@withContext failed("voiceCard JSON missing")
        val voice = runCatching { catalogJson.decodeFromString<VoiceCard>(voiceCardJson) }
            .getOrElse { return@withContext failed("Bad voiceCard JSON: ${it.message}") }

        DownloadNotifications.ensureChannel(applicationContext)
        setForegroundSafely(voice.title, pct = 0, indeterminate = true)

        upsertState(voice.id, DownloadState.STATUS_RUNNING, 0L, 0L, null)

        val downloadsDir = File(applicationContext.cacheDir, "downloads").apply { mkdirs() }
        val partFile = File(downloadsDir, "${voice.id}.tar.bz2.part")
        val finalFile = File(downloadsDir, "${voice.id}.tar.bz2")

        // 1. Network download with throttled progress updates.
        val totalBytes = try {
            downloadBundle(voice, partFile)
        } catch (t: Throwable) {
            partFile.delete()
            return@withContext failPersisted(voice.id, "Download failed: ${t.message ?: t::class.simpleName}")
        }

        partFile.renameTo(finalFile)

        // 2. Integrity check.
        if (!voice.sha256.isNullOrBlank()) {
            val actual = sha256(finalFile)
            if (!actual.equals(voice.sha256, ignoreCase = true)) {
                finalFile.delete()
                return@withContext failPersisted(voice.id, "Checksum mismatch")
            }
        } else {
            log.w { "No sha256 for ${voice.id} — skipping integrity check" }
        }

        // 3. Extract.
        upsertState(voice.id, DownloadState.STATUS_EXTRACTING, totalBytes, totalBytes, null)
        setForegroundSafely(voice.title, pct = 100, indeterminate = true)

        val voiceDir = File(applicationContext.filesDir, "voices/${voice.id}")
        val extractResult = runCatching {
            if (voiceDir.exists()) voiceDir.deleteRecursively()
            voiceDir.mkdirs()
            extractTarBz2(finalFile, voiceDir)
        }
        if (extractResult.isFailure) {
            return@withContext failPersisted(
                voice.id,
                "Extraction failed: ${extractResult.exceptionOrNull()?.message}",
            )
        }

        // 4. Validate the extracted tree.
        val modelFile = File(voiceDir, "model.onnx")
        val tokensFile = File(voiceDir, "tokens.txt")
        if (!modelFile.isFile || !tokensFile.isFile) {
            return@withContext failPersisted(
                voice.id,
                "Bundle missing model.onnx or tokens.txt after extraction",
            )
        }

        // 5. Mark installed + done.
        voiceRepository.markInstalled(voice, voiceDir.absolutePath)
        upsertState(voice.id, DownloadState.STATUS_DONE, totalBytes, totalBytes, null)
        finalFile.delete()
        log.i { "Voice ${voice.id} installed at $voiceDir" }
        Result.success()
    }

    private suspend fun downloadBundle(voice: VoiceCard, partFile: File): Long {
        val request = Request.Builder().url(voice.bundleUrl).build()
        val response = okHttp.newCall(request).execute()
        if (!response.isSuccessful) {
            response.close()
            throw IllegalStateException("HTTP ${response.code}")
        }
        val totalBytes = response.body?.contentLength() ?: -1L
        val source = response.body?.byteStream() ?: error("Empty body")
        var downloaded = 0L
        var lastReportedPct = -1
        var lastReportTime = 0L
        FileOutputStream(partFile).use { sink ->
            source.use { src ->
                val buf = ByteArray(64 * 1024)
                while (true) {
                    if (isStopped) {
                        partFile.delete()
                        upsertState(voice.id, DownloadState.STATUS_CANCELLED, downloaded, totalBytes, null)
                        throw InterruptedException("Cancelled by WorkManager")
                    }
                    val read = src.read(buf)
                    if (read <= 0) break
                    sink.write(buf, 0, read)
                    downloaded += read
                    val now = System.currentTimeMillis()
                    val pct = if (totalBytes > 0) ((downloaded * 100) / totalBytes).toInt() else -1
                    val pctChanged = pct >= 0 && pct - lastReportedPct >= PROGRESS_PCT_STEP
                    val timeElapsed = now - lastReportTime >= PROGRESS_TIME_STEP_MS
                    if (pctChanged || timeElapsed) {
                        lastReportedPct = pct
                        lastReportTime = now
                        upsertState(
                            voice.id,
                            DownloadState.STATUS_RUNNING,
                            downloaded,
                            totalBytes.coerceAtLeast(0L),
                            null,
                        )
                        setForegroundSafely(
                            voice.title,
                            pct = pct.coerceAtLeast(0),
                            indeterminate = totalBytes <= 0,
                        )
                    }
                }
            }
        }
        response.close()
        return if (totalBytes > 0) totalBytes else downloaded
    }

    private fun extractTarBz2(archive: File, destRoot: File) {
        BufferedInputStream(FileInputStream(archive)).use { fileIn ->
            BZip2CompressorInputStream(fileIn).use { bz2In ->
                TarArchiveInputStream(bz2In).use { tarIn ->
                    var entry = tarIn.nextEntry
                    while (entry != null) {
                        if (!tarIn.canReadEntryData(entry)) {
                            entry = tarIn.nextEntry
                            continue
                        }
                        // Strip the leading "<voiceId>/" component if it
                        // exists — sherpa-onnx bundles ship that wrapper.
                        val rawName = entry.name
                        val stripped = stripLeadingDir(rawName)
                        if (stripped.isEmpty()) {
                            entry = tarIn.nextEntry
                            continue
                        }
                        val outFile = File(destRoot, stripped)
                        if (entry.isDirectory) {
                            outFile.mkdirs()
                        } else {
                            outFile.parentFile?.mkdirs()
                            FileOutputStream(outFile).use { out -> tarIn.copyTo(out) }
                        }
                        entry = tarIn.nextEntry
                    }
                }
            }
        }
    }

    private fun stripLeadingDir(path: String): String {
        val normalized = path.replace('\\', '/').removePrefix("./")
        val firstSlash = normalized.indexOf('/')
        return if (firstSlash < 0) "" else normalized.substring(firstSlash + 1)
    }

    private fun sha256(file: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { input ->
            val buf = ByteArray(64 * 1024)
            while (true) {
                val read = input.read(buf)
                if (read <= 0) break
                md.update(buf, 0, read)
            }
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }

    private suspend fun upsertState(
        voiceId: String,
        status: String,
        progressBytes: Long,
        totalBytes: Long,
        errorMessage: String?,
    ) {
        downloadStateDao.upsert(
            DownloadStateEntity(
                voiceId = voiceId,
                status = status,
                progressBytes = progressBytes,
                totalBytes = totalBytes,
                errorMessage = errorMessage,
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }

    private suspend fun setForegroundSafely(title: String, pct: Int, indeterminate: Boolean) {
        runCatching {
            val notification = DownloadNotifications.buildProgressNotification(
                applicationContext,
                title = title,
                progressPct = pct,
                indeterminate = indeterminate,
            )
            val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ForegroundInfo(
                    DownloadNotifications.NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
                )
            } else {
                ForegroundInfo(DownloadNotifications.NOTIFICATION_ID, notification)
            }
            setForeground(info)
        }.onFailure {
            // Foreground promotion can fail if the user revoked POST_NOTIFICATIONS;
            // the download still continues in the background. Log + carry on.
            log.w(it) { "setForeground rejected; continuing as background work" }
        }
    }

    private fun failed(reason: String): Result {
        log.e { "Worker pre-flight failed: $reason" }
        return Result.failure()
    }

    private suspend fun failPersisted(voiceId: String, reason: String): Result {
        log.e { "Download for $voiceId failed: $reason" }
        upsertState(voiceId, DownloadState.STATUS_FAILED, 0L, 0L, reason)
        return Result.failure()
    }

    companion object {
        const val KEY_VOICE_ID = "voiceId"
        const val KEY_TITLE = "title"
        const val KEY_VOICE_JSON = "voiceJson"

        private const val PROGRESS_PCT_STEP = 5
        private const val PROGRESS_TIME_STEP_MS = 250L

        fun uniqueName(voiceId: String): String = "download:$voiceId"
    }
}
