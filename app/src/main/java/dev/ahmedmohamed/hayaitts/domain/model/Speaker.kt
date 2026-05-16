package dev.ahmedmohamed.hayaitts.domain.model

import kotlinx.serialization.Serializable

/**
 * One speaker inside a multi-speaker voice bundle. Most Piper voices have a
 * single speaker; we still model it as a list so multi-speaker LibriTTS-R-style
 * checkpoints can be added later without a schema change.
 */
@Serializable
data class Speaker(
    val id: Int,
    val name: String,
    val gender: String,
)
