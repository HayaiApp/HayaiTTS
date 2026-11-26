package dev.ahmedmohamed.hayaitts.tts

import android.media.AudioFormat
import android.speech.tts.SynthesisCallback
import android.speech.tts.SynthesisRequest
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeechService
import android.speech.tts.Voice
import co.touchlab.kermit.Logger
import dev.ahmedmohamed.hayaitts.data.playground.VoiceTuning
import dev.ahmedmohamed.hayaitts.data.playground.VoiceTuningRepository
import dev.ahmedmohamed.hayaitts.data.voices.parseLocale
import dev.ahmedmohamed.hayaitts.domain.model.InstalledVoice
import dev.ahmedmohamed.hayaitts.domain.repo.DefaultsRepository
import dev.ahmedmohamed.hayaitts.domain.repo.SettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.koin.core.context.GlobalContext

/**
 * System-wide TTS engine. Iterates every voice the user has installed
 * (Room), exposing each one to the TTS framework via [onGetVoices].
 *
 * Locale resolution: when the framework picks a [language/country] the
 * framework calls [onIsLanguageAvailable]; we return `LANG_AVAILABLE` if any
 * installed voice tags it (BCP-47 prefix match). The actual voice picked is
 * decided by [SynthesisRequest.voiceName].
 */
class HayaiTtsService : TextToSpeechService() {

    private val log = Logger.withTag("HayaiTtsService")

    @Volatile private var stopRequested: Boolean = false
    @Volatile private var lastSelectedVoiceId: String = ""

    override fun onIsLanguageAvailable(lang: String?, country: String?, variant: String?): Int {
        val requested = TtsLocales.toBcp47(lang, country)
        if (requested.isEmpty()) return TextToSpeech.LANG_NOT_SUPPORTED
        val best = runtimeOrNull()?.listAvailableVoices().orEmpty()
            .maxOfOrNull { TtsLocales.matchLevel(it.languages, requested) } ?: TtsLocales.NOT_SUPPORTED
        return when (best) {
            TtsLocales.COUNTRY -> TextToSpeech.LANG_COUNTRY_AVAILABLE
            TtsLocales.LANGUAGE -> TextToSpeech.LANG_AVAILABLE
            else -> TextToSpeech.LANG_NOT_SUPPORTED
        }
    }

    override fun onLoadLanguage(lang: String?, country: String?, variant: String?): Int {
        val status = onIsLanguageAvailable(lang, country, variant)
        if (status >= TextToSpeech.LANG_AVAILABLE) {
            voiceForLocale(TtsLocales.toBcp47(lang, country))?.let { lastSelectedVoiceId = it.voiceId }
        }
        return status
    }

    /**
     * The framework's default implementation returns the bare locale tag
     * ("en-US") and then asks [onIsValidVoiceName] about it, which never
     * matched our voice ids, so `TextToSpeech.setLanguage()` failed for every
     * client. Return a real installed voice instead: the user's per-locale
     * default if set, else the best locale match.
     */
    override fun onGetDefaultVoiceNameFor(lang: String?, country: String?, variant: String?): String? {
        val voice = voiceForLocale(TtsLocales.toBcp47(lang, country)) ?: return null
        return frameworkNameFor(voice)
    }

    override fun onGetLanguage(): Array<String> {
        // ISO-639 alpha-3 + ISO-3166 alpha-3 + variant, from the last voice we used.
        val (voiceId, _) = parseVoiceName(lastSelectedVoiceId)
        val voice = runtimeOrNull()?.listAvailableVoices()?.firstOrNull { it.voiceId == voiceId }
        val iso3 = TtsLocales.toIso3(voice?.languages?.firstOrNull()?.let { TtsLocales.toBcp47(it) } ?: "en-US") ?: "eng-USA"
        val parts = iso3.split('-')
        return arrayOf(parts[0], parts.getOrElse(1) { "" }, "")
    }

    override fun onGetVoices(): List<Voice> {
        val voices = runtimeOrNull()?.listAvailableVoices().orEmpty()
        return voices.flatMap { voice ->
            if (voice.speakers.size > 1) {
                voice.speakers.map { speaker ->
                    val voiceName = "${voice.voiceId}#${speaker.id}#${speaker.name}"
                    val locale = parseLocale(TtsLocales.toBcp47(voice.languages.firstOrNull() ?: "en-US"))
                    Voice(
                        voiceName,
                        locale,
                        Voice.QUALITY_NORMAL,
                        Voice.LATENCY_NORMAL,
                        /* requiresNetworkConnection = */ false,
                        /* features = */ emptySet(),
                    )
                }
            } else {
                listOf(voice.toFrameworkVoice())
            }
        }
    }

