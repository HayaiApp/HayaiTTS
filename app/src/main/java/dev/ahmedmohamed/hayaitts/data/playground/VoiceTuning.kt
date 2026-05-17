package dev.ahmedmohamed.hayaitts.data.playground

/**
 * Per-voice synthesis knobs persisted in DataStore (see [VoiceTuningRepository]).
 *
 * - [speed] forwards straight to OfflineTts.generate(text, sid, speed).
 * - [pitch] is a post-process resample (no native pitch knob in sherpa-onnx).
 * - [lengthScale] is baked into the engine config at construction time, so
 *   distinct values create distinct LRU cache entries in SherpaTtsRuntime.
 */
data class VoiceTuning(
    val speed: Float = 1.0f,
    val pitch: Float = 1.0f,
    val lengthScale: Float = 1.0f,
) {
    companion object {
        val Default = VoiceTuning()
        const val SPEED_MIN = 0.5f
        const val SPEED_MAX = 2.0f
        const val PITCH_MIN = 0.5f
        const val PITCH_MAX = 1.5f
        const val LENGTH_MIN = 0.7f
        const val LENGTH_MAX = 1.5f
    }
}
