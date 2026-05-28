package dev.ahmedmohamed.hayaitts.data.cloning

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaRecorder
import android.net.Uri
import dev.ahmedmohamed.hayaitts.core.dispatchers.DispatcherProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Holds a mono float reference clip for the cloning runtime.
 *
 * [samples] is in the `[-1, 1]` range, [sampleRate] in Hz. Both runtime
 * cloning entry points (`OfflineTts.generateWithConfig` /
 * `…AndCallback`) accept the float array directly; we do not need to
 * resample to the model's preferred SR because sherpa-onnx handles
 * resampling internally as long as the source SR is reported truthfully.
 */
data class ReferenceClip(
    val samples: FloatArray,
    val sampleRate: Int,
    val durationMs: Long,
) {
    override fun equals(other: Any?): Boolean = this === other
    override fun hashCode(): Int = System.identityHashCode(this)
}

/**
 * Streams 16-bit PCM from the device microphone while the calling
 * coroutine collects [amplitudes], then returns the captured clip on
 * [stop]. Only one capture may be in flight at a time.
 *
 * `RECORD_AUDIO` must already be granted; the screen UI is responsible
 * for the permission flow before calling [start]. Failures are
 * raised as [IllegalStateException] so the caller can surface them via
 * the standard error snackbar pattern.
 */
class MicReferenceRecorder(private val dispatchers: DispatcherProvider) {

    private val _amplitudes = MutableSharedFlow<Float>(extraBufferCapacity = 32)
    val amplitudes: Flow<Float> = _amplitudes.asSharedFlow()

    @Volatile private var stopRequested = false
    @Volatile private var capturing = false

    /**
     * Record until [stop] is invoked or until [maxDurationMs] is reached
     * (whichever comes first). Throws if RECORD_AUDIO is not granted or
     * the underlying AudioRecord fails to initialise.
     */
    suspend fun record(maxDurationMs: Long = MAX_DURATION_MS): ReferenceClip =
        withContext(dispatchers.io) {
            check(!capturing) { "MicReferenceRecorder is already capturing" }
            capturing = true
            stopRequested = false
            try {
                val sampleRate = SAMPLE_RATE
                val bufSize = AudioRecord.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                ).coerceAtLeast(4096)
                @Suppress("MissingPermission") // gated by the UI permission flow
                val recorder = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufSize * 2,
                )
                check(recorder.state == AudioRecord.STATE_INITIALIZED) {
                    "AudioRecord could not initialise (state=${recorder.state})"
                }
                val pcm = ByteArrayOutputStream()
                val buf = ByteArray(bufSize)
                val start = System.currentTimeMillis()
                recorder.startRecording()
                while (!stopRequested) {
                    val read = recorder.read(buf, 0, buf.size)
                    if (read <= 0) break
                    pcm.write(buf, 0, read)
                    _amplitudes.tryEmit(rmsLevel(buf, read))
                    if (System.currentTimeMillis() - start >= maxDurationMs) break
                }
                recorder.stop()
                recorder.release()
                val durationMs = System.currentTimeMillis() - start
                ReferenceClip(
                    samples = pcm16ToFloats(pcm.toByteArray()),
                    sampleRate = sampleRate,
                    durationMs = durationMs,
                )
            } finally {
                capturing = false
            }
        }

    fun stop() { stopRequested = true }

    companion object {
        // 16 kHz mono is the lowest sherpa-onnx accepts for cloning
        // reference clips while still capturing enough of the speaker's
        // formants. Lower SRs degrade voice identity badly.
        const val SAMPLE_RATE = 16000
        const val MAX_DURATION_MS = 30_000L
    }
}

/**
 * Decode an arbitrary audio file (mp3, aac, ogg, wav, m4a, …) into a
 * mono float clip via Android's MediaExtractor + MediaCodec. The file
 * is picked through SAF so we get a content:// URI; we do not need
 * READ_EXTERNAL_STORAGE.
 */
