package dev.ahmedmohamed.hayaitts.data.custom

import android.content.Context
import android.net.Uri
import co.touchlab.kermit.Logger
import dev.ahmedmohamed.hayaitts.domain.model.ModelFamily
import dev.ahmedmohamed.hayaitts.domain.model.Speaker
import dev.ahmedmohamed.hayaitts.domain.model.VoiceCard
import dev.ahmedmohamed.hayaitts.domain.repo.VoiceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID
import java.util.zip.ZipInputStream

/**
 * Pipes a user-picked TTS bundle from a SAF [Uri] into
 * `filesDir/voices/custom-<uuid>/` and registers it with [VoiceRepository].
 *
 * Three steps, each pushed onto a [MutableStateFlow] so the wizard can render a
 * determinate progress bar:
 *   1. Copy the URI's stream into `cacheDir/imports/<uuid>.<ext>`.
 *   2. Extract that archive into the voice dir (or treat a bare `.onnx` as the
 *      model file itself).
 *   3. Validate the unpacked tree against the chosen [ModelFamily]'s required
 *      file list. On failure, the partially-extracted dir is wiped so the user
 *      can pick a different family without ghost rows.
 */
class CustomBundleInstaller(
    private val context: Context,
    private val voiceRepository: VoiceRepository,
) {

    private val log = Logger.withTag("CustomBundleInstaller")

    sealed class Step {
        data object Copying : Step()
        data object Extracting : Step()
        data object Validating : Step()
    }

    data class Progress(val step: Step, val pct: Float)

    data class Request(
        val sourceUri: Uri,
        val archive: CustomBundleAnalyzer.ArchiveKind,
        val effectiveFamily: ModelFamily,
        val voiceName: String,
        val languages: List<String>,
        val speakers: List<Speaker>,
    )

    data class Installed(val voiceId: String, val dir: File, val card: VoiceCard)

    suspend fun install(
        request: Request,
        progress: MutableStateFlow<Progress>,
    ): Result<Installed> = runCatching {
        val uuid = UUID.randomUUID().toString().take(12)
        val voiceId = "custom-$uuid"
        val voiceDir = File(context.filesDir, "voices/$voiceId")
        if (voiceDir.exists()) voiceDir.deleteRecursively()
        voiceDir.mkdirs()

        val importsCache = File(context.cacheDir, "imports").apply { mkdirs() }
        val cacheFile = File(importsCache, "$uuid${extensionFor(request.archive)}")

        try {
            // 1. Stream the picked URI into cacheDir so we can extract using
            //    the same Commons Compress codepath as the downloader.
            progress.value = Progress(Step.Copying, 0.05f)
            copyUriToFile(request.sourceUri, cacheFile, progress)

            // 2. Extract (or, for bare .onnx, rename the cached file into the
            //    voice dir as model.onnx).
            progress.value = Progress(Step.Extracting, 0.4f)
            when (request.archive) {
                CustomBundleAnalyzer.ArchiveKind.TAR_BZ2 -> extractTarBz2(cacheFile, voiceDir)
                CustomBundleAnalyzer.ArchiveKind.TAR -> extractTar(cacheFile, voiceDir)
                CustomBundleAnalyzer.ArchiveKind.ZIP -> extractZip(cacheFile, voiceDir)
                CustomBundleAnalyzer.ArchiveKind.BARE_ONNX -> {
                    val dest = File(voiceDir, "model.onnx")
                    cacheFile.copyTo(dest, overwrite = true)
                }
                CustomBundleAnalyzer.ArchiveKind.UNKNOWN ->
                    error("Cannot extract: unknown archive type")
            }

            // 3. Validate using the same required-files matrix as the
            //    downloader, surfaced through the chosen effective family.
            progress.value = Progress(Step.Validating, 0.9f)
            val missing = missingRequiredFiles(request.effectiveFamily, voiceDir)
            if (missing.isNotEmpty()) {
                voiceDir.deleteRecursively()
                error("missing:${missing.joinToString(",")}")
            }

            val sampleRate = sampleRateFor(request.effectiveFamily)
            val card = VoiceCard(
                id = voiceId,
                family = "custom",
                title = request.voiceName.ifBlank { voiceId },
                languages = request.languages,
                speakers = request.speakers,
                sampleRateHz = sampleRate,
                approxSizeMb = (voiceDir.walkTopDown().filter { it.isFile }.sumOf { it.length() } / (1024 * 1024)).toInt(),
                tier = "mid",
                license = "User-supplied",
                bundleUrl = request.sourceUri.toString(),
            )
            voiceRepository.markInstalledCustom(card, voiceDir.absolutePath, request.effectiveFamily)
            progress.value = Progress(Step.Validating, 1f)
            log.i { "Imported $voiceId at $voiceDir (effective=${request.effectiveFamily})" }
            Installed(voiceId, voiceDir, card)
        } finally {
            // Always wipe the cache copy — even on success — so we do not
            // leak hundreds of MB after every import.
            cacheFile.delete()
        }
    }.onFailure {
        log.w(it) { "Custom import failed for ${request.sourceUri}" }
    }

    private fun copyUriToFile(uri: Uri, dest: File, progress: MutableStateFlow<Progress>) {
        dest.parentFile?.mkdirs()
        val resolver = context.contentResolver
        // Approx total for progress: best-effort from OpenableColumns.SIZE.
        val total: Long = runCatching {
            resolver.query(uri, null, null, null, null)?.use { c ->
                val idx = c.getColumnIndex(android.provider.OpenableColumns.SIZE)
                if (c.moveToFirst() && idx >= 0) c.getLong(idx) else -1L
            } ?: -1L
        }.getOrDefault(-1L)

        val input = resolver.openInputStream(uri) ?: error("Cannot open $uri")
        var copied = 0L
        var lastReported = 0f
        BufferedInputStream(input).use { src ->
            FileOutputStream(dest).use { sink ->
                val buf = ByteArray(64 * 1024)
                while (true) {
                    val read = src.read(buf)
                    if (read <= 0) break
                    sink.write(buf, 0, read)
                    copied += read
                    val frac = if (total > 0) (copied.toFloat() / total).coerceIn(0f, 0.4f) else 0.2f
                    if (frac - lastReported >= 0.02f) {
                        lastReported = frac
                        progress.value = Progress(Step.Copying, 0.05f + frac)
                    }
                }
            }
        }
    }

    private fun extractTarBz2(archive: File, destRoot: File) {
        BufferedInputStream(archive.inputStream()).use { fileIn ->
            BZip2CompressorInputStream(fileIn).use { bz2 ->
                TarArchiveInputStream(bz2).use { tar -> writeTar(tar, destRoot) }
            }
        }
    }

    private fun extractTar(archive: File, destRoot: File) {
        BufferedInputStream(archive.inputStream()).use { fileIn ->
            TarArchiveInputStream(fileIn).use { tar -> writeTar(tar, destRoot) }
        }
    }

    private fun writeTar(tar: TarArchiveInputStream, destRoot: File) {
        var entry = tar.nextEntry
        while (entry != null) {
            if (!tar.canReadEntryData(entry)) {
                entry = tar.nextEntry
                continue
            }
            val stripped = stripLeadingDir(entry.name)
            if (stripped.isEmpty()) {
                entry = tar.nextEntry
                continue
            }
            val outFile = File(destRoot, stripped)
            if (entry.isDirectory) {
                outFile.mkdirs()
            } else {
                outFile.parentFile?.mkdirs()
                FileOutputStream(outFile).use { out -> tar.copyTo(out) }
            }
            entry = tar.nextEntry
        }
    }

    private fun extractZip(archive: File, destRoot: File) {
        BufferedInputStream(archive.inputStream()).use { fileIn ->
            ZipInputStream(fileIn).use { zip ->
                writeZip(zip, destRoot)
            }
        }
    }

    private fun writeZip(zip: ZipInputStream, destRoot: File) {
        var entry = zip.nextEntry
        while (entry != null) {
            val stripped = stripLeadingDir(entry.name)
            if (stripped.isEmpty()) {
                entry = zip.nextEntry
                continue
            }
            val outFile = File(destRoot, stripped)
            if (entry.isDirectory) {
                outFile.mkdirs()
            } else {
                outFile.parentFile?.mkdirs()
                FileOutputStream(outFile).use { out -> zip.copyTo(out) }
            }
            zip.closeEntry()
            entry = zip.nextEntry
        }
    }

    private fun stripLeadingDir(path: String): String {
        val normalized = path.replace('\\', '/').removePrefix("./")
        // ZIP/TAR bundles from k2-fsa wrap everything under `<voiceId>/`. We
        // peel that off so the on-disk layout is the same as a downloaded
        // bundle. Unwrapped archives stay as-is.
        val firstSlash = normalized.indexOf('/')
        if (firstSlash < 0) return normalized
        val head = normalized.substring(0, firstSlash)
        val tail = normalized.substring(firstSlash + 1)
        // If the head is a generic wrapper dir, strip it. If the archive looks
        // unwrapped (head is a real file like `tokens.txt`), keep both parts.
        return if (head.contains('.')) normalized else tail
    }

    /**
     * Mirrors the same family->files matrix as [VoiceDownloadWorker]. Returns
     * the list of missing required filenames; an empty list means the bundle
     * is valid for [family].
     */
    private fun missingRequiredFiles(family: ModelFamily, dir: File): List<String> {
        val tokens = "tokens.txt"
        val missing = mutableListOf<String>()
        fun requireOneOf(label: String, candidates: List<String>) {
            if (candidates.none { File(dir, it).isFile }) missing += label
        }
        when (family) {
            ModelFamily.PIPER, ModelFamily.VITS -> {
                requireOneOf("model.onnx", listOf("model.onnx", "vits-vctk.onnx", "vits-vctk.int8.onnx"))
                if (!File(dir, tokens).isFile) missing += tokens
            }
            ModelFamily.MATCHA -> {
                requireOneOf(
                    "model-steps-3.onnx",
                    listOf("model-steps-3.onnx", "model-steps-6.onnx", "acoustic.onnx"),
                )
                if (!File(dir, tokens).isFile) missing += tokens
                // Vocoder lives alongside the acoustic model. Accept any
                // `*vocoder*.onnx` / `vocos*.onnx` / `hifigan*.onnx`.
                val hasVocoder = dir.listFiles()?.any {
                    val n = it.name.lowercase()
                    it.isFile && n.endsWith(".onnx") &&
                        (n.contains("vocos") || n.contains("vocoder") || n.contains("hifigan"))
                } ?: false
                if (!hasVocoder) missing += "vocoder.onnx"
            }
            ModelFamily.KOKORO, ModelFamily.KITTEN -> {
                requireOneOf("model.onnx", listOf("model.onnx", "model.fp16.onnx", "model.int8.onnx"))
                if (!File(dir, tokens).isFile) missing += tokens
                if (!File(dir, "voices.bin").isFile) missing += "voices.bin"
            }
            ModelFamily.ZIPVOICE -> {
                requireOneOf("encoder.onnx", listOf("encoder.int8.onnx", "encoder.onnx"))
                requireOneOf("decoder.onnx", listOf("decoder.int8.onnx", "decoder.onnx"))
                requireOneOf("vocoder.onnx", listOf("vocos_24khz.onnx", "vocos_22khz.onnx", "vocoder.onnx"))
                if (!File(dir, tokens).isFile) missing += tokens
            }
            ModelFamily.POCKET -> {
                listOf(
                    "lm_flow.int8.onnx", "lm_main.int8.onnx", "encoder.onnx",
                    "decoder.int8.onnx", "text_conditioner.onnx",
                    "vocab.json", "token_scores.json",
                ).forEach { if (!File(dir, it).isFile) missing += it }
            }
            ModelFamily.SUPERTONIC -> {
                listOf(
                    "duration_predictor.int8.onnx", "text_encoder.int8.onnx",
                    "vector_estimator.int8.onnx", "vocoder.int8.onnx",
                    "tts.json", "unicode_indexer.bin", "voice.bin",
                ).forEach { if (!File(dir, it).isFile) missing += it }
            }
            ModelFamily.CUSTOM -> {
                // Unreachable — the wizard resolves CUSTOM to a concrete
                // effectiveFamily before calling this, but the exhaustive
                // when needs a branch.
                missing += "custom-must-be-resolved-to-effective-family"
            }
        }
        return missing
    }

    private fun sampleRateFor(family: ModelFamily): Int = when (family) {
        // Piper voices commonly emit 16 kHz (low), 22.05 kHz (medium), or
        // 22.05 kHz (high). We pick 22050 as the safe middle: sherpa-onnx
        // reports the real rate post-load, this just seeds the metadata for
        // pre-load callers.
        ModelFamily.PIPER -> 22050
        ModelFamily.VITS -> 22050
        ModelFamily.MATCHA -> 22050
        else -> 22050
    }

    private fun extensionFor(kind: CustomBundleAnalyzer.ArchiveKind): String = when (kind) {
        CustomBundleAnalyzer.ArchiveKind.TAR_BZ2 -> ".tar.bz2"
        CustomBundleAnalyzer.ArchiveKind.TAR -> ".tar"
        CustomBundleAnalyzer.ArchiveKind.ZIP -> ".zip"
        CustomBundleAnalyzer.ArchiveKind.BARE_ONNX -> ".onnx"
        CustomBundleAnalyzer.ArchiveKind.UNKNOWN -> ".bin"
    }
}

// Helper extension so `archive.inputStream()` reads the file with no manual
// FileInputStream boilerplate above.
private fun File.inputStream(): InputStream = java.io.FileInputStream(this)
