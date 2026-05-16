package dev.ahmedmohamed.hayaitts.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Maps a BCP-47 locale tag (e.g. `en-US`) to the [voiceId] that should be used
 * when the framework requests that locale via [android.speech.tts.TextToSpeech.setLanguage].
 */
@Entity(tableName = "default_voices")
data class DefaultVoiceEntity(
    @PrimaryKey val locale: String,
    val voiceId: String,
)
