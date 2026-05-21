package dev.ahmedmohamed.hayaitts.data.preview

import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.max
import kotlin.math.min

/**
 * Writes 16-bit signed PCM WAV files (RIFF/WAVE, format code 1).
 *
 * The Studio "Export to WAV" action synthesizes a FloatArray via the gateway,
 * then hands it here with the voice's sample rate. The output goes through
 * an [OutputStream] supplied by the caller (typically a SAF
 * `contentResolver.openOutputStream(uri)`).
 */
object WavExporter {

    /**
     * Writes [samples] as a mono 16-bit PCM WAV at [sampleRate] Hz to [out].
     * Closes [out] when done.
     */
    fun write(samples: FloatArray, sampleRate: Int, out: OutputStream) {
        val numFrames = samples.size
        val byteRate = sampleRate * 2  // mono, 16-bit
        val dataSize = numFrames * 2
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray())
            putInt(36 + dataSize)
            put("WAVE".toByteArray())
            put("fmt ".toByteArray())
            putInt(16)                // PCM chunk size
            putShort(1)               // PCM format code
            putShort(1)               // channels
            putInt(sampleRate)
            putInt(byteRate)
            putShort(2)               // block align (2 bytes mono)
            putShort(16)              // bits per sample
            put("data".toByteArray())
            putInt(dataSize)
        }
        out.write(header.array())

        val buf = ByteBuffer.allocate(numFrames * 2).order(ByteOrder.LITTLE_ENDIAN)
        for (sample in samples) {
            val clamped = max(-1f, min(1f, sample))
            buf.putShort((clamped * Short.MAX_VALUE).toInt().toShort())
        }
        out.write(buf.array())
        out.flush()
        out.close()
    }
}
