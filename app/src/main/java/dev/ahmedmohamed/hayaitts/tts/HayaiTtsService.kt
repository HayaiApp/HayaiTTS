package dev.ahmedmohamed.hayaitts.tts

import android.media.AudioFormat
import android.speech.tts.SynthesisCallback
import android.speech.tts.SynthesisRequest
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeechService
import android.speech.tts.Voice
import co.touchlab.kermit.Logger
import java.util.Locale

/**
 * System-wide TTS engine backed by sherpa-onnx + the bundled Piper Amy voice.
 * Each [onSynthesizeText] call runs on a worker thread the framework provides,
 * so we can safely block on the (cold-cached) [SherpaTtsRuntime] init.
 */
class HayaiTtsService : TextToSpeechService() {

    private val log = Logger.withTag("HayaiTtsService")

    @Volatile private var stopRequested: Boolean = false

    override fun onIsLanguageAvailable(lang: String?, country: String?, variant: String?): Int =
        if (lang.equals("eng", ignoreCase = true)) {
            TextToSpeech.LANG_AVAILABLE
        } else {
            TextToSpeech.LANG_NOT_SUPPORTED
        }

    override fun onLoadLanguage(lang: String?, country: String?, variant: String?): Int =
        onIsLanguageAvailable(lang, country, variant)

    override fun onGetLanguage(): Array<String> = arrayOf("eng", "USA", "")

    override fun onGetVoices(): List<Voice> = listOf(
        Voice(
            VOICE_NAME,
            Locale.US,
            Voice.QUALITY_NORMAL,
            Voice.LATENCY_NORMAL,
            /* requiresNetworkConnection = */ false,
            /* features = */ emptySet(),
        ),
    )

    override fun onIsValidVoiceName(name: String?): Int =
        if (name == VOICE_NAME) TextToSpeech.SUCCESS else TextToSpeech.ERROR

    override fun onLoadVoice(name: String?): Int =
        if (name == VOICE_NAME) TextToSpeech.SUCCESS else TextToSpeech.ERROR

    override fun onStop() {
        stopRequested = true
    }

    override fun onSynthesizeText(request: SynthesisRequest, callback: SynthesisCallback) {
        stopRequested = false

        val rawText = request.charSequenceText?.toString().orEmpty()
        val text = if (rawText.length > MAX_TEXT_LENGTH) {
            rawText.substring(0, MAX_TEXT_LENGTH)
        } else {
            rawText
        }
        if (text.isEmpty()) {
            // Nothing to say — still need to start/end the callback or the
            // framework holds the stream open.
            if (callback.start(SAFE_SAMPLE_RATE, AudioFormat.ENCODING_PCM_16BIT, 1) ==
                TextToSpeech.SUCCESS
            ) callback.done()
            return
        }

        // Android speech rate uses 100 == default. Map linearly to sherpa's
        // playback `speed` (1.0 == default) and clamp to a sane range —
        // sherpa lets you go faster but the audio quality collapses outside
        // ~0.5x..2x.
        val speed = (request.speechRate.toFloat() / 100f).coerceIn(0.5f, 2.0f)

        val runtime = try {
            SherpaTtsRuntime.get(applicationContext)
        } catch (t: Throwable) {
            log.e(t) { "Failed to initialise SherpaTtsRuntime" }
            callback.error()
            return
        }

        if (callback.start(runtime.sampleRate, AudioFormat.ENCODING_PCM_16BIT, /* channelCount = */ 1)
            != TextToSpeech.SUCCESS
        ) {
            return
        }

        val output = try {
            runtime.synthesize(text = text, sid = 0, speed = speed)
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

    /**
     * Convert sherpa-onnx's float samples (-1..1) to little-endian 16-bit PCM
     * and stream them through [SynthesisCallback.audioAvailable] in chunks no
     * larger than [SynthesisCallback.maxBufferSize].
     */
    private fun streamPcm(callback: SynthesisCallback, samples: FloatArray) {
        val maxBufferBytes = callback.maxBufferSize.coerceAtLeast(BYTES_PER_SAMPLE * 64)
        // Force even byte count so we never split a 16-bit sample.
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
                // Clamp to [-1, 1] and scale to 16-bit signed.
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

    companion object {
        private const val VOICE_NAME = "en_US-amy-low"
        private const val BYTES_PER_SAMPLE = 2
        private const val MAX_TEXT_LENGTH = 2000

        // Only used for the empty-text fast-path before the runtime warms up.
        // 22050 Hz is what Piper low models emit.
        private const val SAFE_SAMPLE_RATE = 22050
    }
}
