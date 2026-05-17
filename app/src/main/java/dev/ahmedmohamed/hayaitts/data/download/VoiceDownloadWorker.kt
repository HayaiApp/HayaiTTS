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
import dev.ahmedmohamed.hayaitts.domain.model.ModelFamily
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
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
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
        val voiceDir = File(applicationContext.filesDir, "voices/${voice.id}")
        // Whether the destination dir existed before this run — used so a retry
        // (which doWork() re-runs from scratch) does not wipe a partially
        // populated dir that the previous attempt put there.
        val voiceDirPreexisted = voiceDir.exists()

        var success = false
        try {
            // 1. Network download with throttled progress updates.
            val totalBytes = try {
                downloadBundle(voice, partFile)
            } catch (t: Throwable) {
                if (isTransient(t) && runAttemptCount < MAX_RETRIES) {
                    log.w(t) { "Transient download failure for ${voice.id}, attempt $runAttemptCount/$MAX_RETRIES — will retry" }
                    return@withContext Result.retry()
                }
                return@withContext failPersisted(
                    voice.id,
                    "Download failed: ${t.message ?: t::class.simpleName}",
                )
            }

            if (!partFile.renameTo(finalFile)) {
                // renameTo is atomic across the same FS but can fail when the
                // dest already exists from a previous attempt. Fall back to
                // delete + rename.
                finalFile.delete()
                partFile.renameTo(finalFile)
            }

            // 2. Integrity check — hard error, no retry.
            if (!voice.sha256.isNullOrBlank()) {
                val actual = sha256(finalFile)
                if (!actual.equals(voice.sha256, ignoreCase = true)) {
                    return@withContext failPersisted(voice.id, "Checksum mismatch")
                }
            } else {
                log.w { "No sha256 for ${voice.id} — skipping integrity check" }
            }

            // 3. Extract.
            upsertState(voice.id, DownloadState.STATUS_EXTRACTING, totalBytes, totalBytes, null)
            setForegroundSafely(voice.title, pct = 100, indeterminate = true)

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

            // 4. Optional secondary asset (matcha vocoder lives in a different release).
            if (!voice.vocoderUrl.isNullOrBlank() && !voice.vocoderFileName.isNullOrBlank()) {
                val sideResult = runCatching {
                    downloadAuxiliary(voice.vocoderUrl, File(voiceDir, voice.vocoderFileName))
                }
                if (sideResult.isFailure) {
                    val err = sideResult.exceptionOrNull()
                    if (isTransient(err) && runAttemptCount < MAX_RETRIES) {
                        log.w(err) { "Vocoder transient failure for ${voice.id}, retrying" }
                        return@withContext Result.retry()
                    }
                    return@withContext failPersisted(
                        voice.id,
                        "Vocoder download failed: ${err?.message}",
                    )
                }
            }

            // 5. Validate the extracted tree using family-aware required-file lists.
            val missing = missingRequiredFiles(voice, voiceDir)
            if (missing.isNotEmpty()) {
                return@withContext failPersisted(
                    voice.id,
                    "Bundle missing required files: ${missing.joinToString()}",
                )
            }

            // 6. Mark installed + done.
            voiceRepository.markInstalled(voice, voiceDir.absolutePath)
            upsertState(voice.id, DownloadState.STATUS_DONE, totalBytes, totalBytes, null)
            finalFile.delete()
            success = true
            log.i { "Voice ${voice.id} installed at $voiceDir" }
            Result.success()
        } finally {
            // Cleanup on every non-success path: scrap the partial tarball and
            // (only if we created it this run) the partially-populated voice
            // directory. Leaving 200 MB of half-extracted Kokoro shards around
            // when the user retries was the Phase 4a TODO this addresses.
            if (!success) {
                partFile.takeIf { it.exists() }?.delete()
                finalFile.takeIf { it.exists() }?.delete()
                if (!voiceDirPreexisted) voiceDir.takeIf { it.exists() }?.deleteRecursively()
            }
        }
    }

    /**
     * Classifies an error as transient (network blip, 5xx, timeout) vs hard
     * (404, malformed bundle). Transients drive WorkManager to retry per the
     * exponential backoff configured in [DownloadRepositoryImpl].
     */
    private fun isTransient(t: Throwable?): Boolean {
        if (t == null) return false
        val msg = t.message.orEmpty()
        return t is SocketTimeoutException ||
            t is UnknownHostException ||
            (t is IOException && t !is java.io.FileNotFoundException) ||
            HTTP_5XX_RE.containsMatchIn(msg)
    }

    private suspend fun downloadBundle(voice: VoiceCard, partFile: File): Long {
        val request = Request.Builder().url(voice.bundleUrl).build()
        val response = okHttp.newCall(request).execute()
        if (!response.isSuccessful) {
            val code = response.code
            response.close()
            // 5xx -> IOException -> transient -> retry. 4xx -> plain
            // IllegalStateException -> hard fail. The retry classifier reads
            // both the exception type and the message ("HTTP 503").
            if (code in 500..599) throw IOException("HTTP $code") else throw IllegalStateException("HTTP $code")
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

    /**
     * Pulls a secondary asset (currently only the Matcha-tts vocoder, which
     * lives in the `vocoder-models` release rather than `tts-models`). The
     * download is streamed straight to disk; no progress is reported because
     * it is small relative to the main bundle (~50 MB vs ~80 MB acoustic).
     */
    private fun downloadAuxiliary(url: String, target: File) {
        target.parentFile?.mkdirs()
        val request = Request.Builder().url(url).build()
        val response = okHttp.newCall(request).execute()
        response.use {
            check(it.isSuccessful) { "HTTP ${it.code}" }
            val src = it.body?.byteStream() ?: error("Empty body for $url")
            FileOutputStream(target).use { sink -> src.copyTo(sink) }
        }
        log.i { "Auxiliary asset saved: $target (${target.length()}B)" }
    }

    /**
     * Returns the list of required filenames missing from [dir] for this
     * voice's family. Bundles vary in layout — VCTK calls its weights
     * `vits-vctk.onnx` rather than `model.onnx`, Matcha uses
     * `model-steps-3.onnx`, etc — so we accept the catalog-provided
     * [VoiceCard.modelFileName] override when set and otherwise fall back to
     * a family-specific candidate list.
     */
    private fun missingRequiredFiles(voice: VoiceCard, dir: File): List<String> {
        val tokens = "tokens.txt"
        val present = mutableListOf<String>()
        val missing = mutableListOf<String>()

        fun requireOneOf(label: String, candidates: List<String>) {
            val hit = candidates.firstOrNull { File(dir, it).isFile }
            if (hit != null) present += hit else missing += label
        }

        when (voice.modelFamily) {
            ModelFamily.PIPER, ModelFamily.VITS -> {
                val modelCandidates = listOfNotNull(
                    voice.modelFileName, "model.onnx", "vits-vctk.onnx", "vits-vctk.int8.onnx",
                )
                requireOneOf(voice.modelFileName ?: "model.onnx", modelCandidates)
                if (!File(dir, tokens).isFile) missing += tokens
            }
            ModelFamily.MATCHA -> {
                val acoustic = listOfNotNull(
                    voice.modelFileName, "model-steps-3.onnx", "model-steps-6.onnx", "acoustic.onnx",
                )
                requireOneOf(voice.modelFileName ?: "model-steps-3.onnx", acoustic)
                if (!File(dir, tokens).isFile) missing += tokens
                // Vocoder is downloaded as a sidecar — fail if it didn't land.
                val vocoderName = voice.vocoderFileName ?: "vocos-22khz-univ.onnx"
                if (!File(dir, vocoderName).isFile) missing += vocoderName
            }
            ModelFamily.KOKORO -> {
                // Kokoro bundles ship `model.onnx` for the English release and
                // `kokoro-multi-lang-v1_0.onnx` for the multilingual one.
                val candidates = listOfNotNull(
                    voice.modelFileName, "model.onnx",
                    "kokoro-multi-lang-v1_0.onnx", "kokoro-en-v0_19.onnx",
                )
                requireOneOf(voice.modelFileName ?: "model.onnx", candidates)
                if (!File(dir, tokens).isFile) missing += tokens
                if (!File(dir, KOKORO_VOICES_FILE).isFile) missing += KOKORO_VOICES_FILE
            }
            ModelFamily.KITTEN -> {
                val candidates = listOfNotNull(
                    voice.modelFileName, "model.onnx", "model.int8.onnx",
                )
                requireOneOf(voice.modelFileName ?: "model.onnx", candidates)
                if (!File(dir, tokens).isFile) missing += tokens
                if (!File(dir, KOKORO_VOICES_FILE).isFile) missing += KOKORO_VOICES_FILE
            }
            ModelFamily.CUSTOM -> {
                // CUSTOM never reaches the worker (custom imports skip the
                // download path entirely) but we still guard so an
                // accidentally-enqueued bundle fails with a clear message.
                if (!File(dir, "model.onnx").isFile) missing += "model.onnx"
                if (!File(dir, tokens).isFile) missing += tokens
            }
        }
        if (present.isNotEmpty()) log.i { "Validated bundle files: $present" }
        return missing
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

        /** Shared embedding bank shipped by Kokoro and Kitten releases. */
        private const val KOKORO_VOICES_FILE = "voices.bin"

        /** Max WorkManager attempts (counting from 0). */
        private const val MAX_RETRIES = 3

        /** Used to detect 5xx HTTP errors stamped into thrown messages. */
        private val HTTP_5XX_RE = Regex("HTTP 5\\d{2}")

        fun uniqueName(voiceId: String): String = "download:$voiceId"
    }
}
