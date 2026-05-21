package dev.ahmedmohamed.hayaitts.data.tts

import android.content.Context
import dev.ahmedmohamed.hayaitts.core.dispatchers.DispatcherProvider
import dev.ahmedmohamed.hayaitts.domain.usecase.SynthesisGateway
import dev.ahmedmohamed.hayaitts.domain.usecase.SynthesizeUseCase
import dev.ahmedmohamed.hayaitts.tts.SherpaTtsRuntime
import kotlinx.coroutines.withContext

/**
 * Concrete [SynthesisGateway] that thunks into [SherpaTtsRuntime]. Lives in
 * `data/` because the runtime is an Android-bound JNI wrapper.
 */
class SherpaSynthesisGateway(
    private val context: Context,
    private val dispatchers: DispatcherProvider,
) : SynthesisGateway {

    private val runtime: SherpaTtsRuntime get() = SherpaTtsRuntime.get(context)

    override suspend fun synthesize(request: SynthesizeUseCase.Request): SynthesizeUseCase.Output =
        withContext(dispatchers.default) {
            val out = runtime.synthesize(
                voiceId = request.voiceId,
                text = request.text,
                sid = request.sid,
                speed = request.speed,
                pitch = request.pitch,
                lengthScale = request.lengthScale,
                noiseScale = request.noiseScale,
                noiseScaleW = request.noiseScaleW,
            )
            SynthesizeUseCase.Output(sampleRate = out.sampleRate, samples = out.samples)
        }
}
