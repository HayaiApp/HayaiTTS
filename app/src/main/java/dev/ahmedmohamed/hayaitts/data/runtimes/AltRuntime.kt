package dev.ahmedmohamed.hayaitts.data.runtimes

import dev.ahmedmohamed.hayaitts.domain.model.ModelFamily

/**
 * Pluggable runtime for non-sherpa-onnx model families. Reserved seam for
 * future F5-TTS / OuteTTS / MeloTTS / Style2 integrations — none ship in
 * v2.0.0-b1, but the registry below ensures the dispatcher in
 * [SherpaSynthesisGateway] can fall through cleanly when sherpa-onnx says
 * "I don't speak this family".
 *
 * Concrete implementations register at app startup via [AltRuntimes.register].
 * Hidden behind a Power-user Settings toggle so users opt in to experimental
 * runtimes.
 */
interface AltRuntime {
    val family: ModelFamily
    suspend fun synthesize(
        voiceId: String,
        text: String,
        sid: Int,
        speed: Float,
        pitch: Float,
    ): Output

    data class Output(val sampleRate: Int, val samples: FloatArray) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Output) return false
            return sampleRate == other.sampleRate && samples.contentEquals(other.samples)
        }
        override fun hashCode(): Int = 31 * sampleRate + samples.contentHashCode()
    }
}

/**
 * Process-wide registry. Reads are lock-free; registration happens once at
 * boot from the app DI module so contention is not a concern.
 */
object AltRuntimes {
    private val byFamily = mutableMapOf<ModelFamily, AltRuntime>()

    fun register(runtime: AltRuntime) {
        byFamily[runtime.family] = runtime
    }

    fun lookup(family: ModelFamily): AltRuntime? = byFamily[family]

    fun families(): Set<ModelFamily> = byFamily.keys
}
