package dev.ahmedmohamed.hayaitts.data.preview

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import co.touchlab.kermit.Logger
import dev.ahmedmohamed.hayaitts.core.dispatchers.DispatcherProvider
import dev.ahmedmohamed.hayaitts.data.playground.VoiceTuning
import dev.ahmedmohamed.hayaitts.tts.SherpaTtsRuntime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Voice Detail's waveform widget consumes [amplitudes]: a 32-bin RMS envelope
 * sampled in ~60 ms windows during playback. The Playground screen subscribes
 * to the same flow — every code path that ends up in [playSamples] feeds it.
 */
class VoicePreviewPlayer(
    private val context: Context,
    private val dispatchers: DispatcherProvider,
) {

    private val log = Logger.withTag("VoicePreview")
    private val runtime: SherpaTtsRuntime get() = SherpaTtsRuntime.get(context)

    @Volatile
    private var current: AudioTrack? = null

    private val _amplitudes = MutableStateFlow(FloatArray(BAR_COUNT))
    val amplitudes: StateFlow<FloatArray> = _amplitudes.asStateFlow()

    /** Voice Detail entry point — synth + play, untuned. */
    suspend fun play(voiceId: String, text: String, sid: Int = 0) = withContext(dispatchers.default) {
        stop()
        val output = runCatching { runtime.synthesize(voiceId, text, sid = sid) }
            .onFailure { log.w(it) { "Preview synth failed for $voiceId" } }
            .getOrNull() ?: return@withContext
        playSamples(output.samples, output.sampleRate)
    }

    /** Playground entry point — synth with [tuning], caller plays via [playSamples] after persisting. */
    suspend fun synthesizeTuned(
        voiceId: String,
        text: String,
        sid: Int,
        tuning: VoiceTuning,
    ): SherpaTtsRuntime.SynthesisOutput? = withContext(dispatchers.default) {
        runCatching {
            runtime.synthesize(
                voiceId = voiceId, text = text, sid = sid,
                speed = tuning.speed, pitch = tuning.pitch, lengthScale = tuning.lengthScale,
                noiseScale = tuning.noiseScale, noiseScaleW = tuning.noiseScaleW,
            )
        }.onFailure { log.w(it) { "Tuned synth failed for $voiceId" } }.getOrNull()
    }

    /** Pure playback for raw FloatArray. Used by playground generate + replay paths. */
    suspend fun playSamples(samples: FloatArray, sampleRate: Int) = withContext(dispatchers.default) {
        stop()
        if (samples.isEmpty() || sampleRate <= 0) return@withContext
        val pcm = floatToPcm16(samples)
        val track = AudioTrack.Builder()
            .setAudioAttributes(AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build())
            .setAudioFormat(AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(sampleRate)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build())
            .setBufferSizeInBytes(pcm.size.coerceAtLeast(MIN_BUFFER_BYTES))
            .setTransferMode(AudioTrack.MODE_STREAM)
            .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
            .build()

        current = track
        track.setVolume(AudioTrack.getMaxVolume())
        track.play()
        runCatching {
            track.write(pcm, 0, pcm.size)
            val frames = pcm.size / 2
            val playbackMs = (frames.toLong() * 1000L / sampleRate)
            publishAmplitudeWindows(samples = samples, sampleRate = sampleRate, playbackMs = playbackMs)
        }
        stop()
    }

    fun stop() {
        _amplitudes.value = FloatArray(BAR_COUNT)
        val toRelease = current ?: return
        current = null
        runCatching { toRelease.stop() }
        runCatching { toRelease.release() }
    }

    private fun publishAmplitudeWindows(samples: FloatArray, sampleRate: Int, playbackMs: Long) {
        val windowMs = WINDOW_MS
        val windowSamples = ((sampleRate.toLong() * windowMs / 1000L).toInt()).coerceAtLeast(1)
        val totalWindows = (samples.size + windowSamples - 1) / windowSamples
        var cursor = 0
        val tail = FloatArray(BAR_COUNT)
        while (cursor < totalWindows && current != null) {
            for (i in 0 until BAR_COUNT - 1) tail[i] = tail[i + 1]
            tail[BAR_COUNT - 1] = rmsOfWindow(samples, cursor * windowSamples, windowSamples)
            _amplitudes.value = tail.copyOf()
            cursor++
            Thread.sleep(windowMs)
        }
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
        private const val LEGACY_MODE = AudioManager.STREAM_MUSIC
        private const val BAR_COUNT = 32
        private const val WINDOW_MS = 60L
    }
}
