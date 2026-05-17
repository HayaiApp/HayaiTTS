package dev.ahmedmohamed.hayaitts.data.preview

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import co.touchlab.kermit.Logger
import dev.ahmedmohamed.hayaitts.tts.SherpaTtsRuntime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Synthesizes a short preview sentence on the requested voice and streams the
 * resulting PCM through a one-shot [AudioTrack]. The track is released as soon
 * as playback completes (or [stop] is called) so we never accumulate native
 * handles across previews.
 *
 * sherpa-onnx returns audio as `FloatArray` in [-1.0, 1.0] — we convert to
 * 16-bit signed little-endian PCM here because [AudioFormat.ENCODING_PCM_16BIT]
 * is the only encoding guaranteed across every API level the app supports.
 *
 * Voice Detail's waveform widget consumes [amplitudes]: a 32-bin RMS envelope
 * sampled in ~60 ms windows during playback, scaled to [0f, 1f]. The flow
 * emits zero-arrays when idle so observers can render a flat baseline.
 */
class VoicePreviewPlayer(private val context: Context) {

    private val log = Logger.withTag("VoicePreview")
    private val runtime: SherpaTtsRuntime get() = SherpaTtsRuntime.get(context)

    @Volatile
    private var current: AudioTrack? = null

    private val _amplitudes = MutableStateFlow(FloatArray(BAR_COUNT))
    /** 32 normalized RMS bins, updated on every ~60ms playback frame. */
    val amplitudes: StateFlow<FloatArray> = _amplitudes.asStateFlow()

    /** Blocks until synth + playback finishes. Cancel-friendly via [stop]. */
    suspend fun play(voiceId: String, text: String, sid: Int = 0) = withContext(Dispatchers.Default) {
        stop()
        val output = runCatching { runtime.synthesize(voiceId, text, sid = sid) }
            .onFailure { log.w(it) { "Preview synth failed for $voiceId" } }
            .getOrNull() ?: return@withContext

        val pcm = floatToPcm16(output.samples)
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build(),
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(output.sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build(),
            )
            .setBufferSizeInBytes(pcm.size.coerceAtLeast(MIN_BUFFER_BYTES))
            .setTransferMode(AudioTrack.MODE_STREAM)
            .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
            .build()

        current = track
        track.setVolume(AudioTrack.getMaxVolume())
        track.play()
        runCatching {
            // Stream the pre-encoded PCM in one go; AudioTrack will pull at its
            // own pace from the buffer we just sized to the full clip.
            track.write(pcm, 0, pcm.size)
            // Block until the hardware drains so callers can chain previews.
            // Sample count = bytes / 2; frames at sampleRate -> wall-clock.
            val frames = pcm.size / 2
            val playbackMs = (frames.toLong() * 1000L / output.sampleRate)
            // Stream amplitude bins while the track drains so the waveform UI
            // animates with the audio. We don't touch the AudioTrack itself —
            // the float samples we already have are the source of truth — so
            // there's no latency penalty.
            publishAmplitudeWindows(
                samples = output.samples,
                sampleRate = output.sampleRate,
                playbackMs = playbackMs,
            )
        }
        stop()
    }

    /** Releases the currently playing track, if any. Idempotent. */
    fun stop() {
        // Resetting amplitudes here is what makes the UI fall back to the flat
        // idle waveform the instant playback ends or the user taps Stop.
        _amplitudes.value = FloatArray(BAR_COUNT)
        val toRelease = current ?: return
        current = null
        runCatching { toRelease.stop() }
        runCatching { toRelease.release() }
    }

    /**
     * Emits RMS bins for [samples] at ~60 ms intervals, sleeping in between to
     * match wall-clock playback. Cooperatively bails out when [stop] clears the
     * AudioTrack reference (i.e. the screen unmounted or the user cancelled).
     */
    private fun publishAmplitudeWindows(
        samples: FloatArray,
        sampleRate: Int,
        playbackMs: Long,
    ) {
        val windowMs = WINDOW_MS
        // Cast to Int — sherpa-onnx voices top out at 48 kHz so a 60 ms window
        // is at most 2880 samples, well within Int range.
        val windowSamples = ((sampleRate.toLong() * windowMs / 1000L).toInt()).coerceAtLeast(1)
        val totalWindows = (samples.size + windowSamples - 1) / windowSamples
        // Walk windows in BAR_COUNT-sized strides so each emitted FloatArray is
        // a full snapshot of the next N windows. That lets the UI draw a
        // moving waveform without having to maintain ring-buffer state.
        var cursor = 0
        val tail = FloatArray(BAR_COUNT)
        while (cursor < totalWindows && current != null) {
            // Shift the tail buffer left by one and append the new RMS bin so
            // the right-most bar is always "now".
            for (i in 0 until BAR_COUNT - 1) tail[i] = tail[i + 1]
            tail[BAR_COUNT - 1] = rmsOfWindow(samples, cursor * windowSamples, windowSamples)
            _amplitudes.value = tail.copyOf()
            cursor++
            Thread.sleep(windowMs)
        }
        // Drain a few zero frames so the bars settle back to baseline before
        // stop() resets them entirely. Without this the last loud frame is
        // visible during the AudioTrack drain (~50 ms).
        val cooldownMs = (playbackMs - cursor * windowMs).coerceAtLeast(0L)
        if (cooldownMs > 0) Thread.sleep(cooldownMs.coerceAtMost(120L))
    }

    private fun rmsOfWindow(samples: FloatArray, start: Int, count: Int): Float {
        if (start >= samples.size) return 0f
        val end = min(samples.size, start + count)
        var sum = 0.0
        for (i in start until end) {
            val s = samples[i].toDouble()
            sum += s * s
        }
        val rms = sqrt(sum / (end - start).coerceAtLeast(1))
        // RMS for sherpa output rarely exceeds ~0.4; scale to [0,1] so the bars
        // span the full visual range.
        return (rms * 2.5).toFloat().coerceIn(0f, 1f)
    }

    private fun floatToPcm16(samples: FloatArray): ByteArray {
        val buf = ByteBuffer.allocate(samples.size * 2).order(ByteOrder.LITTLE_ENDIAN)
        for (s in samples) {
            val clamped = (s.coerceIn(-1f, 1f) * Short.MAX_VALUE).toInt().toShort()
            buf.putShort(clamped)
        }
        return buf.array()
    }

    companion object {
        private const val MIN_BUFFER_BYTES = 4 * 1024
        @Suppress("unused")
        private const val LEGACY_MODE = AudioManager.STREAM_MUSIC // doc-only

        // Waveform widget renders 32 bars; the bin window is 60 ms which lines
        // up nicely with a 60 fps display refresh budget.
        private const val BAR_COUNT = 32
        private const val WINDOW_MS = 60L
    }
}
