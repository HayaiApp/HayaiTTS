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
import dev.ahmedmohamed.hayaitts.data.playground.PitchResampler
import dev.ahmedmohamed.hayaitts.data.voices.VoiceRepositoryImpl
import dev.ahmedmohamed.hayaitts.domain.model.InstalledVoice
import dev.ahmedmohamed.hayaitts.domain.model.ModelFamily
import kotlinx.coroutines.runBlocking
import org.koin.core.context.GlobalContext
import java.io.File

/**
 * Multi-voice wrapper around sherpa-onnx [OfflineTts]. The runtime holds an
 * LRU of up to [MAX_LOADED_VOICES] loaded engines keyed by
 * `(voiceId, lengthScaleBucket)` — lengthScale is part of the key because it
 * is baked into the config at engine-construction time. Pitch is a pure
 * post-process on the FloatArray output and does NOT affect cache keys.
 */
class SherpaTtsRuntime private constructor(
    private val context: Context,
) {

    private val log = Logger.withTag("SherpaTtsRuntime")

    /** Composite cache key. lengthBucket = lengthScale * 100, integer. */
    private data class EngineKey(val voiceId: String, val lengthBucket: Int)

    private val loaded = LinkedHashMap<EngineKey, OfflineTts>(MAX_LOADED_VOICES, 0.75f, true)

    data class SynthesisOutput(val sampleRate: Int, val samples: FloatArray)

    fun sampleRateOf(voiceId: String): Int = engine(voiceId, 1f).sampleRate()

    fun synthesize(
        voiceId: String,
        text: String,
        sid: Int = 0,
        speed: Float = 1.0f,
        pitch: Float = 1.0f,
        lengthScale: Float = 1.0f,
    ): SynthesisOutput {
        val tts = engine(voiceId, lengthScale)
        val audio = tts.generate(text = text, sid = sid, speed = speed)
        val shifted = if (kotlin.math.abs(pitch - 1f) < 0.001f) {
            audio.samples
        } else {
            PitchResampler.resample(audio.samples, pitch)
        }
        return SynthesisOutput(sampleRate = audio.sampleRate, samples = shifted)
    }

    fun listAvailableVoices(): List<InstalledVoice> {
        val repo = runCatching { GlobalContext.get().get<VoiceRepositoryImpl>() }.getOrNull()
            ?: return emptyList()
        return runCatching { runBlocking { repo.installedSnapshot() } }
            .getOrElse {
                log.w(it) { "installedSnapshot failed; returning empty voice list" }
                emptyList()
            }
    }

    private fun engine(voiceId: String, lengthScale: Float): OfflineTts = synchronized(this) {
        val key = EngineKey(voiceId, lengthBucket(lengthScale))
        loaded[key]?.let { return@synchronized it }
        val tts = buildEngine(voiceId, lengthScale.coerceIn(0.5f, 2.0f))
        loaded[key] = tts
        evictIfNeeded(voiceId)
        tts
    }

    private fun evictIfNeeded(currentVoiceId: String) {
        // Step 1: cap distinct lengthScale buckets per voice. The most-recently-used
        // bucket(s) for the active voice stay hot so a slider bounce doesn't churn
        // native handles.
        val perVoiceKeys = loaded.keys.filter { it.voiceId == currentVoiceId }
        if (perVoiceKeys.size > MAX_LENGTH_BUCKETS_PER_VOICE) {
            val drop = perVoiceKeys.size - MAX_LENGTH_BUCKETS_PER_VOICE
            perVoiceKeys.take(drop).forEach { key ->
                val evicted = loaded.remove(key)
                evicted?.runCatching { release() }
                log.i { "Evicted lengthScale bucket ${key.lengthBucket} for $currentVoiceId" }
            }
        }
        // Step 2: classic LRU over the whole cache.
        while (loaded.size > MAX_LOADED_VOICES) {
            val oldestKey = loaded.keys.iterator().next()
            val evicted = loaded.remove(oldestKey)
            evicted?.runCatching { release() }
            log.i { "Evicted ${oldestKey.voiceId}@${oldestKey.lengthBucket} from runtime cache" }
        }
    }

    private fun lengthBucket(lengthScale: Float): Int =
        (lengthScale.coerceIn(0.5f, 2.0f) * 100f).toInt()

    private fun buildEngine(voiceId: String, lengthScale: Float): OfflineTts {
        val voice = installedVoiceOf(voiceId)
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
        log.i { "Loading voice $voiceId (family=$family, lengthScale=$lengthScale) from $voiceDir" }
        val config = buildConfig(family, voiceDir, lengthScale)
        val tts = OfflineTts(assetManager = null, config = config)
        log.i { "OfflineTts ready for $voiceId (sampleRate=${tts.sampleRate()})" }
        return tts
    }

    private fun buildConfig(family: ModelFamily, dir: File, lengthScale: Float): OfflineTtsConfig = when (family) {
        ModelFamily.PIPER, ModelFamily.VITS -> buildVitsConfig(dir, lengthScale)
        ModelFamily.MATCHA -> buildMatchaConfig(dir, lengthScale)
        ModelFamily.KOKORO -> buildKokoroConfig(dir, lengthScale)
        ModelFamily.KITTEN -> buildKittenConfig(dir, lengthScale)
        ModelFamily.ZIPVOICE -> buildZipVoiceConfig(dir)
        ModelFamily.POCKET -> buildPocketConfig(dir)
        ModelFamily.SUPERTONIC -> buildSupertonicConfig(dir)
        ModelFamily.CUSTOM -> throw IllegalStateException(
            "Custom voices should have been resolved to an effective family before reaching buildConfig.",
        )
    }

    private fun buildVitsConfig(dir: File, lengthScale: Float): OfflineTtsConfig {
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
                    model = modelPath, lexicon = lexiconPath, tokens = tokensPath,
                    dataDir = dataDirPath, dictDir = dictDirPath, lengthScale = lengthScale,
                ),
                numThreads = 1, debug = false,
            ),
            ruleFsts = collectRuleFsts(dir), maxNumSentences = 2,
        )
    }

    private fun buildMatchaConfig(dir: File, lengthScale: Float): OfflineTtsConfig {
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
                    acousticModel = acoustic, vocoder = vocoder.absolutePath,
                    lexicon = lexiconPath, tokens = tokensPath,
                    dataDir = dataDirPath, dictDir = dictDirPath, lengthScale = lengthScale,
                ),
                numThreads = 1, debug = false,
            ),
            ruleFsts = collectRuleFsts(dir), maxNumSentences = 2,
        )
    }

    private fun buildKokoroConfig(dir: File, lengthScale: Float): OfflineTtsConfig {
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
                    model = modelPath, voices = voices.absolutePath, tokens = tokensPath,
                    dataDir = dataDirPath, lexicon = lexiconPath, dictDir = dictDirPath,
                    lengthScale = lengthScale,
                ),
                numThreads = 2, debug = false,
            ),
            ruleFsts = collectRuleFsts(dir), maxNumSentences = 2,
        )
    }

    private fun buildKittenConfig(dir: File, lengthScale: Float): OfflineTtsConfig {
        val modelPath = resolveModelFile(dir, KITTEN_MODEL_CANDIDATES)
        val voices = File(dir, KOKORO_VOICES_FILE)
        check(voices.isFile) { "Kitten voice at $dir is missing $KOKORO_VOICES_FILE" }
        val tokensPath = File(dir, TOKENS_FILE).absolutePath
        val dataDir = File(dir, ESPEAK_DIR)
        val dataDirPath = if (dataDir.isDirectory) dataDir.absolutePath else ""
        return OfflineTtsConfig(
            model = OfflineTtsModelConfig(
                kitten = OfflineTtsKittenModelConfig(
                    model = modelPath, voices = voices.absolutePath, tokens = tokensPath,
                    dataDir = dataDirPath, lengthScale = lengthScale,
                ),
                numThreads = 2, debug = false,
            ),
            maxNumSentences = 2,
        )
    }

    /**
     * ZipVoice has no lengthScale slot — slider is a no-op for these voices.
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
                    tokens = tokensPath, encoder = encoder, decoder = decoder,
                    vocoder = vocoder, dataDir = dataDirPath, lexicon = lexiconPath,
                ),
                numThreads = 2, debug = false,
            ),
            maxNumSentences = 2,
        )
    }

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
                numThreads = 2, debug = false,
            ),
            maxNumSentences = 2,
        )
    }

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
                numThreads = 2, debug = false,
            ),
            maxNumSentences = 2,
        )
    }

    private fun resolveModelFile(dir: File, candidates: List<String>): String {
        candidates.forEach { name ->
            val candidate = File(dir, name)
            if (candidate.isFile) return candidate.absolutePath
        }
        val firstOnnx = dir.listFiles()?.firstOrNull { it.isFile && it.extension == "onnx" }
        if (firstOnnx != null) {
            log.w { "Unknown bundle layout at $dir, falling back to ${firstOnnx.name}" }
            return firstOnnx.absolutePath
        }
        error("No .onnx weight file found in $dir (tried $candidates)")
    }

    private fun collectRuleFsts(dir: File): String {
        val present = RULE_FST_FILES.mapNotNull { name ->
            val f = File(dir, name)
            if (f.isFile) f.absolutePath else null
        }
        return present.joinToString(",")
    }

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
         * Hard cap on distinct lengthScale buckets per voiceId. 2 = "the user's
         * last two slider positions stay hot." Combined with [MAX_LOADED_VOICES],
         * the per-voice cap runs first then the global LRU enforces the ceiling.
         */
        private const val MAX_LENGTH_BUCKETS_PER_VOICE = 2

        private val VITS_MODEL_CANDIDATES = listOf("model.onnx", "vits-vctk.onnx", "vits-vctk.int8.onnx")
        private val MATCHA_ACOUSTIC_CANDIDATES = listOf("model-steps-3.onnx", "model-steps-6.onnx", "acoustic.onnx")
        private val KOKORO_MODEL_CANDIDATES = listOf("model.onnx", "kokoro-multi-lang-v1_0.onnx", "kokoro-en-v0_19.onnx")
        private val KITTEN_MODEL_CANDIDATES = listOf("model.onnx", "model.int8.onnx")
        private const val KOKORO_VOICES_FILE = "voices.bin"
        private val ZIPVOICE_ENCODER_CANDIDATES = listOf("encoder.int8.onnx", "encoder.onnx")
        private val ZIPVOICE_DECODER_CANDIDATES = listOf("decoder.int8.onnx", "decoder.onnx")
        private val ZIPVOICE_VOCODER_CANDIDATES = listOf("vocos_24khz.onnx", "vocos_22khz.onnx", "vocoder.onnx")
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
