package dev.ahmedmohamed.hayaitts.tts

import android.media.AudioFormat
import android.speech.tts.SynthesisCallback
import android.speech.tts.SynthesisRequest
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeechService
import android.speech.tts.Voice
import co.touchlab.kermit.Logger
import dev.ahmedmohamed.hayaitts.data.voices.parseLocale
import dev.ahmedmohamed.hayaitts.domain.model.InstalledVoice
import dev.ahmedmohamed.hayaitts.tts.SherpaTtsRuntime.Companion.BUNDLED_VOICE_ID
import java.util.Locale

/**
 * System-wide TTS engine. After Phase 4a this iterates every voice the user
 * has installed (Room) plus the bundled Amy voice, exposing each one to the
 * TTS framework via [onGetVoices].
 *
 * Locale resolution: when the framework picks a [language/country] the
 * framework calls [onIsLanguageAvailable]; we return `LANG_AVAILABLE` if any
 * installed voice tags it (BCP-47 prefix match). The actual voice picked is
 * decided by [SynthesisRequest.voiceName].
 */
class HayaiTtsService : TextToSpeechService() {

    private val log = Logger.withTag("HayaiTtsService")

    @Volatile private var stopRequested: Boolean = false
    @Volatile private var lastSelectedVoiceId: String = BUNDLED_VOICE_ID

    override fun onIsLanguageAvailable(lang: String?, country: String?, variant: String?): Int {
        val voices = runtimeOrNull()?.listAvailableVoices().orEmpty()
        val requested = buildBcp47(lang, country, variant)
        return when {
            requested.isEmpty() -> TextToSpeech.LANG_NOT_SUPPORTED
            voices.any { it.matchesLocale(requested) } -> TextToSpeech.LANG_AVAILABLE
            voices.any { it.matchesLanguage(lang) } -> TextToSpeech.LANG_AVAILABLE
            else -> TextToSpeech.LANG_NOT_SUPPORTED
        }
    }

    override fun onLoadLanguage(lang: String?, country: String?, variant: String?): Int =
        onIsLanguageAvailable(lang, country, variant)

    override fun onGetLanguage(): Array<String> {
        // The framework expects ISO-639 alpha-3 + ISO-3166 alpha-3 + variant.
        // For the currently selected voice, derive these from its first
        // language tag. Defaults to eng/USA so the framework treats the engine
        // as English-capable when no synthesis has run yet.
        val voice = runtimeOrNull()?.listAvailableVoices()?.firstOrNull { it.voiceId == lastSelectedVoiceId }
        val tag = voice?.languages?.firstOrNull() ?: "en-US"
        val locale = parseLocale(tag)
        return arrayOf(locale.isO3Language.ifEmpty { "eng" }, runCatching { locale.isO3Country }.getOrDefault("USA"), "")
    }

    override fun onGetVoices(): List<Voice> {
        val voices = runtimeOrNull()?.listAvailableVoices().orEmpty()
        return voices.map { it.toFrameworkVoice() }
    }

    override fun onIsValidVoiceName(name: String?): Int {
        if (name.isNullOrEmpty()) return TextToSpeech.ERROR
        val voices = runtimeOrNull()?.listAvailableVoices().orEmpty()
        return if (voices.any { it.voiceId == name }) TextToSpeech.SUCCESS else TextToSpeech.ERROR
    }

    override fun onLoadVoice(name: String?): Int {
        if (name.isNullOrEmpty()) return TextToSpeech.ERROR
        val voices = runtimeOrNull()?.listAvailableVoices().orEmpty()
        if (voices.none { it.voiceId == name }) return TextToSpeech.ERROR
        lastSelectedVoiceId = name
        return TextToSpeech.SUCCESS
    }

    override fun onStop() {
        stopRequested = true
    }

    override fun onSynthesizeText(request: SynthesisRequest, callback: SynthesisCallback) {
        stopRequested = false

        val rawText = request.charSequenceText?.toString().orEmpty()
        val text = if (rawText.length > MAX_TEXT_LENGTH) {
            rawText.substring(0, MAX_TEXT_LENGTH)
        } else rawText

        val runtime = try {
            SherpaTtsRuntime.get(applicationContext)
        } catch (t: Throwable) {
            log.e(t) { "Failed to initialise SherpaTtsRuntime" }
            callback.error()
            return
        }

        // Pick the voice. Order: explicit request.voiceName > last selected > bundled.
        val candidates = runtime.listAvailableVoices()
        val voice = candidates.firstOrNull { it.voiceId == request.voiceName }
            ?: candidates.firstOrNull { it.voiceId == lastSelectedVoiceId }
            ?: candidates.firstOrNull { it.bundled }
            ?: candidates.firstOrNull()
            ?: run {
                log.e { "No voices available" }
                callback.error()
                return
            }

        if (text.isEmpty()) {
            if (callback.start(voice.sampleRateHz, AudioFormat.ENCODING_PCM_16BIT, 1) ==
                TextToSpeech.SUCCESS
            ) callback.done()
            return
        }

        val speed = (request.speechRate.toFloat() / 100f).coerceIn(0.5f, 2.0f)
        val sid = request.params
            ?.getString("speakerId")
            ?.toIntOrNull()
            ?: voice.speakers.firstOrNull()?.id
            ?: 0

        val sampleRate = try {
            runtime.sampleRateOf(voice.voiceId)
        } catch (t: Throwable) {
            log.e(t) { "Could not load voice ${voice.voiceId}" }
            callback.error()
            return
        }

        if (callback.start(sampleRate, AudioFormat.ENCODING_PCM_16BIT, /* channelCount = */ 1)
            != TextToSpeech.SUCCESS
        ) {
            return
        }

        val output = try {
            runtime.synthesize(voiceId = voice.voiceId, text = text, sid = sid, speed = speed)
        } catch (t: Throwable) {
            log.e(t) { "sherpa-onnx synthesis failed" }
            callback.error()
            return
        }

        if (stopRequested) {
            callback.done()
            return
        }

        streamPcm(callback, output.samples)
        callback.done()
    }

