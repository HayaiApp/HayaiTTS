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
