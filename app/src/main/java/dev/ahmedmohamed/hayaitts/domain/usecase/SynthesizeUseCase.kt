package dev.ahmedmohamed.hayaitts.domain.usecase

import dev.ahmedmohamed.hayaitts.core.result.AppError
import dev.ahmedmohamed.hayaitts.core.result.Outcome
import dev.ahmedmohamed.hayaitts.core.result.asFailure
import dev.ahmedmohamed.hayaitts.core.result.asSuccess

/**
 * Domain-side orchestrator for one synthesis call. v2 plumbs profile lookup,
 * pronunciation overrides, and SSML preprocessing through here before
 * handing the cleaned text to the sherpa runtime.
 *
 * For v2.0.0-b1 the body is a thin pass-through over [SynthesisGateway] —
 * subsequent commits inside the same branch will layer in the additional
 * concerns. Keeping the seam in place now means `HayaiTtsService` and the
 * Studio screen call the same entry point.
 */
class SynthesizeUseCase(
    private val gateway: SynthesisGateway,
) {
    data class Output(val sampleRate: Int, val samples: FloatArray) {
        // Auto-generated equals/hashCode would compare the FloatArray by reference;
        // override so test-doubles can compare by content if they need to.
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Output) return false
            return sampleRate == other.sampleRate && samples.contentEquals(other.samples)
        }
        override fun hashCode(): Int = 31 * sampleRate + samples.contentHashCode()
    }

    data class Request(
        val voiceId: String,
        val text: String,
        val sid: Int = 0,
        val speed: Float = 1.0f,
        val pitch: Float = 1.0f,
        val lengthScale: Float = 1.0f,
        val noiseScale: Float = 0.667f,
        val noiseScaleW: Float = 0.8f,
    )

    suspend operator fun invoke(request: Request): Outcome<Output> {
        if (request.text.isBlank()) {
            return AppError.Validation("text", "must not be blank").asFailure()
        }
        return try {
            val result = gateway.synthesize(request)
            result.asSuccess()
        } catch (t: kotlinx.coroutines.CancellationException) {
            throw t
        } catch (t: Throwable) {
            AppError.Runtime("synthesis").asFailure(t)
        }
    }
}

/**
 * Lowest-layer abstraction over the sherpa-onnx engine that the use case
 * depends on. Concrete impl lives in `data/` (or `tts/`) and adapts
 * [SherpaTtsRuntime]. Keeps the use case Android-free.
 */
interface SynthesisGateway {
    suspend fun synthesize(request: SynthesizeUseCase.Request): SynthesizeUseCase.Output
}