    private fun runtimeOrNull(): SherpaTtsRuntime? = runCatching {
        SherpaTtsRuntime.get(applicationContext)
    }.getOrNull()

    private fun InstalledVoice.toFrameworkVoice(): Voice {
        val locale = parseLocale(languages.firstOrNull() ?: "en-US")
        return Voice(
            voiceId,
            locale,
            Voice.QUALITY_NORMAL,
            Voice.LATENCY_NORMAL,
            /* requiresNetworkConnection = */ false,
            /* features = */ emptySet(),
        )
    }

    private fun InstalledVoice.matchesLocale(bcp47: String): Boolean = languages.any {
        it.equals(bcp47, ignoreCase = true)
    }

    private fun InstalledVoice.matchesLanguage(lang: String?): Boolean {
        if (lang.isNullOrEmpty()) return false
        val target = parseLocale(lang).language
        if (target.isEmpty()) return false
        return languages.any { parseLocale(it).language.equals(target, ignoreCase = true) }
    }

    /**
     * Best-effort BCP-47 builder for the (lang, country, variant) triple the
     * framework hands us. The framework uses ISO-639 alpha-3 for lang and
     * ISO-3166 alpha-3 for country; we convert via [Locale] so the comparison
     * with the catalog's BCP-47 tags (alpha-2) works.
     */
    private fun buildBcp47(lang: String?, country: String?, variant: String?): String {
        if (lang.isNullOrEmpty()) return ""
        // Locale(String) does not accept ISO-639 alpha-3 reliably; build via Builder.
        val locale = runCatching {
            val builder = Locale.Builder()
            // Try alpha-2 first; if the input is alpha-3, Locale.forLanguageTag handles it.
            val candidate = Locale.forLanguageTag(lang)
            if (candidate.language.isEmpty()) {
                builder.setLanguage(lang.lowercase()).build()
            } else {
                candidate
            }
        }.getOrNull() ?: return ""
        val final = if (!country.isNullOrEmpty()) {
            runCatching {
                Locale.Builder()
                    .setLanguage(locale.language)
                    .setRegion(country)
                    .build()
            }.getOrDefault(locale)
        } else locale
        return final.toLanguageTag()
    }

    /**
     * Convert sherpa-onnx's float samples (-1..1) to little-endian 16-bit PCM
     * and stream them through [SynthesisCallback.audioAvailable] in chunks no
     * larger than [SynthesisCallback.maxBufferSize].
     */
    private fun streamPcm(callback: SynthesisCallback, samples: FloatArray) {
        val maxBufferBytes = callback.maxBufferSize.coerceAtLeast(BYTES_PER_SAMPLE * 64)
        val chunkBytes = (maxBufferBytes and 0x7FFFFFFE)
        val samplesPerChunk = chunkBytes / BYTES_PER_SAMPLE
        val buffer = ByteArray(chunkBytes)

        var sampleCursor = 0
        while (sampleCursor < samples.size && !stopRequested) {
            val remaining = samples.size - sampleCursor
            val take = minOf(samplesPerChunk, remaining)
            var writeIdx = 0
            for (i in 0 until take) {
                val f = samples[sampleCursor + i]
                val clamped = when {
                    f >= 1.0f -> Short.MAX_VALUE.toInt()
                    f <= -1.0f -> Short.MIN_VALUE.toInt()
                    else -> (f * Short.MAX_VALUE).toInt()
                }
                buffer[writeIdx] = (clamped and 0xFF).toByte()
                buffer[writeIdx + 1] = ((clamped shr 8) and 0xFF).toByte()
                writeIdx += 2
            }
            if (callback.audioAvailable(buffer, 0, writeIdx) != TextToSpeech.SUCCESS) return
            sampleCursor += take
        }
    }

    private companion object {
        const val BYTES_PER_SAMPLE = 2
        const val MAX_TEXT_LENGTH = 2000
    }
}
