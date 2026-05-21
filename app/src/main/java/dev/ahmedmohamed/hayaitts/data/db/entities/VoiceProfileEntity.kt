package dev.ahmedmohamed.hayaitts.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One saved synthesis preset for a voice. Stored as a row keyed by an
 * autoincrement id so multiple profiles per voice are possible.
 *
 * When [isDefaultForVoice] is true the row is the profile [HayaiTtsService]
 * applies when speaking that voice; only one row per voiceId should carry
 * the flag (enforced at write time).
 */
@Entity(tableName = "voice_profiles")
data class VoiceProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val voiceId: String,
    val name: String,
    val sid: Int = 0,
    val speed: Float = 1.0f,
    val pitch: Float = 1.0f,
    val lengthScale: Float = 1.0f,
    val noiseScale: Float = 0.667f,
    val noiseScaleW: Float = 0.8f,
    val ssmlEnabled: Boolean = false,
    val isDefaultForVoice: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)
