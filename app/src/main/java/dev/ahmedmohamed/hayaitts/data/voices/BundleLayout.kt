package dev.ahmedmohamed.hayaitts.data.voices

import dev.ahmedmohamed.hayaitts.domain.model.ModelFamily
import java.io.File

/**
 * The files a sherpa-onnx bundle needs, resolved from what is actually on
 * disk. Both the install validator and the runtime use [BundleLayouts.resolve],
 * so a bundle that installs is a bundle that loads.
 *
 * Upstream archive names drift (Piper int8/fp16 variants ship
 * `<voice>.onnx`, ZipVoice full-precision ships `text_encoder.onnx` +
 * `fm_decoder.onnx`, Kokoro int8 ships `model.int8.onnx`, Pocket fp32 drops
 * the `.int8` infix, …), so every role is matched by preferred names first,
 * then by a pattern over the top-level `.onnx` files.
 */
sealed interface BundleLayout {
    val dir: File

    data class Vits(
        override val dir: File,
        val model: File,
        val tokens: File,
        val lexicon: File?,
        val dataDir: File?,
        val dictDir: File?,
        val ruleFsts: List<File>,
    ) : BundleLayout

    data class Matcha(
        override val dir: File,
        val acoustic: File,
        val vocoder: File,
        val tokens: File,
        val lexicon: File?,
        val dataDir: File?,
        val dictDir: File?,
        val ruleFsts: List<File>,
    ) : BundleLayout

    data class Kokoro(
        override val dir: File,
        val model: File,
        val voices: File,
        val tokens: File,
        /** `lexicon-*.txt` for multi-lang releases, else `lexicon.txt` if present. */
        val lexicons: List<File>,
        val dataDir: File?,
        val dictDir: File?,
        /** Language-neutral rule FSTs (`date.fst`, …). */
        val ruleFsts: List<File>,
        /** Chinese-only rule FSTs (`date-zh.fst`, …) shipped by multi-lang releases. */
        val zhRuleFsts: List<File>,
        val isMultiLang: Boolean,
    ) : BundleLayout

    data class Kitten(
        override val dir: File,
        val model: File,
        val voices: File,
        val tokens: File,
        val dataDir: File?,
    ) : BundleLayout

    data class ZipVoice(
        override val dir: File,
        val encoder: File,
        val decoder: File,
        val vocoder: File,
        val tokens: File,
        val lexicon: File?,
        val dataDir: File?,
    ) : BundleLayout

    data class Pocket(
        override val dir: File,
        val lmFlow: File,
        val lmMain: File,
        val encoder: File,
        val decoder: File,
        val textConditioner: File,
        val vocabJson: File,
        val tokenScoresJson: File,
    ) : BundleLayout

    data class Supertonic(
        override val dir: File,
        val durationPredictor: File,
        val textEncoder: File,
        val vectorEstimator: File,
        val vocoder: File,
        val ttsJson: File,
        val unicodeIndexer: File,
        val voiceStyle: File,
    ) : BundleLayout
}

sealed interface LayoutResult {
    data class Resolved(val layout: BundleLayout) : LayoutResult

    /** [missing] names roles, e.g. `model (*.onnx)` or `tokens.txt`. */
    data class Missing(val missing: List<String>) : LayoutResult
}

object BundleLayouts {

    /** Vocoder ZipVoice int8 bundles expect but don't ship. */
    const val ZIPVOICE_VOCODER_FILE = "vocos_24khz.onnx"
    const val ZIPVOICE_VOCODER_URL =
        "https://github.com/k2-fsa/sherpa-onnx/releases/download/vocoder-models/vocos_24khz.onnx"
    const val ZIPVOICE_VOCODER_SHA256 =
        "bcb3b970e384161c4d634f0bb9e999ff1c471b34c9bc0b1049a5014065ed3cc0"