suspend fun decodeReferenceClip(
    context: Context,
    uri: Uri,
    dispatchers: DispatcherProvider,
): ReferenceClip = withContext(dispatchers.io) {
    val extractor = MediaExtractor()
    context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { afd ->
        extractor.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
    } ?: throw IllegalStateException("Could not open reference clip at $uri")

    var trackIndex = -1
    var format: MediaFormat? = null
    for (i in 0 until extractor.trackCount) {
        val f = extractor.getTrackFormat(i)
        val mime = f.getString(MediaFormat.KEY_MIME) ?: continue
        if (mime.startsWith("audio/")) {
            trackIndex = i
            format = f
            break
        }
    }
    val mediaFormat = format
        ?: throw IllegalStateException("File has no audio track")
    extractor.selectTrack(trackIndex)

    val mime = mediaFormat.getString(MediaFormat.KEY_MIME)!!
    val sampleRate = mediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
    val channelCount = mediaFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
    val codec = MediaCodec.createDecoderByType(mime)
    codec.configure(mediaFormat, null, null, 0)
    codec.start()

    val pcm = ByteArrayOutputStream()
    val info = MediaCodec.BufferInfo()
    var inputDone = false
    var outputDone = false
    val timeoutUs = 10_000L
    while (!outputDone) {
        if (!inputDone) {
            val inputIdx = codec.dequeueInputBuffer(timeoutUs)
            if (inputIdx >= 0) {
                val inputBuf = codec.getInputBuffer(inputIdx)!!
                val read = extractor.readSampleData(inputBuf, 0)
                if (read < 0) {
                    codec.queueInputBuffer(
                        inputIdx, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM,
                    )
                    inputDone = true
                } else {
                    codec.queueInputBuffer(
                        inputIdx, 0, read, extractor.sampleTime, 0,
                    )
                    extractor.advance()
                }
            }
        }
        val outputIdx = codec.dequeueOutputBuffer(info, timeoutUs)
        if (outputIdx >= 0) {
            val outputBuf = codec.getOutputBuffer(outputIdx)!!
            val chunk = ByteArray(info.size)
            outputBuf.position(info.offset)
            outputBuf.get(chunk, 0, info.size)
            pcm.write(chunk)
            codec.releaseOutputBuffer(outputIdx, false)
            if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                outputDone = true
            }
        }
    }
    codec.stop()
    codec.release()
    extractor.release()

    val mono = if (channelCount == 1) {
        pcm16ToFloats(pcm.toByteArray())
    } else {
        // Downmix to mono — sherpa-onnx cloning expects single-channel.
        val interleaved = pcm16ToFloats(pcm.toByteArray())
        val out = FloatArray(interleaved.size / channelCount)
        for (i in out.indices) {
            var sum = 0f
            for (c in 0 until channelCount) {
                sum += interleaved[i * channelCount + c]
            }
            out[i] = sum / channelCount
        }
        out
    }
    val durationMs = (mono.size.toLong() * 1000L) / sampleRate.coerceAtLeast(1)
    ReferenceClip(samples = mono, sampleRate = sampleRate, durationMs = durationMs)
}

private fun pcm16ToFloats(bytes: ByteArray): FloatArray {
    val bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
    val out = FloatArray(bytes.size / 2)
    var i = 0
    while (bb.remaining() >= 2) {
        out[i++] = bb.short / Short.MAX_VALUE.toFloat()
    }
    return out
}

private fun rmsLevel(bytes: ByteArray, length: Int): Float {
    if (length < 2) return 0f
    val bb = ByteBuffer.wrap(bytes, 0, length).order(ByteOrder.LITTLE_ENDIAN)
    var sumSq = 0.0
    var n = 0
    while (bb.remaining() >= 2) {
        val s = bb.short / Short.MAX_VALUE.toFloat()
        sumSq += (s * s).toDouble()
        n++
    }
    if (n == 0) return 0f
    return kotlin.math.sqrt(sumSq / n).toFloat()
}
