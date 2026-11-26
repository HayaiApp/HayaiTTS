package dev.ahmedmohamed.hayaitts.data.voices

import dev.ahmedmohamed.hayaitts.domain.model.ModelFamily
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Top-level file lists below are copied from the real upstream archives
 * (k2-fsa/sherpa-onnx `tts-models` release, 2026-11), one per layout variant
 * the catalog references. Every one must resolve, so install and load agree.
 */
class BundleLayoutTest {

    @get:Rule val tmp = TemporaryFolder()

    private data class Fixture(val id: String, val family: ModelFamily, val files: List<String>, val dirs: List<String>)

    private val fixtures = listOf(
        Fixture("kitten-micro-en-v0_8", ModelFamily.KITTEN, listOf("model.onnx", "tokens.txt", "voices.bin"), listOf()),
        Fixture("kitten-mini-en-v0_1-fp16", ModelFamily.KITTEN, listOf("model.fp16.onnx", "tokens.txt", "voices.bin"), listOf()),
        Fixture("kitten-mini-en-v0_8", ModelFamily.KITTEN, listOf("model.onnx", "tokens.txt", "voices.bin"), listOf()),
        Fixture("kitten-nano-en-v0_1-fp16", ModelFamily.KITTEN, listOf("model.fp16.onnx", "tokens.txt", "voices.bin"), listOf()),
        Fixture("kitten-nano-en-v0_2-fp16", ModelFamily.KITTEN, listOf("model.fp16.onnx", "tokens.txt", "voices.bin"), listOf()),
        Fixture("kitten-nano-en-v0_8-fp32", ModelFamily.KITTEN, listOf("model.fp32.onnx", "tokens.txt", "voices.bin"), listOf()),
        Fixture("kitten-nano-en-v0_8-int8", ModelFamily.KITTEN, listOf("model.int8.onnx", "tokens.txt", "voices.bin"), listOf()),
        Fixture("kokoro-en-v0_19", ModelFamily.KOKORO, listOf("model.onnx", "tokens.txt", "voices.bin"), listOf()),
        Fixture("kokoro-int8-en-v0_19", ModelFamily.KOKORO, listOf("model.int8.onnx", "tokens.txt", "voices.bin"), listOf()),
        Fixture("kokoro-int8-multi-lang-v1_0", ModelFamily.KOKORO, listOf("date-zh.fst", "lexicon-gb-en.txt", "lexicon-us-en.txt", "lexicon-zh.txt", "model.int8.onnx", "number-zh.fst", "phone-zh.fst", "tokens.txt", "voices.bin"), listOf()),
        Fixture("kokoro-int8-multi-lang-v1_1", ModelFamily.KOKORO, listOf("date-zh.fst", "lexicon-gb-en.txt", "lexicon-us-en.txt", "lexicon-zh.txt", "model.int8.onnx", "number-zh.fst", "phone-zh.fst", "tokens.txt", "voices.bin"), listOf()),
        Fixture("kokoro-multi-lang-v1_0", ModelFamily.KOKORO, listOf("date-zh.fst", "lexicon-gb-en.txt", "lexicon-us-en.txt", "lexicon-zh.txt", "model.onnx", "number-zh.fst", "phone-zh.fst", "tokens.txt", "voices.bin"), listOf()),
        Fixture("kokoro-multi-lang-v1_1", ModelFamily.KOKORO, listOf("date-zh.fst", "lexicon-gb-en.txt", "lexicon-us-en.txt", "lexicon-zh.txt", "model.onnx", "number-zh.fst", "phone-zh.fst", "tokens.txt", "voices.bin"), listOf()),
        Fixture("matcha-icefall-en_US-ljspeech", ModelFamily.MATCHA, listOf("model-steps-3.onnx", "tokens.txt"), listOf()),
        Fixture("matcha-icefall-zh-baker", ModelFamily.MATCHA, listOf("date.fst", "lexicon.txt", "model-steps-3.onnx", "number.fst", "phone.fst", "tokens.txt"), listOf()),
        Fixture("matcha-icefall-zh-en", ModelFamily.MATCHA, listOf("date-zh.fst", "lexicon.txt", "model-steps-3.onnx", "number-zh.fst", "phone-zh.fst", "tokens.txt"), listOf()),
        Fixture("matcha-tts-fa_en-khadijah", ModelFamily.MATCHA, listOf("model.onnx", "tokens.txt"), listOf()),
        Fixture("matcha-tts-fa_en-musa", ModelFamily.MATCHA, listOf("model.onnx", "tokens.txt"), listOf()),
        Fixture("sherpa-onnx-pocket-tts-2026-01-26", ModelFamily.POCKET, listOf("decoder.onnx", "encoder.onnx", "lm_flow.onnx", "lm_main.onnx", "text_conditioner.onnx", "token_scores.json", "vocab.json"), listOf()),
        Fixture("sherpa-onnx-pocket-tts-int8-2026-01-26", ModelFamily.POCKET, listOf("decoder.int8.onnx", "encoder.onnx", "lm_flow.int8.onnx", "lm_main.int8.onnx", "text_conditioner.onnx", "token_scores.json", "vocab.json"), listOf()),
        Fixture("sherpa-onnx-supertonic-3-tts-int8-2026-05-11", ModelFamily.SUPERTONIC, listOf("duration_predictor.int8.onnx", "text_encoder.int8.onnx", "tts.json", "unicode_indexer.bin", "vector_estimator.int8.onnx", "vocoder.int8.onnx", "voice.bin"), listOf()),
        Fixture("sherpa-onnx-supertonic-tts-int8-2026-03-06", ModelFamily.SUPERTONIC, listOf("duration_predictor.int8.onnx", "text_encoder.int8.onnx", "tts.json", "unicode_indexer.bin", "vector_estimator.int8.onnx", "vocoder.int8.onnx", "voice.bin"), listOf()),
        Fixture("sherpa-onnx-zipvoice-distill-fp32-zh-en-emilia", ModelFamily.ZIPVOICE, listOf("decoder.onnx", "encoder.onnx", "lexicon.txt", "tokens.txt"), listOf()),
        Fixture("sherpa-onnx-zipvoice-distill-int8-zh-en-emilia", ModelFamily.ZIPVOICE, listOf("decoder.int8.onnx", "encoder.int8.onnx", "lexicon.txt", "tokens.txt"), listOf()),
        Fixture("sherpa-onnx-zipvoice-distill-zh-en-emilia", ModelFamily.ZIPVOICE, listOf("fm_decoder.onnx", "fm_decoder_int8.onnx", "pinyin.raw", "prompt.wav", "text_encoder.onnx", "text_encoder_int8.onnx", "tokens.txt", "vocos_24khz.onnx"), listOf()),
        Fixture("sherpa-onnx-zipvoice-zh-en-emilia", ModelFamily.ZIPVOICE, listOf("fm_decoder.onnx", "fm_decoder_int8.onnx", "pinyin.raw", "prompt.wav", "text_encoder.onnx", "text_encoder_int8.onnx", "tokens.txt", "vocos_24khz.onnx"), listOf()),
        Fixture("vits-coqui-bg-cv", ModelFamily.VITS, listOf("config.json", "language_ids.json", "model.onnx", "speaker_ids.json", "tokens.txt", "vits-coqui.py", "vits-coqui.sh"), listOf()),
        Fixture("vits-inflect-en-nano-v2", ModelFamily.VITS, listOf("model.onnx", "tokens.txt"), listOf()),
        Fixture("vits-mimic3-el_GR-rapunzelina_low", ModelFamily.VITS, listOf("el_GR-rapunzelina_low.onnx", "el_GR-rapunzelina_low.onnx.json", "tokens.txt", "vits-mimic3.py", "vits-mimic3.sh"), listOf()),
        Fixture("vits-piper-ca_ES-upc_ona-x_low-int8", ModelFamily.PIPER, listOf("ca_ES-upc_ona-x_low.onnx", "ca_ES-upc_ona-x_low.onnx.json", "tokens.txt"), listOf()),
        Fixture("vits-piper-it_IT-riccardo-x_low-fp16", ModelFamily.PIPER, listOf("it_IT-riccardo-x_low.onnx", "it_IT-riccardo-x_low.onnx.json", "tokens.txt"), listOf()),
        Fixture("vits-piper-it_IT-riccardo-x_low", ModelFamily.PIPER, listOf("it_IT-riccardo-x_low.onnx", "it_IT-riccardo-x_low.onnx.json", "tokens.txt"), listOf()),
        Fixture("vits-piper-nl_BE-rdh-x_low", ModelFamily.PIPER, listOf("nl_BE-rdh-x_low.onnx", "nl_BE-rdh-x_low.onnx.json", "tokens.txt", "vits-piper.py", "vits-piper.sh"), listOf()),
    )