    fun resolve(
        family: ModelFamily,
        dir: File,
        modelFileName: String? = null,
        vocoderFileName: String? = null,
    ): LayoutResult {
        val r = Resolver(dir)
        val layout: BundleLayout? = when (family) {
            ModelFamily.PIPER, ModelFamily.VITS -> {
                val model = r.onnx("model (*.onnx)", listOfNotNull(modelFileName, "model.onnx", "vits-vctk.onnx", "vits-vctk.int8.onnx")) { true }
                val tokens = r.file(TOKENS)
                if (model != null && tokens != null) {
                    BundleLayout.Vits(dir, model, tokens, r.optional(LEXICON), r.dataDir(), r.dictDir(), r.ruleFsts())
                } else null
            }
            ModelFamily.MATCHA -> {
                val acoustic = r.onnx("acoustic model (*.onnx)", listOfNotNull(modelFileName, "model-steps-3.onnx", "model-steps-6.onnx", "model-steps-2.onnx", "acoustic.onnx")) { !it.isVocoderName() }
                val vocoder = r.onnx("vocoder (vocos-*.onnx)", listOfNotNull(vocoderFileName, "vocos-22khz-univ.onnx", "vocos-16khz-univ.onnx", "vocoder.onnx")) { it.isVocoderName() }
                val tokens = r.file(TOKENS)
                if (acoustic != null && vocoder != null && tokens != null) {
                    BundleLayout.Matcha(dir, acoustic, vocoder, tokens, r.optional(LEXICON), r.dataDir(), r.dictDir(), r.ruleFsts())
                } else null
            }
            ModelFamily.KOKORO -> {
                val model = r.onnx("model (*.onnx)", listOfNotNull(modelFileName, "model.onnx", "model.int8.onnx")) { true }
                val voices = r.file(VOICES_BIN)
                val tokens = r.file(TOKENS)
                val multi = r.topLevel().filter { it.name.startsWith("lexicon-") && it.name.endsWith(".txt") }.sortedBy { it.name }
                val isMulti = multi.isNotEmpty() || dir.name.contains("multi-lang", ignoreCase = true)
                val lexicons = multi.ifEmpty { listOfNotNull(r.optional(LEXICON)) }
                val zhFsts = ZH_RULE_FSTS.mapNotNull { r.optional(it) }
                if (model != null && voices != null && tokens != null) {
                    BundleLayout.Kokoro(dir, model, voices, tokens, lexicons, r.dataDir(), r.dictDir(), r.neutralRuleFsts(), zhFsts, isMulti)
                } else null
            }
            ModelFamily.KITTEN -> {
                val model = r.onnx("model (*.onnx)", listOfNotNull(modelFileName, "model.onnx", "model.int8.onnx", "model.fp16.onnx", "model.fp32.onnx")) { true }
                val voices = r.file(VOICES_BIN)
                val tokens = r.file(TOKENS)
                if (model != null && voices != null && tokens != null) {
                    BundleLayout.Kitten(dir, model, voices, tokens, r.dataDir())
                } else null
            }
            ModelFamily.ZIPVOICE -> {
                // int8: encoder.int8.onnx + decoder.int8.onnx, vocoder fetched separately.
                // fp32: text_encoder.onnx + fm_decoder.onnx + vocos_24khz.onnx.
                val encoder = r.onnx("encoder (*encoder*.onnx)", listOf("encoder.int8.onnx", "text_encoder_int8.onnx", "text_encoder.int8.onnx", "encoder.onnx", "text_encoder.onnx")) { it.contains("encoder") }
                val decoder = r.onnx("decoder (*decoder*.onnx)", listOf("decoder.int8.onnx", "fm_decoder_int8.onnx", "fm_decoder.int8.onnx", "decoder.onnx", "fm_decoder.onnx")) { it.contains("decoder") }
                val vocoder = r.onnx("vocoder ($ZIPVOICE_VOCODER_FILE)", listOfNotNull(vocoderFileName, ZIPVOICE_VOCODER_FILE, "vocos_22khz.onnx", "vocoder.onnx")) { it.isVocoderName() }
                val tokens = r.file(TOKENS)
                if (encoder != null && decoder != null && vocoder != null && tokens != null) {
                    BundleLayout.ZipVoice(dir, encoder, decoder, vocoder, tokens, r.optional(LEXICON), r.dataDir())
                } else null
            }
            ModelFamily.POCKET -> {
                val lmFlow = r.onnx("lm_flow*.onnx", listOf("lm_flow.int8.onnx", "lm_flow.onnx")) { it.startsWith("lm_flow") }
                val lmMain = r.onnx("lm_main*.onnx", listOf("lm_main.int8.onnx", "lm_main.onnx")) { it.startsWith("lm_main") }
                val encoder = r.onnx("encoder*.onnx", listOf("encoder.onnx", "encoder.int8.onnx")) { it.startsWith("encoder") }
                val decoder = r.onnx("decoder*.onnx", listOf("decoder.int8.onnx", "decoder.onnx")) { it.startsWith("decoder") }
                val cond = r.onnx("text_conditioner*.onnx", listOf("text_conditioner.onnx", "text_conditioner.int8.onnx")) { it.startsWith("text_conditioner") }
                val vocab = r.file("vocab.json")
                val scores = r.file("token_scores.json")
                if (lmFlow != null && lmMain != null && encoder != null && decoder != null && cond != null && vocab != null && scores != null) {
                    BundleLayout.Pocket(dir, lmFlow, lmMain, encoder, decoder, cond, vocab, scores)
                } else null
            }
            ModelFamily.SUPERTONIC -> {
                val dp = r.onnx("duration_predictor*.onnx", listOf("duration_predictor.int8.onnx", "duration_predictor.onnx")) { it.startsWith("duration_predictor") }
                val te = r.onnx("text_encoder*.onnx", listOf("text_encoder.int8.onnx", "text_encoder.onnx")) { it.startsWith("text_encoder") }
                val ve = r.onnx("vector_estimator*.onnx", listOf("vector_estimator.int8.onnx", "vector_estimator.onnx")) { it.startsWith("vector_estimator") }
                val voc = r.onnx("vocoder*.onnx", listOf("vocoder.int8.onnx", "vocoder.onnx")) { it.startsWith("vocoder") }
                val ttsJson = r.file("tts.json")
                val indexer = r.file("unicode_indexer.bin")
                val style = r.file("voice.bin")
                if (dp != null && te != null && ve != null && voc != null && ttsJson != null && indexer != null && style != null) {
                    BundleLayout.Supertonic(dir, dp, te, ve, voc, ttsJson, indexer, style)
                } else null
            }
            ModelFamily.CUSTOM -> {
                r.missing += "family (custom bundles resolve via their effective family)"
                null
            }
        }
        return layout?.let { LayoutResult.Resolved(it) } ?: LayoutResult.Missing(r.missing.toList())
    }

