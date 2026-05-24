package dev.ahmedmohamed.hayaitts.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * One pre-rendered MP3 clip representing how a specific (speaker, language)
 * combination on a [VoiceCard] sounds. Populated by the offline
 * `tools/samples/render_samples.py` pipeline; consumed by the in-app
 * audition button so users can hear a voice before committing to the
 * multi-MB model download.
 *
 * `speakerId` matches [Speaker.id]; `language` is the BCP47 tag the clip was
 * rendered with (so a Kokoro multi-lang voice ships one [VoiceSample] per
 * (sid, language) pair).
 */
@Serializable
data class VoiceSample(
    @SerialName("speakerId") val speakerId: Int,
    @SerialName("language") val language: String,
    @SerialName("url") val url: String,
)
