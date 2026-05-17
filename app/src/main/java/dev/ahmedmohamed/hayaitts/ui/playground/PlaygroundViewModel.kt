package dev.ahmedmohamed.hayaitts.ui.playground

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.ahmedmohamed.hayaitts.data.db.entities.PlaygroundSampleEntity
import dev.ahmedmohamed.hayaitts.data.playground.SampleHistoryRepository
import dev.ahmedmohamed.hayaitts.data.playground.VoiceTuning
import dev.ahmedmohamed.hayaitts.data.playground.VoiceTuningRepository
import dev.ahmedmohamed.hayaitts.data.preview.VoicePreviewPlayer
import dev.ahmedmohamed.hayaitts.domain.model.InstalledVoice
import dev.ahmedmohamed.hayaitts.domain.model.Speaker
import dev.ahmedmohamed.hayaitts.domain.model.VoiceCard
import dev.ahmedmohamed.hayaitts.domain.repo.CatalogRepository
import dev.ahmedmohamed.hayaitts.domain.repo.VoiceRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * State holder for the Model Playground. See [PlaygroundScreen] for layout.
 */
class PlaygroundViewModel(
    private val voiceId: String,
    catalogRepository: CatalogRepository,
    voiceRepository: VoiceRepository,
    private val tuningRepo: VoiceTuningRepository,
    private val historyRepo: SampleHistoryRepository,
    private val previewPlayer: VoicePreviewPlayer,
) : ViewModel() {

    data class UiState(
        val card: VoiceCard? = null,
        val installed: InstalledVoice? = null,
        val tuning: VoiceTuning = VoiceTuning.Default,
        val history: List<PlaygroundSampleEntity> = emptyList(),
        val text: String = "",
        val selectedSid: Int = 0,
        val playing: Boolean = false,
        val generating: Boolean = false,
        val error: String? = null,
    ) {
        val speakers: List<Speaker> get() = installed?.speakers ?: card?.speakers.orEmpty()
        val title: String get() = installed?.title ?: card?.title ?: "—"
        val isInstalled: Boolean get() = installed != null
    }

    val previewAmplitudes: StateFlow<FloatArray> = previewPlayer.amplitudes

    private val textFlow = MutableStateFlow("")
    private val selectedSidFlow = MutableStateFlow(0)
    private val playingFlow = MutableStateFlow(false)
    private val generatingFlow = MutableStateFlow(false)
    private val errorFlow = MutableStateFlow<String?>(null)

    private var playbackJob: Job? = null

    val uiState: StateFlow<UiState> = combine(
        catalogRepository.catalog,
        voiceRepository.installed,
        tuningRepo.tuningFor(voiceId),
        historyRepo.observe(voiceId),
        combine(textFlow, selectedSidFlow, playingFlow, generatingFlow, errorFlow) { t, s, p, g, e ->
            Quint(t, s, p, g, e)
        },
    ) { catalog, installed, tuning, history, ephem ->
        UiState(
            card = catalog.firstOrNull { it.id == voiceId },
            installed = installed.firstOrNull { it.voiceId == voiceId },
            tuning = tuning,
            history = history,
            text = ephem.text,
            selectedSid = ephem.sid,
            playing = ephem.playing,
            generating = ephem.generating,
            error = ephem.error,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), UiState())

    fun onTextChange(value: String) {
        textFlow.value = if (value.length <= MAX_INPUT) value else value.take(MAX_INPUT)
    }

    fun setSpeaker(sid: Int) { selectedSidFlow.value = sid }

    fun setSpeed(value: Float) = setTuning(uiState.value.tuning.copy(speed = value))
    fun setPitch(value: Float) = setTuning(uiState.value.tuning.copy(pitch = value))
    fun setLengthScale(value: Float) = setTuning(uiState.value.tuning.copy(lengthScale = value))
    fun resetSpeed() = setTuning(uiState.value.tuning.copy(speed = 1f))
    fun resetPitch() = setTuning(uiState.value.tuning.copy(pitch = 1f))
    fun resetLengthScale() = setTuning(uiState.value.tuning.copy(lengthScale = 1f))

    private fun setTuning(tuning: VoiceTuning) {
        viewModelScope.launch { tuningRepo.setTuning(voiceId, tuning) }
    }

    fun generate() {
        val current = uiState.value
        if (current.generating || !current.isInstalled) return
        val text = current.text.trim()
        if (text.isEmpty()) return
        playbackJob?.cancel()
        generatingFlow.value = true
        errorFlow.value = null
        playbackJob = viewModelScope.launch {
            try {
                val output = previewPlayer.synthesizeTuned(
                    voiceId = voiceId, text = text, sid = current.selectedSid, tuning = current.tuning,
                )
                if (output == null) {
                    errorFlow.value = "Synthesis failed"
                    return@launch
                }
                historyRepo.record(
                    voiceId = voiceId, text = text, sid = current.selectedSid,
                    tuning = current.tuning, samples = output.samples, sampleRate = output.sampleRate,
                )
                generatingFlow.value = false
                playingFlow.value = true
                previewPlayer.playSamples(output.samples, output.sampleRate)
            } catch (t: Throwable) {
                errorFlow.value = t.message ?: t.javaClass.simpleName
            } finally {
                generatingFlow.value = false
                playingFlow.value = false
            }
        }
    }

    fun replay(entry: PlaygroundSampleEntity) {
        playbackJob?.cancel()
        playbackJob = viewModelScope.launch {
            val samples = historyRepo.readSamples(entry)
            if (samples == null) {
                errorFlow.value = "Sample file is missing"
                return@launch
            }
            playingFlow.value = true
            try {
                previewPlayer.playSamples(samples, entry.sampleRate)
            } finally {
                playingFlow.value = false
            }
        }
    }

    fun deleteSample(entry: PlaygroundSampleEntity) {
        viewModelScope.launch { historyRepo.delete(entry) }
    }

    fun stop() {
        playbackJob?.cancel()
        playbackJob = null
        playingFlow.value = false
        generatingFlow.value = false
        previewPlayer.stop()
    }

    fun dismissError() { errorFlow.value = null }

    override fun onCleared() { super.onCleared(); stop() }

    private data class Quint(
        val text: String,
        val sid: Int,
        val playing: Boolean,
        val generating: Boolean,
        val error: String?,
    )

    companion object { const val MAX_INPUT = 1000 }
}