    /** Convenience for callers that want the layout or an exception with the missing roles. */
    fun require(family: ModelFamily, dir: File, modelFileName: String? = null, vocoderFileName: String? = null): BundleLayout =
        when (val res = resolve(family, dir, modelFileName, vocoderFileName)) {
            is LayoutResult.Resolved -> res.layout
            is LayoutResult.Missing -> error("Bundle at $dir is missing: ${res.missing.joinToString()}")
        }

    private class Resolver(val dir: File) {
        val missing = mutableListOf<String>()
        private val files: List<File> by lazy { dir.listFiles()?.filter { it.isFile }.orEmpty() }

        fun topLevel(): List<File> = files

        fun file(name: String): File? = File(dir, name).takeIf { it.isFile } ?: run { missing += name; null }

        fun optional(name: String): File? = File(dir, name).takeIf { it.isFile }

        /**
         * Picks an `.onnx` for one role: the first existing preferred name,
         * else the single best top-level `.onnx` whose name passes [matches].
         * Sidecar weights for other roles are excluded from the fallback.
         */
        fun onnx(label: String, preferred: List<String>, matches: (String) -> Boolean): File? {
            preferred.firstOrNull { File(dir, it).isFile }?.let { return File(dir, it) }
            val pool = files.filter { it.extension == "onnx" && matches(it.name.lowercase()) }
            val pick = pool.minWithOrNull(compareBy<File>({ quantRank(it.name) }, { it.name }))
            if (pick == null) missing += label
            return pick
        }

        fun dataDir(): File? = File(dir, ESPEAK_DIR).takeIf { it.isDirectory }
        fun dictDir(): File? = File(dir, DICT_DIR).takeIf { it.isDirectory }
        /** Single-language bundles: whatever rule FSTs they ship (zh ones are `*-zh.fst`). */
        fun ruleFsts(): List<File> = (RULE_FSTS + ZH_RULE_FSTS).mapNotNull { optional(it) }

        fun neutralRuleFsts(): List<File> = RULE_FSTS.mapNotNull { optional(it) }
    }

    /** Prefer the smallest/fastest variant when a bundle ships several. */
    private fun quantRank(name: String): Int = when {
        name.contains(".int8.") || name.contains("_int8.") -> 0
        name.contains(".fp16.") || name.contains("_fp16.") -> 1
        else -> 2
    }

    /** True if [dir] has a top-level vocoder `.onnx` (vocos/hifigan/vocoder). */
    fun hasVocoder(dir: File): Boolean =
        dir.listFiles()?.any { it.isFile && it.extension == "onnx" && it.name.isVocoderName() } == true

    internal fun String.isVocoderName(): Boolean {
        val n = lowercase()
        return n.startsWith("vocos") || n.contains("vocoder") || n.startsWith("hifigan")
    }

    private const val TOKENS = "tokens.txt"
    private const val LEXICON = "lexicon.txt"
    private const val VOICES_BIN = "voices.bin"
    private const val ESPEAK_DIR = "espeak-ng-data"
    private const val DICT_DIR = "dict"
    private val RULE_FSTS = listOf("date.fst", "number.fst", "phone.fst")
    private val ZH_RULE_FSTS = listOf("date-zh.fst", "number-zh.fst", "phone-zh.fst")
}
