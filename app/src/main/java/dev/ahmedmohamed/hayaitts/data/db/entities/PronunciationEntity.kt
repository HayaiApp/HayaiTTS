package dev.ahmedmohamed.hayaitts.data.db.entities

import androidx.room.Entity

/**
 * Per-locale homograph / pronunciation override. The TTS service applies
 * these as a pre-synthesis text rewrite before any SSML preprocessing.
 *
 * The primary key is composite — same `word` can have different replacements
 * per locale.
 */
@Entity(
    tableName = "pronunciations",
    primaryKeys = ["locale", "word"],
)
data class PronunciationEntity(
    val locale: String,
    val word: String,
    val replacement: String,
    val createdAt: Long = System.currentTimeMillis(),
)
