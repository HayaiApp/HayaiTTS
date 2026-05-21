package dev.ahmedmohamed.hayaitts.ui.speakselection

import android.content.Intent
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import dev.ahmedmohamed.hayaitts.core.dispatchers.DispatcherProvider
import dev.ahmedmohamed.hayaitts.core.result.Outcome
import dev.ahmedmohamed.hayaitts.domain.usecase.SynthesizeUseCase
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import java.util.Locale

/**
 * Entry point for Android's `Intent.ACTION_PROCESS_TEXT`. When the user picks
 * "Speak with HayaiTTS" from a text-selection menu (Chrome, Gmail, etc.) the
 * system fires this activity with the selected text in
 * `Intent.EXTRA_PROCESS_TEXT`.
 *
 * v2.0.0-b1 ships a minimal entry — the activity:
 *   1. Reads the selected text.
 *   2. Resolves an installed voice (first available; future commits will
 *      consult the per-locale default).
 *   3. Synthesizes via [SynthesizeUseCase] and plays through an AudioTrack.
 *   4. Finishes itself.
 *
 * No UI is rendered; the activity is `theme=Theme.Translucent.NoTitleBar`
 * via the manifest entry so it appears as a brief flash.
 */
class SpeakSelectionActivity : ComponentActivity() {

    private val synthesize: SynthesizeUseCase by inject()
    private val dispatchers: DispatcherProvider by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val text = intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString().orEmpty()
        if (text.isBlank()) {
            finish()
            return
        }
        val voiceId = pickVoiceId()
        if (voiceId == null) {
            finish()
            return
        }
        lifecycleScope.launch {
            val result = synthesize(SynthesizeUseCase.Request(voiceId = voiceId, text = text))
            if (result is Outcome.Success) {
                withContext(dispatchers.default) {
                    playPcm(result.value.samples, result.value.sampleRate)
                }
            }
            finish()
        }
    }

    /**
     * Best-effort voice pick. The use case will fail loudly if the picked
     * voice can't be loaded — we don't try to fall back to other voices in
     * the short-lived activity context.
     */
    private fun pickVoiceId(): String? {
        // v2 ships bundled Piper Amy as a guaranteed installation. Using the
        // bundled id keeps Speak Selection working even when the user has
        // not browsed for additional voices yet.
        return "en_US-amy-low"
    }

    private fun playPcm(samples: FloatArray, sampleRate: Int) {
        val sizeBytes = samples.size * 2
        val track = AudioTrack(
            AudioManager.STREAM_MUSIC,
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            sizeBytes,
            AudioTrack.MODE_STATIC,
        )
        val shorts = ShortArray(samples.size) { i ->
            (samples[i].coerceIn(-1f, 1f) * Short.MAX_VALUE).toInt().toShort()
        }
        track.write(shorts, 0, shorts.size)
        track.play()
        // Wait for playback to finish — the activity stays alive until then.
        val frames = samples.size
        val durationMs = (frames * 1000L) / sampleRate
        try {
            Thread.sleep(durationMs + 100)
        } catch (_: InterruptedException) {
            // proceed to release
        }
        track.stop()
        track.release()
    }

    companion object {
        @Suppress("unused") // surfaced via manifest intent filter.
        const val LABEL_RES = "Speak with HayaiTTS"
        const val DEFAULT_LOCALE_TAG = "en-US"

        @Suppress("unused")
        fun defaultLocale(): Locale = Locale.US
    }
}