    private fun materialize(f: Fixture): File {
        val dir = tmp.newFolder(f.id)
        f.files.forEach { File(dir, it).writeText("x") }
        f.dirs.forEach { File(dir, it).mkdirs() }
        return dir
    }

    /** Matcha vocoders come from the catalog; ZipVoice's from the worker fallback. */
    private fun addSidecars(f: Fixture, dir: File) {
        val hasVocoder = dir.listFiles()!!.any { it.name.startsWith("vocos") }
        if (hasVocoder) return
        when (f.family) {
            ModelFamily.MATCHA -> File(dir, "vocos-22khz-univ.onnx").writeText("x")
            ModelFamily.ZIPVOICE -> File(dir, BundleLayouts.ZIPVOICE_VOCODER_FILE).writeText("x")
            else -> Unit
        }
    }

    @Test
    fun `every real upstream layout resolves once sidecars are present`() {
        val failures = fixtures.mapNotNull { f ->
            val dir = materialize(f)
            addSidecars(f, dir)
            when (val res = BundleLayouts.resolve(f.family, dir)) {
                is LayoutResult.Resolved -> null
                is LayoutResult.Missing -> "${f.id}: ${res.missing}"
            }
        }
        assertTrue(failures.joinToString("\n"), failures.isEmpty())
    }

