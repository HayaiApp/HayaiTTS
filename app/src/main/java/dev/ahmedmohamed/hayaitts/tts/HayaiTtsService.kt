package dev.ahmedmohamed.hayaitts.tts

import android.media.AudioFormat
import android.speech.tts.SynthesisCallback
import android.speech.tts.SynthesisRequest
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeechService
import android.speech.tts.Voice
import java.util.Locale
import kotlin.math.PI
import kotlin.math.sin

/**
 * Phase-1 stub engine. Reports a single English voice and synthesises a 1-second
 * 440 Hz sine tone in place of real speech. The point is to prove that the
 * app registers as a system-wide TTS provider and that `cb.audioAvailable` is
 * wired up correctly. Real neural synthesis (sherpa-onnx) lands in a later
 * phase.
 */
class HayaiTtsService : TextToSpeechService() {

    override fun onCreate() {
        super.onCreate()
        // TextToSpeechService.onCreate() calls onIsLanguageAvailable to choose
        // a startup default; we just have to be ready to answer.
    }

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
            STUB_VOICE_NAME,
            Locale.US,
            Voice.QUALITY_NORMAL,
            Voice.LATENCY_NORMAL,
            /* requiresNetworkConnection = */ false,
            /* features = */ emptySet(),
        ),
    )

    override fun onIsValidVoiceName(name: String?): Int =
        if (name == STUB_VOICE_NAME) TextToSpeech.SUCCESS else TextToSpeech.ERROR

    override fun onLoadVoice(name: String?): Int =
        if (name == STUB_VOICE_NAME) TextToSpeech.SUCCESS else TextToSpeech.ERROR

    @Volatile private var stopRequested: Boolean = false

    override fun onStop() {
        stopRequested = true
    }

    override fun onSynthesizeText(request: SynthesisRequest, callback: SynthesisCallback) {
        stopRequested = false

        if (callback.start(SAMPLE_RATE, AudioFormat.ENCODING_PCM_16BIT, /* channelCount = */ 1)
            != TextToSpeech.SUCCESS
        ) {
            return
        }

        val totalSamples = SAMPLE_RATE // one second of audio
        val bytesPerSample = 2
        val totalBytes = totalSamples * bytesPerSample
        val maxChunkBytes = callback.maxBufferSize.coerceAtLeast(bytesPerSample * 64)
        val chunk = ByteArray(maxChunkBytes)

        var byteCursor = 0
        var sampleCursor = 0
        val twoPiFOverFs = 2.0 * PI * TONE_HZ / SAMPLE_RATE

        while (byteCursor < totalBytes && !stopRequested) {
            val remainingBytes = totalBytes - byteCursor
            val chunkBytes = minOf(maxChunkBytes, remainingBytes) and 0x7FFFFFFE // even
            var writeIdx = 0
            while (writeIdx < chunkBytes) {
                val s = (sin(twoPiFOverFs * sampleCursor) * AMPLITUDE).toInt().toShort()
                // Little-endian 16-bit PCM.
                chunk[writeIdx] = (s.toInt() and 0xFF).toByte()
                chunk[writeIdx + 1] = ((s.toInt() shr 8) and 0xFF).toByte()
                writeIdx += 2
                sampleCursor++
            }
            if (callback.audioAvailable(chunk, 0, chunkBytes) != TextToSpeech.SUCCESS) {
                return
            }
            byteCursor += chunkBytes
        }

        callback.done()
    }

    companion object {
        private const val STUB_VOICE_NAME = "hayai-stub-en"
        private const val SAMPLE_RATE = 22050
        private const val TONE_HZ = 440.0
        private const val AMPLITUDE = 16384.0 // ~half of Short.MAX_VALUE
    }
}
