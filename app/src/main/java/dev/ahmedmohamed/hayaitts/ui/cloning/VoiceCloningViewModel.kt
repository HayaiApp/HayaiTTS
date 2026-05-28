package dev.ahmedmohamed.hayaitts.ui.cloning

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import dev.ahmedmohamed.hayaitts.core.dispatchers.DispatcherProvider
import dev.ahmedmohamed.hayaitts.data.cloning.MicReferenceRecorder
import dev.ahmedmohamed.hayaitts.data.cloning.ReferenceClip
import dev.ahmedmohamed.hayaitts.data.cloning.decodeReferenceClip
import dev.ahmedmohamed.hayaitts.data.preview.VoicePreviewPlayer
import dev.ahmedmohamed.hayaitts.tts.SherpaTtsRuntime
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * State + actions for [VoiceCloningScreen]. Owns:
 *
 *   - the in-memory [ReferenceClip] (mic-captured or file-picked),
 *   - the reference transcript and target text fields,
 *   - the running mic recorder, if any,
 *   - the synthesised output and a one-shot play button.
 *
 * The screen is reachable only from voices whose [family.supportsCloning]
 * is true; the VM is constructed with the target voiceId via Koin's
 * `parametersOf(voiceId)`.
 */
class VoiceCloningViewModel(
    private val context: Context,
    private val voiceId: String,
    private val dispatchers: DispatcherProvider,
    private val previewPlayer: VoicePreviewPlayer,
) : ViewModel() {

    private val log = Logger.withTag("VoiceCloningVM")
    private val recorder = MicReferenceRecorder(dispatchers)

    data class UiState(
        val referenceClip: ReferenceClip? = null,
        val referenceText: String = "",
        val targetText: String = "",
        val recording: Boolean = false,
        val recordingAmplitude: Float = 0f,
        val generating: Boolean = false,
        val playing: Boolean = false,
        val error: String? = null,
    ) {
        val canGenerate: Boolean
            get() = !generating &&
                referenceClip != null &&
                referenceText.isNotBlank() &&
                targetText.isNotBlank()
    }

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private var recordJob: Job? = null
    private var amplitudeJob: Job? = null

    init {
        amplitudeJob = viewModelScope.launch {
            recorder.amplitudes.collect { rms ->
                _state.update { it.copy(recordingAmplitude = rms) }
            }
        }
    }

    fun setReferenceText(value: String) {
        _state.update { it.copy(referenceText = value) }
    }

    fun setTargetText(value: String) {
        _state.update { it.copy(targetText = value) }
    }

    fun dismissError() {
        _state.update { it.copy(error = null) }
    }

    /**
     * Start mic capture. The screen UI is responsible for the RECORD_AUDIO
     * runtime permission; this method assumes it has been granted.
     */
    fun startRecording() {
        if (_state.value.recording) return
        _state.update { it.copy(recording = true, error = null) }
        recordJob = viewModelScope.launch {
            try {
                val clip = recorder.record()
                _state.update {
                    it.copy(referenceClip = clip, recording = false)
                }
            } catch (t: Throwable) {
                log.e(t) { "Mic recording failed" }
                _state.update {
                    it.copy(recording = false, error = t.message ?: "Recording failed")
                }
            }
        }
    }

    fun stopRecording() {
        recorder.stop()
    }

    /**
     * Decode an arbitrary audio file picked via SAF. Heavy work runs on
     * the IO dispatcher; the resulting [ReferenceClip] lives in memory.
     */
    fun loadReferenceFile(uri: Uri) {
        viewModelScope.launch {
            try {
                val clip = withContext(dispatchers.io) {
                    decodeReferenceClip(context, uri, dispatchers)
                }
                _state.update { it.copy(referenceClip = clip, error = null) }
            } catch (t: Throwable) {
                log.e(t) { "Reference file decode failed for $uri" }
                _state.update {
                    it.copy(error = t.message ?: "Could not read audio file")
                }
            }
        }
    }

    fun clearReference() {
        _state.update { it.copy(referenceClip = null) }
    }

    /**
     * Run the cloning synthesis. Reads the current reference clip + texts
     * out of the state at call time so the user's last keystroke is
     * always included.
     */
    fun generate() {
        val current = _state.value
        if (!current.canGenerate) return
        val clip = current.referenceClip ?: return
        _state.update { it.copy(generating = true, error = null) }
        viewModelScope.launch {
            try {
                val output = withContext(dispatchers.default) {
                    SherpaTtsRuntime.get(context).synthesizeCloned(
                        voiceId = voiceId,
                        text = current.targetText.trim(),
                        referenceAudio = clip.samples,
                        referenceSampleRate = clip.sampleRate,
                        referenceText = current.referenceText.trim(),
                    )
                }
                _state.update { it.copy(generating = false, playing = true) }
                previewPlayer.playSamples(output.samples, output.sampleRate)
                _state.update { it.copy(playing = false) }
            } catch (t: Throwable) {
                log.e(t) { "Cloning synthesis failed" }
                _state.update {
                    it.copy(
                        generating = false,
                        playing = false,
                        error = t.message ?: "Synthesis failed",
                    )
                }
            }
        }
    }

    override fun onCleared() {
        recordJob?.cancel()
        amplitudeJob?.cancel()
        previewPlayer.stop()
    }
}
