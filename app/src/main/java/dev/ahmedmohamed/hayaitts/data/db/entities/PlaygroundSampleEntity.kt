package dev.ahmedmohamed.hayaitts.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One generated playground sample. PCM bytes live on disk under
 * `cacheDir/playground/<voiceId>/<id>.pcm`; only metadata + path here.
 * Added in DB schema v3.
 */
@Entity(tableName = "playground_samples")
data class PlaygroundSampleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val voiceId: String,
    val text: String,
    val sid: Int,
    val speed: Float,
    val pitch: Float,
    val lengthScale: Float,
    val pcmPath: String,
    val sampleRate: Int,
    val createdAt: Long,
)