    @Test
    fun `piper int8 bundle resolves its voice-named onnx instead of model_onnx`() {
        val f = fixtures.first { it.id.endsWith("-int8") && it.family == ModelFamily.PIPER }
        val layout = BundleLayouts.require(f.family, materialize(f)) as BundleLayout.Vits
        assertTrue(layout.model.name, layout.model.name.endsWith(".onnx") && layout.model.name != "model.onnx")
    }

    @Test
    fun `zipvoice int8 without vocoder reports the vocoder as missing`() {
        val f = fixtures.first { it.id.contains("zipvoice-distill-int8") }
        val res = BundleLayouts.resolve(f.family, materialize(f))
        assertTrue(res is LayoutResult.Missing)
        assertTrue((res as LayoutResult.Missing).missing.single().contains("vocoder"))
    }

    @Test
    fun `zipvoice full precision prefers int8 encoder and decoder`() {
        val f = fixtures.first { it.id == "sherpa-onnx-zipvoice-distill-zh-en-emilia" }
        val layout = BundleLayouts.require(f.family, materialize(f)) as BundleLayout.ZipVoice
        assertEquals("text_encoder_int8.onnx", layout.encoder.name)
        assertEquals("fm_decoder_int8.onnx", layout.decoder.name)
        assertEquals("vocos_24khz.onnx", layout.vocoder.name)
    }

    @Test
    fun `kokoro multi-lang keeps zh rule fsts separate from neutral ones`() {
        val f = fixtures.first { it.id == "kokoro-multi-lang-v1_1" }
        val layout = BundleLayouts.require(f.family, materialize(f)) as BundleLayout.Kokoro
        assertTrue(layout.isMultiLang)
        assertEquals(3, layout.lexicons.size)
        assertEquals(3, layout.zhRuleFsts.size)
        assertTrue(layout.ruleFsts.isEmpty())
    }

    @Test
    fun `pocket fp32 resolves non-int8 names`() {
        val f = fixtures.first { it.id == "sherpa-onnx-pocket-tts-2026-01-26" }
        val layout = BundleLayouts.require(f.family, materialize(f)) as BundleLayout.Pocket
        assertEquals("lm_flow.onnx", layout.lmFlow.name)
        assertEquals("decoder.onnx", layout.decoder.name)
    }

    @Test
    fun `missing tokens is reported by name`() {
        val dir = tmp.newFolder("broken")
        File(dir, "en_US-amy-low.onnx").writeText("x")
        val res = BundleLayouts.resolve(ModelFamily.PIPER, dir) as LayoutResult.Missing
        assertEquals(listOf("tokens.txt"), res.missing)
    }
}
