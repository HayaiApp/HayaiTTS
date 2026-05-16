package dev.ahmedmohamed.hayaitts.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One row per voice the user has successfully downloaded. The bundled
 * Piper Amy voice is NOT stored here — it lives only in assets and is
 * stitched in by [dev.ahmedmohamed.hayaitts.data.voices.VoiceRepositoryImpl].
 *
 * Speakers are persisted as a JSON-encoded `List<Speaker>` string to avoid a
 * second table; the list is small (typically 1 element) and we only need to
 * read it as a unit.
 */
@Entity(tableName = "installed_voices")
data class InstalledVoiceEntity(
    @PrimaryKey val voiceId: String,
    val family: String,
    val title: String,
    /** Comma-separated BCP-47 tags, e.g. `en-US,en-CA`. */
    val languages: String,
    /** kotlinx.serialization-encoded `List<Speaker>`. */
    val speakersJson: String,
    val sampleRateHz: Int,
    val installedPath: String,
    val tier: String,
    val installedAt: Long,
)