    override fun onIsValidVoiceName(name: String?): Int =
        if (resolveVoiceName(name) != null) TextToSpeech.SUCCESS else TextToSpeech.ERROR

    override fun onLoadVoice(name: String?): Int {
        if (resolveVoiceName(name) == null) return TextToSpeech.ERROR
        lastSelectedVoiceId = name!!
        return TextToSpeech.SUCCESS
    }

    /**
     * Parse a framework voice name and validate it against the installed
     * catalog. Returns the resolved [InstalledVoice] (and optionally the
     * speaker id parsed from a `voiceId#speakerId#name` form) on success, or
     * null if the name is malformed or unknown.
     */
    private fun resolveVoiceName(name: String?): InstalledVoice? {
        if (name.isNullOrEmpty()) return null
        val (voiceId, speakerId) = parseVoiceName(name)
        val voice = runtimeOrNull()?.listAvailableVoices().orEmpty()
            .firstOrNull { it.voiceId == voiceId } ?: return null
        if (speakerId != null && voice.speakers.none { it.id == speakerId }) return null
        return voice
    }

    /**
     * Voice names exposed via [onGetVoices] use the form
     * `voiceId#speakerId#speakerName` for multi-speaker models and plain
     * `voiceId` otherwise. This helper recovers both parts.
     */
    private fun parseVoiceName(name: String): Pair<String, Int?> {
        if (name.isEmpty()) return "" to null
        val parts = name.split("#", limit = 3)
        val voiceId = parts[0]
        val speakerId = parts.getOrNull(1)?.toIntOrNull()
        return voiceId to speakerId
    }

    override fun onStop() {
        stopRequested = true
    }

    override fun onSynthesizeText(request: SynthesisRequest, callback: SynthesisCallback) {
        stopRequested = false

        val text = request.charSequenceText?.toString().orEmpty()

        val runtime = try {
            SherpaTtsRuntime.get(applicationContext)
        } catch (t: Throwable) {
            log.e(t) { "Failed to initialise SherpaTtsRuntime" }
            callback.error(TextToSpeech.ERROR_SERVICE)
            return
        }

        val requestLocale = TtsLocales.toBcp47(request.language, request.country)
        val requestLanguageHint = requestLocale.ifBlank { null }
        val (selectedVoiceId, selectedSpeakerId) = parseVoiceName(request.voiceName.orEmpty())
        val (lastVoiceId, lastSpeakerId) = parseVoiceName(lastSelectedVoiceId)

        // Explicit voice > the user's/best voice for the requested locale >
        // whatever we used last > anything installed.
        val candidates = runtime.listAvailableVoices()
        val explicit = candidates.firstOrNull { it.voiceId == selectedVoiceId }
        val voice = explicit
            ?: voiceForLocale(requestLocale)
            ?: candidates.firstOrNull { it.voiceId == lastVoiceId }
            ?: candidates.firstOrNull()
            ?: run {
                log.e { "No voices available" }
                callback.error(TextToSpeech.ERROR_NOT_INSTALLED_YET)
                return
            }

        val tuningRepo = runCatching { GlobalContext.get().get<VoiceTuningRepository>() }.getOrNull()
        val tuning = tuningRepo?.let { repo ->
            runCatching { runBlocking { repo.tuningFor(voice.voiceId).first() } }.getOrNull()
        } ?: VoiceTuning.Default

        val requestSpeed = (request.speechRate.toFloat() / 100f).coerceIn(0.25f, 4.0f)
        val speed = (requestSpeed * tuning.speed).coerceIn(0.25f, 4.0f)
        val requestPitch = (request.pitch.toFloat() / 100f).coerceIn(0.25f, 4.0f)
        val pitch = (requestPitch * tuning.pitch).coerceIn(0.5f, 2.0f)

        val settingsRepo = runCatching { GlobalContext.get().get<SettingsRepository>() }.getOrNull()
        val defaultSpeaker = settingsRepo?.let { repo ->
            runCatching { runBlocking { repo.defaultSpeakerByVoice.first()[voice.voiceId] } }.getOrNull()
        }

        val sid = (if (explicit != null) selectedSpeakerId else null)
            ?: request.params?.getString("speakerId")?.toIntOrNull()
            ?: (if (voice.voiceId == lastVoiceId) lastSpeakerId else null)
            ?: defaultSpeaker
            ?: voice.speakers.firstOrNull()?.id
            ?: 0

        val sampleRate = try {
            runtime.sampleRateOf(voice.voiceId, languageHint = requestLanguageHint)
        } catch (t: Throwable) {
            log.e(t) { "Could not load voice ${voice.voiceId}" }
            callback.error(TextToSpeech.ERROR_SYNTHESIS)
            return
        }

        if (callback.start(sampleRate, AudioFormat.ENCODING_PCM_16BIT, /* channelCount = */ 1)
            != TextToSpeech.SUCCESS
        ) {
            return
        }

        // Synthesize and stream one section at a time: audio starts after the
        // first sentence, and long requests are never truncated.
        var framesWritten = 0
        var failed = 0
        val sections = TextSections.split(text)
        for (section in sections) {
            if (stopRequested) break
            val output = try {
                runtime.synthesize(
                    voiceId = voice.voiceId,
                    text = section.text,
                    sid = sid,
                    speed = speed,
                    pitch = pitch,
                    lengthScale = tuning.lengthScale,
                    noiseScale = tuning.noiseScale,
                    noiseScaleW = tuning.noiseScaleW,
                    languageHint = requestLanguageHint,
                )
            } catch (t: Throwable) {
                // One unreadable section (odd characters, a model quirk)
                // shouldn't silence the rest of the request.
                log.e(t) { "sherpa-onnx synthesis failed for a section" }
                failed++
                continue
            }
            if (stopRequested) break
            callback.rangeStart(framesWritten, section.start, section.end)
            if (!streamPcm(callback, output.samples)) return
            framesWritten += output.samples.size
        }
        if (sections.isNotEmpty() && failed == sections.size) {
            callback.error(TextToSpeech.ERROR_SYNTHESIS)
            return
        }
        callback.done()
    }

