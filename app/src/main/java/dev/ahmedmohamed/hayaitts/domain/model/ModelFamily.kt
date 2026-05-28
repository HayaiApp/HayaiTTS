package dev.ahmedmohamed.hayaitts.domain.model

/**
 * Neural TTS model family supported by sherpa-onnx 1.13.2's JNI surface.
 * `SherpaTtsRuntime` builds an `OfflineTtsConfig` per family; the catalog
 * generator's `RUNTIME_SUPPORTED_FAMILIES` set must mirror these enum
 * entries (minus CUSTOM) so the Browse screen never shows "Coming Soon"
 * for a family the runtime can actually play.
 */
enum class ModelFamily {
    PIPER,
    KOKORO,
    VITS,
    MATCHA,
    KITTEN,
    ZIPVOICE,
    POCKET,
    SUPERTONIC,
    CUSTOM;

    /**
     * `true` when the family's sherpa-onnx config accepts a reference audio
     * clip + transcript via [com.k2fsa.sherpa.onnx.GenerationConfig] so the
     * runtime can synthesise *in the cloned voice*. Only the flow-matching
     * families ZipVoice and Pocket implement this in lib-sherpa-onnx 1.13.2.
     */
    val supportsCloning: Boolean get() = this == ZIPVOICE || this == POCKET

    /**
     * Every sherpa-onnx family exposes the JNI `generateWithCallback` hook
     * which streams audio chunks as they're produced, so streaming is a
     * runtime feature, not a per-voice one. Kept as a property here so
     * future families that don't (e.g. a hypothetical pre-batched neural
     * codec) can override.
     */
    val supportsStreaming: Boolean get() = this != CUSTOM

    companion object {
        fun fromCatalog(raw: String): ModelFamily = when (raw.lowercase()) {
            "piper" -> PIPER
            "kokoro" -> KOKORO
            "vits" -> VITS
            "matcha" -> MATCHA
            "kitten" -> KITTEN
            "zipvoice" -> ZIPVOICE
            "pocket" -> POCKET
            "supertonic" -> SUPERTONIC
            else -> CUSTOM
        }
    }
}
