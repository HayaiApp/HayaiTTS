package dev.ahmedmohamed.hayaitts.domain.model

/**
 * Neural TTS model family supported by sherpa-onnx. Phase 4a only wires up
 * [PIPER] end-to-end; the other entries are listed so callers can persist
 * future families without an upgrade migration.
 */
enum class ModelFamily {
    PIPER,
    KOKORO,
    VITS,
    MATCHA,
    KITTEN,
    CUSTOM;

    companion object {
        fun fromCatalog(raw: String): ModelFamily = when (raw.lowercase()) {
            "piper" -> PIPER
            "kokoro" -> KOKORO
            "vits" -> VITS
            "matcha" -> MATCHA
            "kitten" -> KITTEN
            else -> CUSTOM
        }
    }
}
