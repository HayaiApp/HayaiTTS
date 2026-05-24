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
    /**
     * SHA-256 of the [vocoderUrl] asset. Verified alongside the main bundle
     * when present; null obeys the same trust policy as [sha256] (see
     * [VoiceDownloadWorker] for the rules).
     */
    val vocoderSha256: String? = null,
    /**
     * Set to `true` by [dev.ahmedmohamed.hayaitts.data.catalog.CatalogRepositoryImpl]
     * when the catalog was loaded from the remote `raw.githubusercontent.com`
     * URL rather than the bundled APK asset. The download worker enforces a
     * stricter integrity policy on remote-sourced entries: a null [sha256] is
     * a hard error because the bytes weren't shipped with the (signed) APK.
     * Default is `false` so deserialized JSON (which never sets this field)
     * defaults to the trusted-asset code path until the repository marks it.
     */
    val fromRemote: Boolean = false,
    /**
     * Optional URL to a hosted demo (e.g. a Hugging Face Space) where the
     * user can hear this voice **before downloading**. Voice Detail surfaces
     * this as a "Try in browser" link button. Null for voices with no public
     * demo (most non-marketed Piper voices).
     */
    val demoUrl: String? = null,
    /**
     * Canonical sample URL (sid=0, primary language). Preserved for
     * back-compat with older catalog rows that predate [speakerSamples].
     * When [speakerSamples] is non-empty this typically points at the same
     * file as `speakerSamples[0]`.
     */
    val sampleAudioUrl: String? = null,
    /**
     * Per-(speaker, language) audition clips. One entry per combination the
     * offline renderer produced — multi-speaker voices ship one per speaker,
     * multi-lang voices ship one per language, multi-speaker × multi-lang
     * voices (Kokoro multi-lang) ship the cartesian product.
     */
    @SerialName("speakerSamples")
    val speakerSamples: List<VoiceSample> = emptyList(),
) {
    val modelFamily: ModelFamily get() = ModelFamily.fromCatalog(family)
    val tierEnum: Tier get() = Tier.fromCatalog(tier)

    /**
     * Best-match sample URL for the given (speaker, language). Resolution
     * order: exact match → speaker-only match → language-only match → first
     * available [speakerSamples] entry → [sampleAudioUrl] fallback.
     */
    fun sampleFor(sid: Int, language: String?): String? {
        if (speakerSamples.isEmpty()) return sampleAudioUrl
        speakerSamples.firstOrNull { it.speakerId == sid && it.language == language }?.let { return it.url }
        speakerSamples.firstOrNull { it.speakerId == sid }?.let { return it.url }
        if (language != null) {
            speakerSamples.firstOrNull { it.language == language }?.let { return it.url }
        }
        return speakerSamples.firstOrNull()?.url ?: sampleAudioUrl
    }
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
