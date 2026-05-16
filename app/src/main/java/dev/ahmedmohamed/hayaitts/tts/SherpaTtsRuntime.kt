package dev.ahmedmohamed.hayaitts.tts

import android.content.Context
import android.content.res.AssetManager
import co.touchlab.kermit.Logger
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsVitsModelConfig
import dev.ahmedmohamed.hayaitts.data.voices.VoiceRepositoryImpl
import dev.ahmedmohamed.hayaitts.domain.model.InstalledVoice
import kotlinx.coroutines.runBlocking
import org.koin.core.context.GlobalContext
import java.io.File
import java.io.FileOutputStream

/**
 * Multi-voice wrapper around sherpa-onnx [OfflineTts]. The runtime holds an
 * LRU of up to [MAX_LOADED_VOICES] loaded engines keyed by voiceId — swapping
 * between two voices keeps both instances hot, swapping to a third evicts the
 * coldest one (releasing the native handle).
 *
 * Voice resolution:
 *   - [BUNDLED_VOICE_ID]  -> assets/voices/en_US-amy-low (mirrored to filesDir)
 *   - anything else       -> filesDir/voices/<voiceId>/ (the downloader writes here)
 */
class SherpaTtsRuntime private constructor(
    private val context: Context,
) {

    private val log = Logger.withTag("SherpaTtsRuntime")

    /**
     * LRU cache: the head is the most-recently-used voice. Mutated under
     * `synchronized(this)` because both the TTS framework thread and the
     * library composable can call into it.
     */
    private val loaded = LinkedHashMap<String, OfflineTts>(MAX_LOADED_VOICES, 0.75f, true)

    data class SynthesisOutput(val sampleRate: Int, val samples: FloatArray)

    fun sampleRateOf(voiceId: String): Int = engine(voiceId).sampleRate()

    fun synthesize(
        voiceId: String,
        text: String,
        sid: Int = 0,
        speed: Float = 1.0f,
    ): SynthesisOutput {
        val tts = engine(voiceId)
        val audio = tts.generate(text = text, sid = sid, speed = speed)
        return SynthesisOutput(sampleRate = audio.sampleRate, samples = audio.samples)
    }

    /**
     * Returns the bundled voice plus every Room-installed voice (via the
     * Koin-registered [VoiceRepositoryImpl]). Used by
     * [HayaiTtsService.onGetVoices]; tolerant of Koin not yet being up
     * (returns just the bundled voice in that case).
     */
    fun listAvailableVoices(): List<InstalledVoice> {
        val repo = runCatching { GlobalContext.get().get<VoiceRepositoryImpl>() }.getOrNull()
            ?: return listOf(bundledMetadata())
        return runCatching { runBlocking { repo.installedSnapshot() } }
            .getOrElse {
                log.w(it) { "installedSnapshot failed; falling back to bundled-only" }
                listOf(bundledMetadata())
            }
    }

    /** Returns the cached engine for [voiceId], lazily building it on first use. */
    private fun engine(voiceId: String): OfflineTts = synchronized(this) {
        loaded[voiceId]?.let { return@synchronized it }
        val tts = buildEngine(voiceId)
        loaded[voiceId] = tts
        evictIfNeeded()
        tts
    }

    private fun evictIfNeeded() {
        while (loaded.size > MAX_LOADED_VOICES) {
            // LinkedHashMap in access-order: the first key is the oldest by
            // last access. Pull it out and release the native handle.
            val oldestKey = loaded.keys.iterator().next()
            val evicted = loaded.remove(oldestKey)
            evicted?.runCatching { release() }
            log.i { "Evicted voice $oldestKey from runtime cache" }
        }
    }

    private fun buildEngine(voiceId: String): OfflineTts {
        val voiceDir: File = if (voiceId == BUNDLED_VOICE_ID) {
            val dest = File(context.filesDir, VOICE_ASSET_SUBDIR)
            mirrorBundledAssetTreeIfNeeded(dest)
            dest
        } else {
            File(context.filesDir, "voices/$voiceId").also { dir ->
                require(dir.isDirectory) { "Voice $voiceId not installed at $dir" }
            }
        }

        val modelPath = File(voiceDir, MODEL_FILE).absolutePath
        val tokensPath = File(voiceDir, TOKENS_FILE).absolutePath
        val dataDir = File(voiceDir, ESPEAK_DIR)
        val dataDirPath = if (dataDir.isDirectory) dataDir.absolutePath else ""

        log.i { "Loading voice $voiceId from $voiceDir" }
        val config = OfflineTtsConfig(
            model = OfflineTtsModelConfig(
                vits = OfflineTtsVitsModelConfig(
                    model = modelPath,
                    tokens = tokensPath,
                    dataDir = dataDirPath,
                    lengthScale = 1.0f,
                ),
                numThreads = 1,
                debug = false,
            ),
            maxNumSentences = 2,
        )
        val tts = OfflineTts(assetManager = null, config = config)
        log.i { "OfflineTts ready for $voiceId (sampleRate=${tts.sampleRate()})" }
        return tts
    }

    private fun bundledMetadata(): InstalledVoice = InstalledVoice(
        voiceId = BUNDLED_VOICE_ID,
        family = dev.ahmedmohamed.hayaitts.domain.model.ModelFamily.PIPER,
        title = "Amy",
        languages = listOf("en-US"),
        speakers = listOf(
            dev.ahmedmohamed.hayaitts.domain.model.Speaker(
                id = 0, name = "amy", gender = "F",
            ),
        ),
        sampleRateHz = BUNDLED_VOICE_SAMPLE_RATE,
        installedPath = File(context.filesDir, VOICE_ASSET_SUBDIR).absolutePath,
        tier = dev.ahmedmohamed.hayaitts.domain.model.Tier.LOW,
        installedAt = 0L,
        bundled = true,
    )

    // -- Asset mirroring (bundled voice only). -------------------------------

    private fun mirrorBundledAssetTreeIfNeeded(destRoot: File) {
        val manifestFile = File(destRoot, MANIFEST_FILE)
        if (destRoot.isDirectory && manifestFile.isFile) {
            val cached = manifestFile.readText().trim()
            val actual = manifestOnDisk(destRoot, manifestFile)
            if (cached == actual) {
                log.i { "Bundled voice mirror up to date at $destRoot" }
                return
            }
            log.i { "Bundled voice mirror stale (manifest=$cached, disk=$actual)" }
        }
        if (destRoot.exists()) destRoot.deleteRecursively()
        destRoot.mkdirs()
        val am = context.assets
        val files = collectAssetFiles(am, VOICE_ASSET_SUBDIR)
        var totalBytes = 0L
        files.forEach { rel ->
            val relative = rel.removePrefix("$VOICE_ASSET_SUBDIR/")
            val outFile = File(destRoot, relative)
            outFile.parentFile?.mkdirs()
            am.open(rel).use { input ->
                FileOutputStream(outFile).use { output ->
                    totalBytes += input.copyTo(output)
                }
            }
        }
        manifestFile.writeText("${files.size}\n$totalBytes\n")
        log.i { "Mirrored ${files.size} bundled assets (${totalBytes}B) to $destRoot" }
    }

    private fun collectAssetFiles(am: AssetManager, path: String): List<String> {
        val entries = am.list(path).orEmpty()
        if (entries.isEmpty()) {
            return try {
                am.open(path).close()
                listOf(path)
            } catch (_: Throwable) {
                emptyList()
            }
        }
        return entries.flatMap { child -> collectAssetFiles(am, "$path/$child") }
    }

    private fun manifestOnDisk(destRoot: File, manifestFile: File): String {
        var count = 0
        var bytes = 0L
        destRoot.walkTopDown().filter { it.isFile && it != manifestFile }
            .forEach { count++; bytes += it.length() }
        return "$count\n$bytes"
    }

    companion object {
        /** Sentinel id for the bundled-in-APK Piper Amy voice. */
        const val BUNDLED_VOICE_ID = "vits-piper-en_US-amy-low"
        /** Piper low-quality models emit at 16 kHz. */
        const val BUNDLED_VOICE_SAMPLE_RATE = 16000
        /** Subdirectory inside `assets/` where the bundled voice lives. */
        const val VOICE_ASSET_SUBDIR = "voices/en_US-amy-low"

        private const val MODEL_FILE = "model.onnx"
        private const val TOKENS_FILE = "tokens.txt"
        private const val ESPEAK_DIR = "espeak-ng-data"
        private const val MANIFEST_FILE = ".mirror-manifest"
        private const val MAX_LOADED_VOICES = 2

        @Volatile
        private var instance: SherpaTtsRuntime? = null

        fun get(context: Context): SherpaTtsRuntime {
            instance?.let { return it }
            return synchronized(this) {
                instance ?: SherpaTtsRuntime(context.applicationContext).also { instance = it }
            }
        }
    }
}
