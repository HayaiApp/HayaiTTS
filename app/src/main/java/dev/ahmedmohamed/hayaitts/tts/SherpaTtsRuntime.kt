package dev.ahmedmohamed.hayaitts.tts

import android.content.Context
import co.touchlab.kermit.Logger
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsKittenModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsKokoroModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsMatchaModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsPocketModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsSupertonicModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsVitsModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsZipVoiceModelConfig
import dev.ahmedmohamed.hayaitts.data.voices.VoiceRepositoryImpl
import dev.ahmedmohamed.hayaitts.domain.model.InstalledVoice
import dev.ahmedmohamed.hayaitts.domain.model.ModelFamily
import kotlinx.coroutines.runBlocking
import org.koin.core.context.GlobalContext
import java.io.File

/**
 * Multi-voice wrapper around sherpa-onnx [OfflineTts]. The runtime holds an
 * LRU of up to [MAX_LOADED_VOICES] loaded engines keyed by voiceId — swapping
 * between two voices keeps both instances hot, swapping to a third evicts the
 * coldest one (releasing the native handle).
 *
 * Every voice is resolved from the `installedPath` Room stores when the
 * downloader (or the custom-import installer) finishes writing the bundle.
 * Nothing is bundled in the APK — first-run, `listAvailableVoices()`
 * returns an empty list and Library shows its empty state.
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
     * Every Room-installed voice (via the Koin-registered [VoiceRepositoryImpl]).
     * Used by [HayaiTtsService.onGetVoices]. Empty until the user installs
     * their first voice from Browse.
     */
    fun listAvailableVoices(): List<InstalledVoice> {
        val repo = runCatching { GlobalContext.get().get<VoiceRepositoryImpl>() }.getOrNull()
            ?: return emptyList()
        return runCatching { runBlocking { repo.installedSnapshot() } }
            .getOrElse {
                log.w(it) { "installedSnapshot failed; returning empty voice list" }
                emptyList()
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
        val voice = installedVoiceOf(voiceId)
        // The downloader / custom-import installer writes voices wherever
        // `SettingsRepository.storageLocation` pointed at the time of
        // install, and `StorageMigrator.moveAllVoices` can rewrite
        // `installedPath` later. Trust the Room row over any hardcoded
        // filesDir layout — that's how external SD-card installs and
        // post-move dirs are discoverable.
        val path = voice?.installedPath?.takeIf { it.isNotBlank() }
            ?: error("Voice $voiceId has no installedPath; reinstall the bundle.")
        val voiceDir = File(path).also { dir ->
            require(dir.isDirectory) { "Voice $voiceId not installed at $dir" }
        }
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
     * Picks the right [OfflineTtsConfig] shape for a family. The upstream
     * sherpa-onnx 1.13.2 AAR exposes JNI for seven families; we wire all of
     * them so every voice the catalog generator scrapes is actually playable.
     */
    private fun buildConfig(family: ModelFamily, dir: File): OfflineTtsConfig = when (family) {
        ModelFamily.PIPER, ModelFamily.VITS -> buildVitsConfig(dir)
        ModelFamily.MATCHA -> buildMatchaConfig(dir)
        ModelFamily.KOKORO -> buildKokoroConfig(dir)
        ModelFamily.KITTEN -> buildKittenConfig(dir)
        ModelFamily.ZIPVOICE -> buildZipVoiceConfig(dir)
        ModelFamily.POCKET -> buildPocketConfig(dir)
        ModelFamily.SUPERTONIC -> buildSupertonicConfig(dir)
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

    /**
     * ZipVoice (k2-fsa, 2025) — flow-matching multilingual TTS, distilled to
     * encoder + decoder + 24 kHz vocoder. Upstream bundles ship a 24 kHz
     * vocoder named `vocos_24khz.onnx` alongside the model files. Per the
     * upstream Java example, `setVocoder` is passed an explicit path (not
     * derived from the bundle dir) — but sherpa-onnx-published bundles
     * include the vocoder inside the same archive, so we resolve relative to
     * `dir`.
     */
    private fun buildZipVoiceConfig(dir: File): OfflineTtsConfig {
        val encoder = resolveModelFile(dir, ZIPVOICE_ENCODER_CANDIDATES)
        val decoder = resolveModelFile(dir, ZIPVOICE_DECODER_CANDIDATES)
        val vocoder = resolveModelFile(dir, ZIPVOICE_VOCODER_CANDIDATES)
        val tokensPath = File(dir, TOKENS_FILE).absolutePath
        val dataDir = File(dir, ESPEAK_DIR)
        val dataDirPath = if (dataDir.isDirectory) dataDir.absolutePath else ""
        val lexicon = File(dir, LEXICON_FILE)
        val lexiconPath = if (lexicon.isFile) lexicon.absolutePath else ""
        return OfflineTtsConfig(
            model = OfflineTtsModelConfig(
                zipvoice = OfflineTtsZipVoiceModelConfig(
                    tokens = tokensPath,
                    encoder = encoder,
                    decoder = decoder,
                    vocoder = vocoder,
                    dataDir = dataDirPath,
                    lexicon = lexiconPath,
                ),
                numThreads = 2,
                debug = false,
            ),
            maxNumSentences = 2,
        )
    }

    /**
     * Pocket (k2-fsa, 2026) — small autoregressive TTS that splits the
     * acoustic model across an LM flow + LM main + encoder + decoder +
     * text-conditioner stack. Tokens come from `vocab.json` instead of
     * `tokens.txt`. No espeak-ng dependency.
     */
    private fun buildPocketConfig(dir: File): OfflineTtsConfig {
        fun req(name: String): String {
            val f = File(dir, name)
            check(f.isFile) { "Pocket voice at $dir is missing $name" }
            return f.absolutePath
        }
        return OfflineTtsConfig(
            model = OfflineTtsModelConfig(
                pocket = OfflineTtsPocketModelConfig(
                    lmFlow = req("lm_flow.int8.onnx"),
                    lmMain = req("lm_main.int8.onnx"),
                    encoder = req("encoder.onnx"),
                    decoder = req("decoder.int8.onnx"),
                    textConditioner = req("text_conditioner.onnx"),
                    vocabJson = req("vocab.json"),
                    tokenScoresJson = req("token_scores.json"),
                ),
                numThreads = 2,
                debug = false,
            ),
            maxNumSentences = 2,
        )
    }

    /**
     * Supertonic (k2-fsa, 2026) — diffusion-flow TTS with separate duration
     * predictor + text encoder + vector estimator + vocoder, configured via
     * `tts.json` and Unicode-aware via `unicode_indexer.bin`. Voice timbre is
     * carried by `voice.bin` (analogous to Kokoro's `voices.bin`).
     */
    private fun buildSupertonicConfig(dir: File): OfflineTtsConfig {
        fun req(name: String): String {
            val f = File(dir, name)
            check(f.isFile) { "Supertonic voice at $dir is missing $name" }
            return f.absolutePath
        }
        return OfflineTtsConfig(
            model = OfflineTtsModelConfig(
                supertonic = OfflineTtsSupertonicModelConfig(
                    durationPredictor = req("duration_predictor.int8.onnx"),
                    textEncoder = req("text_encoder.int8.onnx"),
                    vectorEstimator = req("vector_estimator.int8.onnx"),
                    vocoder = req("vocoder.int8.onnx"),
                    ttsJson = req("tts.json"),
                    unicodeIndexer = req("unicode_indexer.bin"),
                    voiceStyle = req("voice.bin"),
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
        val repo = runCatching { GlobalContext.get().get<VoiceRepositoryImpl>() }.getOrNull()
            ?: return null
        val snapshot = runCatching { runBlocking { repo.installedSnapshot() } }.getOrNull()
            ?: return null
        return snapshot.firstOrNull { it.voiceId == voiceId }
    }

    companion object {
        private const val TOKENS_FILE = "tokens.txt"
        private const val LEXICON_FILE = "lexicon.txt"
        private const val VOCODER_FILE = "vocos-22khz-univ.onnx"
        private const val ESPEAK_DIR = "espeak-ng-data"
        private const val DICT_DIR = "dict"
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

        /** ZipVoice ships encoder/decoder/vocoder as separate ONNX files;
         *  int8 quantised variants are released first. */
        private val ZIPVOICE_ENCODER_CANDIDATES = listOf(
            "encoder.int8.onnx", "encoder.onnx",
        )
        private val ZIPVOICE_DECODER_CANDIDATES = listOf(
            "decoder.int8.onnx", "decoder.onnx",
        )
        private val ZIPVOICE_VOCODER_CANDIDATES = listOf(
            "vocos_24khz.onnx", "vocos_22khz.onnx", "vocoder.onnx",
        )

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
