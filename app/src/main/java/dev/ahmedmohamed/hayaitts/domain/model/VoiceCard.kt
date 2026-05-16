package dev.ahmedmohamed.hayaitts.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Catalog entry for one downloadable voice. Shape matches `catalog/v1/models.json`
 * exactly so we can [kotlinx.serialization.json.Json.decodeFromString] straight
 * into a list of these from either the bundled asset or the remote URL.
 */
@Serializable
data class VoiceCard(
    val id: String,
    val family: String,
    val title: String,
    val languages: List<String>,
    val speakers: List<Speaker>,
    val sampleRateHz: Int,
    val approxSizeMb: Int,
    val tier: String,
    val license: String,
    val bundleUrl: String,
    val sha256: String? = null,
) {
    val modelFamily: ModelFamily get() = ModelFamily.fromCatalog(family)
    val tierEnum: Tier get() = Tier.fromCatalog(tier)
}

/**
 * Top-level wrapper for `catalog/v1/models.json` — only used for parsing.
 */
@Serializable
data class CatalogManifest(
    val version: Int,
    val voices: List<VoiceCard>,
    @SerialName("updatedAt") val updatedAt: String? = null,
)
