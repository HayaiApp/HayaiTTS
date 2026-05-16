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
    /**
     * `true` when the current build of sherpa-onnx can actually load and
     * synthesise this voice. We set it to `false` for Kokoro and Kitten —
     * lib-sherpa-onnx 6.25.12 ships no JNI for those families (confirmed by
     * grepping the .so for `Java_com_k2fsa_sherpa_onnx_OfflineTts*` exports
     * and for the string "Kokoro" / "Kitten"; both came back empty). The
     * catalog still lists them so the Browse screen can surface them as
     * "Coming soon" — Phase 7+ may upgrade the JNI dependency.
     */
    val available: Boolean = true,
    /**
     * Optional secondary asset (matcha-tts vocoder) downloaded alongside the
     * main bundle and dropped into the voice directory as [vocoderFileName].
     * Null for every other family.
     */
    val vocoderUrl: String? = null,
    /** Filename to write [vocoderUrl] to inside the unpacked voice dir. */
    val vocoderFileName: String? = null,
    /**
     * Override the ONNX weight filename when the bundle does not use
     * `model.onnx` (e.g. VCTK ships `vits-vctk.onnx`, Matcha ships
     * `model-steps-3.onnx`). Resolved relative to the unpacked voice dir.
     */
    val modelFileName: String? = null,
    /** Optional lexicon file present in some VITS / Matcha bundles. */
    val lexiconFileName: String? = null,
    /** Optional dict directory (Chinese VITS / Matcha use jieba dicts). */
    val dictDirName: String? = null,
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