    /** User's per-locale default if installed, else the installed voice that matches [bcp47] best. */
    private fun voiceForLocale(bcp47: String): InstalledVoice? {
        if (bcp47.isBlank()) return null
        val voices = runtimeOrNull()?.listAvailableVoices().orEmpty()
        if (voices.isEmpty()) return null
        val defaults = runCatching {
            runBlocking { GlobalContext.get().get<DefaultsRepository>().defaults.first() }
        }.getOrDefault(emptyMap())
        val language = bcp47.substringBefore('-')
        val pinned = listOf(bcp47, language).firstNotNullOfOrNull { tag ->
            defaults.entries.firstOrNull { it.key.equals(tag, ignoreCase = true) }?.value
        }
        voices.firstOrNull { it.voiceId == pinned }?.let { return it }
        return voices
            .map { it to TtsLocales.matchLevel(it.languages, bcp47) }
            .filter { it.second > TtsLocales.NOT_SUPPORTED }
            .maxByOrNull { it.second }
            ?.first
    }

    /** A name that [onGetVoices] lists, so `TextToSpeech.getVoice()` finds it. */
    private fun frameworkNameFor(voice: InstalledVoice): String {
        if (voice.speakers.size <= 1) return voice.voiceId
        val preferred = runCatching {
            runBlocking { GlobalContext.get().get<SettingsRepository>().defaultSpeakerByVoice.first()[voice.voiceId] }
        }.getOrNull()
        val speaker = voice.speakers.firstOrNull { it.id == preferred } ?: voice.speakers.first()
        return "${voice.voiceId}#${speaker.id}#${speaker.name}"
    }

    private fun runtimeOrNull(): SherpaTtsRuntime? = runCatching {
        SherpaTtsRuntime.get(applicationContext)
    }.getOrNull()

    private fun InstalledVoice.toFrameworkVoice(): Voice {
        val locale = parseLocale(TtsLocales.toBcp47(languages.firstOrNull() ?: "en-US"))
        return Voice(
            voiceId,
            locale,
            Voice.QUALITY_NORMAL,
            Voice.LATENCY_NORMAL,
            /* requiresNetworkConnection = */ false,
            /* features = */ emptySet(),
        )
    }

    /**
     * Convert sherpa-onnx's float samples (-1..1) to little-endian 16-bit PCM
     * and stream them through [SynthesisCallback.audioAvailable] in chunks no
     * larger than [SynthesisCallback.maxBufferSize].
     */
    private fun streamPcm(callback: SynthesisCallback, samples: FloatArray): Boolean {
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
            if (callback.audioAvailable(buffer, 0, writeIdx) != TextToSpeech.SUCCESS) return false
            sampleCursor += take
        }
        return true
    }

    private companion object {
        const val BYTES_PER_SAMPLE = 2
    }
}
