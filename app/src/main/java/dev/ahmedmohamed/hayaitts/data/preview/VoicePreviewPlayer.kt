package dev.ahmedmohamed.hayaitts.data.preview

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import co.touchlab.kermit.Logger
import dev.ahmedmohamed.hayaitts.tts.SherpaTtsRuntime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Synthesizes a short preview sentence on the requested voice and streams the
 * resulting PCM through a one-shot [AudioTrack]. The track is released as soon
 * as playback completes (or [stop] is called) so we never accumulate native
 * handles across previews.
 *
 * sherpa-onnx returns audio as `FloatArray` in [-1.0, 1.0] — we convert to
 * 16-bit signed little-endian PCM here because [AudioFormat.ENCODING_PCM_16BIT]
 * is the only encoding guaranteed across every API level the app supports.
 */
class VoicePreviewPlayer(private val context: Context) {

    private val log = Logger.withTag("VoicePreview")
    private val runtime: SherpaTtsRuntime get() = SherpaTtsRuntime.get(context)

    @Volatile
    private var current: AudioTrack? = null

    /** Blocks until synth + playback finishes. Cancel-friendly via [stop]. */
    suspend fun play(voiceId: String, text: String) = withContext(Dispatchers.Default) {
        stop()
        val output = runCatching { runtime.synthesize(voiceId, text) }
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
            // Add 50ms of jitter so the buffer fully empties before we stop.
            Thread.sleep(playbackMs + 50)
        }
        stop()
    }

    /** Releases the currently playing track, if any. Idempotent. */
    fun stop() {
        val toRelease = current ?: return
        current = null
        runCatching { toRelease.stop() }
        runCatching { toRelease.release() }
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
    }
}
