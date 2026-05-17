package dev.ahmedmohamed.hayaitts.data.playground

import android.content.Context
import co.touchlab.kermit.Logger
import dev.ahmedmohamed.hayaitts.data.db.dao.PlaygroundSampleDao
import dev.ahmedmohamed.hayaitts.data.db.entities.PlaygroundSampleEntity
import kotlinx.coroutines.flow.Flow
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

class SampleHistoryRepository(
    private val context: Context,
    private val dao: PlaygroundSampleDao,
) {
    private val log = Logger.withTag("SampleHistory")

    fun observe(voiceId: String): Flow<List<PlaygroundSampleEntity>> = dao.observeByVoice(voiceId)

    suspend fun record(
        voiceId: String,
        text: String,
        sid: Int,
        tuning: VoiceTuning,
        samples: FloatArray,
        sampleRate: Int,
    ): PlaygroundSampleEntity {
        val dir = playgroundDir(voiceId).also { it.mkdirs() }
        val timestamp = System.currentTimeMillis()
        val file = File(dir, "${'$'}timestamp.pcm")
        writePcm16(file, samples)
        val row = PlaygroundSampleEntity(
            voiceId = voiceId, text = text, sid = sid,
            speed = tuning.speed, pitch = tuning.pitch, lengthScale = tuning.lengthScale,
            pcmPath = file.absolutePath, sampleRate = sampleRate, createdAt = timestamp,
        )
        val id = dao.insert(row)
        evictOverflow(voiceId)
        return row.copy(id = id)
    }

    suspend fun delete(entry: PlaygroundSampleEntity) {
        dao.deleteById(entry.id)
        runCatching { File(entry.pcmPath).delete() }
    }

    fun readSamples(entry: PlaygroundSampleEntity): FloatArray? {
        val f = File(entry.pcmPath)
        if (!f.isFile) {
            log.w { "Missing PCM for sample ${'$'}{entry.id} at ${'$'}{entry.pcmPath}" }
            return null
        }
        val bytes = runCatching { f.readBytes() }.getOrNull() ?: return null
        val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        val out = FloatArray(bytes.size / 2)
        var i = 0
        while (buf.remaining() >= 2) out[i++] = buf.short.toFloat() / Short.MAX_VALUE
        return out
    }

    private suspend fun evictOverflow(voiceId: String) {
        val overflow = dao.overflowFor(voiceId, MAX_PER_VOICE)
        if (overflow.isEmpty()) return
        dao.trim(voiceId, MAX_PER_VOICE)
        overflow.forEach { row -> runCatching { File(row.pcmPath).delete() } }
        log.i { "Evicted ${'$'}{overflow.size} overflow samples for ${'$'}voiceId" }
    }

    private fun writePcm16(file: File, samples: FloatArray) {
        val buf = ByteBuffer.allocate(samples.size * 2).order(ByteOrder.LITTLE_ENDIAN)
        for (s in samples) {
            val clamped = (s.coerceIn(-1f, 1f) * Short.MAX_VALUE).toInt().toShort()
            buf.putShort(clamped)
        }
        file.writeBytes(buf.array())
    }

    private fun playgroundDir(voiceId: String): File =
        File(File(context.cacheDir, "playground"), voiceId.sanitize())

    private fun String.sanitize(): String = replace(Regex("[^A-Za-z0-9_.-]"), "_")

    companion object {
        const val MAX_PER_VOICE = 10
    }
}
