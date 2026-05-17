package dev.ahmedmohamed.hayaitts.tts

import android.content.Context
import android.content.res.AssetManager
import co.touchlab.kermit.Logger
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsKittenModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsKokoroModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsMatchaModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsVitsModelConfig
import dev.ahmedmohamed.hayaitts.data.voices.VoiceRepositoryImpl
import dev.ahmedmohamed.hayaitts.domain.model.InstalledVoice
import dev.ahmedmohamed.hayaitts.domain.model.ModelFamily
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
        val voice = installedVoiceOf(voiceId)
        val family = if (voice?.family == ModelFamily.CUSTOM) {
            voice.effectiveFamily ?: error(
                "Custom voice $voiceId is missing effectiveFamily — re-import the bundle.",
            )
        } else {
            voice?.family ?: ModelFamily.PIPER
        }
        log.i { "Loading voice $voiceId (family=$family) from $voiceDir" }
        val config = buildConfig(family, voiceDir)
        val tts = OfflineTts(assetManager = null, config = config)
        log.i { "OfflineTts ready for $voiceId (sampleRate=${tts.sampleRate()})" }
        return tts
    }

    /**
     * Picks the right [OfflineTtsConfig] shape for a family. Piper and the
     * other non-Piper VITS bundles both go through the VITS config because
     * Piper voices ARE VITS variants under the hood. Phase 7 added Kokoro
     * and Kitten — the upstream v1.13.2 AAR ships JNI for both families.
     */
    private fun buildConfig(family: ModelFamily, dir: File): OfflineTtsConfig = when (family) {
        ModelFamily.PIPER, ModelFamily.VITS -> buildVitsConfig(dir)
        ModelFamily.MATCHA -> buildMatchaConfig(dir)
        ModelFamily.KOKORO -> buildKokoroConfig(dir)
        ModelFamily.KITTEN -> buildKittenConfig(dir)
        ModelFamily.CUSTOM -> throw IllegalStateException(
            "Custom voices should have been resolved to an effective family before reaching buildConfig.",
        )
    }

    private fun buildVitsConfig(dir: File): OfflineTtsConfig {
        val modelPath = resolveModelFile(dir, VITS_MODEL_CANDIDATES)
        val tokensPath = File(dir, TOKENS_FILE).absolutePath
        val dataDir = File(dir, ESPEAK_DIR)
        val dataDirPath = if (dataDir.isDirectory) dataDir.absolutePath else ""
        val lexicon = File(dir, LEXICON_FILE)
        val lexiconPath = if (lexicon.isFile) lexicon.absolutePath else ""
        val dictDir = File(dir, DICT_DIR)
        val dictDirPath = if (dictDir.isDirectory) dictDir.absolutePath else ""
        return OfflineTtsConfig(
            model = OfflineTtsModelConfig(
                vits = OfflineTtsVitsModelConfig(
                    model = modelPath,
                    lexicon = lexiconPath,
                    tokens = tokensPath,
                    dataDir = dataDirPath,
                    dictDir = dictDirPath,
                    lengthScale = 1.0f,
                ),
                numThreads = 1,
                debug = false,
            ),
            ruleFsts = collectRuleFsts(dir),
            maxNumSentences = 2,
        )
    }

    private fun buildMatchaConfig(dir: File): OfflineTtsConfig {
        val acoustic = resolveModelFile(dir, MATCHA_ACOUSTIC_CANDIDATES)
        val vocoder = File(dir, VOCODER_FILE)
        check(vocoder.isFile) { "Matcha voice at $dir is missing $VOCODER_FILE" }
        val tokensPath = File(dir, TOKENS_FILE).absolutePath
        val dataDir = File(dir, ESPEAK_DIR)
        val dataDirPath = if (dataDir.isDirectory) dataDir.absolutePath else ""
        val lexicon = File(dir, LEXICON_FILE)
        val lexiconPath = if (lexicon.isFile) lexicon.absolutePath else ""
        val dictDir = File(dir, DICT_DIR)
        val dictDirPath = if (dictDir.isDirectory) dictDir.absolutePath else ""
        return OfflineTtsConfig(
            model = OfflineTtsModelConfig(
                matcha = OfflineTtsMatchaModelConfig(
                    acousticModel = acoustic,
                    vocoder = vocoder.absolutePath,
                    lexicon = lexiconPath,
                    tokens = tokensPath,
                    dataDir = dataDirPath,
                    dictDir = dictDirPath,
                    lengthScale = 1.0f,
                ),
                numThreads = 1,
                debug = false,
            ),
            ruleFsts = collectRuleFsts(dir),
            maxNumSentences = 2,
        )
    }

    /**
     * Kokoro layout: a `model.onnx` weight file, the `voices.bin` voice-embedding
     * bank that lists the speaker embeddings, espeak-ng phoneme data, and an
     * optional lexicon used for non-English languages. Kokoro auto-detects the
     * language tag from the bundle's `lang` directory; we leave `lang` empty
     * and let the native side fall back to multilingual mode.
     */
    private fun buildKokoroConfig(dir: File): OfflineTtsConfig {
        val modelPath = resolveModelFile(dir, KOKORO_MODEL_CANDIDATES)
        val voices = File(dir, KOKORO_VOICES_FILE)
        check(voices.isFile) { "Kokoro voice at $dir is missing $KOKORO_VOICES_FILE" }
        val tokensPath = File(dir, TOKENS_FILE).absolutePath
        val dataDir = File(dir, ESPEAK_DIR)
        val dataDirPath = if (dataDir.isDirectory) dataDir.absolutePath else ""
        val lexicon = File(dir, LEXICON_FILE)
        val lexiconPath = if (lexicon.isFile) lexicon.absolutePath else ""
        val dictDir = File(dir, DICT_DIR)
        val dictDirPath = if (dictDir.isDirectory) dictDir.absolutePath else ""
        return OfflineTtsConfig(
            model = OfflineTtsModelConfig(
                kokoro = OfflineTtsKokoroModelConfig(
                    model = modelPath,
                    voices = voices.absolutePath,
                    tokens = tokensPath,
                    dataDir = dataDirPath,
                    lexicon = lexiconPath,
                    dictDir = dictDirPath,
                    lengthScale = 1.0f,
                ),
                numThreads = 2,
                debug = false,
            ),
            ruleFsts = collectRuleFsts(dir),
            maxNumSentences = 2,
        )
    }

    /**
     * Kitten layout matches Kokoro but is smaller: a single ONNX checkpoint
     * plus voices.bin and tokens.txt. No lexicon / dictDir on the current
     * `kitten-nano-en` release.
     */
    private fun buildKittenConfig(dir: File): OfflineTtsConfig {
        val modelPath = resolveModelFile(dir, KITTEN_MODEL_CANDIDATES)
        val voices = File(dir, KOKORO_VOICES_FILE)
        check(voices.isFile) { "Kitten voice at $dir is missing $KOKORO_VOICES_FILE" }
        val tokensPath = File(dir, TOKENS_FILE).absolutePath
        val dataDir = File(dir, ESPEAK_DIR)
        val dataDirPath = if (dataDir.isDirectory) dataDir.absolutePath else ""
        return OfflineTtsConfig(
            model = OfflineTtsModelConfig(
                kitten = OfflineTtsKittenModelConfig(
                    model = modelPath,
                    voices = voices.absolutePath,
                    tokens = tokensPath,
                    dataDir = dataDirPath,
                    lengthScale = 1.0f,
                ),
                numThreads = 2,
                debug = false,
            ),
            maxNumSentences = 2,
        )
    }

    private fun resolveModelFile(dir: File, candidates: List<String>): String {
        candidates.forEach { name ->
            val candidate = File(dir, name)
            if (candidate.isFile) return candidate.absolutePath
        }
        // Last-resort: pick any .onnx file in the dir so we surface a clearer
        // error from the native side ("failed to load <path>") instead of a
        // mystery NPE.
        val firstOnnx = dir.listFiles()?.firstOrNull { it.isFile && it.extension == "onnx" }
        if (firstOnnx != null) {
            log.w { "Unknown bundle layout at $dir, falling back to ${firstOnnx.name}" }
            return firstOnnx.absolutePath
        }
        error("No .onnx weight file found in $dir (tried $candidates)")
    }

    /** Concatenated comma-separated list of `*.fst` rule files for Chinese voices. */
    private fun collectRuleFsts(dir: File): String {
        val present = RULE_FST_FILES.mapNotNull { name ->
            val f = File(dir, name)
            if (f.isFile) f.absolutePath else null
        }
        return present.joinToString(",")
    }

    /**
     * Returns the [InstalledVoice] for [voiceId] from the Room mirror. Null
     * when the voice is the bundled one or when Koin is not yet up — both
     * cases are handled by the caller defaulting to Piper.
     */
    private fun installedVoiceOf(voiceId: String): InstalledVoice? {
        if (voiceId == BUNDLED_VOICE_ID) return null
        val repo = runCatching { GlobalContext.get().get<VoiceRepositoryImpl>() }.getOrNull()
            ?: return null
        val snapshot = runCatching { runBlocking { repo.installedSnapshot() } }.getOrNull()
            ?: return null
        return snapshot.firstOrNull { it.voiceId == voiceId }
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

        private const val TOKENS_FILE = "tokens.txt"
        private const val LEXICON_FILE = "lexicon.txt"
        private const val VOCODER_FILE = "vocos-22khz-univ.onnx"
        private const val ESPEAK_DIR = "espeak-ng-data"
        private const val DICT_DIR = "dict"
        private const val MANIFEST_FILE = ".mirror-manifest"
        private const val MAX_LOADED_VOICES = 2

        /**
         * Filenames where a VITS / Piper bundle commonly stores its weights.
         * Most bundles use `model.onnx`; VCTK ships `vits-vctk.onnx` and the
         * int8 variant ships `vits-vctk.int8.onnx`. We probe in order and
         * fall back to "first .onnx in the dir" if none match.
         */
        private val VITS_MODEL_CANDIDATES = listOf(
            "model.onnx",
            "vits-vctk.onnx",
            "vits-vctk.int8.onnx",
        )

        /**
         * Matcha bundles ship `model-steps-3.onnx` (the 3-step diffusion
         * checkpoint). We probe for two alternatives that some upstream
         * bundles use before failing through to a generic .onnx scan.
         */
        private val MATCHA_ACOUSTIC_CANDIDATES = listOf(
            "model-steps-3.onnx",
            "model-steps-6.onnx",
            "acoustic.onnx",
        )

        /**
         * Kokoro voices ship `model.onnx`. The multilingual v1.0 release uses
         * `kokoro-multi-lang-v1_0.onnx` instead; probe both before falling
         * back to a generic .onnx scan in [resolveModelFile].
         */
        private val KOKORO_MODEL_CANDIDATES = listOf(
            "model.onnx",
            "kokoro-multi-lang-v1_0.onnx",
            "kokoro-en-v0_19.onnx",
        )

        /**
         * Kitten nano ships `model.onnx` only; quantized variants are
         * `model.int8.onnx`. The voices.bin embedding bank lives next to it.
         */
        private val KITTEN_MODEL_CANDIDATES = listOf(
            "model.onnx",
            "model.int8.onnx",
        )

        /** Speaker-embedding bank shared by Kokoro and Kitten releases. */
        private const val KOKORO_VOICES_FILE = "voices.bin"

        /**
         * Comma-joined into [OfflineTtsConfig.ruleFsts] when present. These
         * are sherpa-onnx text-normalisation FSTs shipped with Chinese
         * voices for digit / date / number expansion.
         */
        private val RULE_FST_FILES = listOf("date.fst", "number.fst", "phone.fst")

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
