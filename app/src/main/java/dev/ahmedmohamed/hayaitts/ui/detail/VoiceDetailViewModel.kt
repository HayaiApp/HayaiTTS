package dev.ahmedmohamed.hayaitts.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.ahmedmohamed.hayaitts.data.preview.VoicePreviewPlayer
import dev.ahmedmohamed.hayaitts.domain.model.DownloadState
import dev.ahmedmohamed.hayaitts.domain.model.InstalledVoice
import dev.ahmedmohamed.hayaitts.domain.model.VoiceCard
import dev.ahmedmohamed.hayaitts.domain.repo.CatalogRepository
import dev.ahmedmohamed.hayaitts.domain.repo.DefaultsRepository
import dev.ahmedmohamed.hayaitts.domain.repo.DownloadRepository
import dev.ahmedmohamed.hayaitts.domain.repo.VoiceRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Voice Detail screen state holder. Stitches the catalog entry, the installed
 * record, the current download state, and the default-locale map together so
 * the screen can render the right action button without doing its own
 * cross-flow joins.
 *
 * Preview playback is launched from [play] and tracked via [previewJob] so the
 * player gets cleanly released on screen exit ([onCleared]). The currently
 * selected speaker id is tracked in [selectedSid] so the speaker chooser can
 * swap sids without reloading the underlying voice.
 *
 * The waveform widget consumes [previewAmplitudes] directly — same StateFlow
 * the player publishes RMS bins into during playback.
 */
class VoiceDetailViewModel(
    private val voiceId: String,
    private val catalogRepository: CatalogRepository,
    private val voiceRepository: VoiceRepository,
    private val downloadRepository: DownloadRepository,
    private val defaultsRepository: DefaultsRepository,
    private val previewPlayer: VoicePreviewPlayer,
) : ViewModel() {

    data class UiState(
        val card: VoiceCard? = null,
        val installed: InstalledVoice? = null,
        val downloadState: DownloadState = DownloadState.Idle,
        val defaults: Map<String, String> = emptyMap(),
        val previewing: Boolean = false,
        /**
         * `true` from the moment the user taps Play until the runtime
         * returns audio (synthesis only, no playback). Mirrors the
         * Playground/Studio "generating" affordance so multi-speaker
         * Kokoros that take ~3 s to synthesise don't look hung.
         */
        val generating: Boolean = false,
        val selectedSid: Int = 0,
    ) {
        val isInstalled: Boolean get() = installed != null
        fun defaultedLocales(): Set<String> =
            defaults.filterValues { it == installed?.voiceId }.keys
    }

    private var previewJob: Job? = null

    private val selectedSidFlow = MutableStateFlow(0)
    private val previewingFlow = MutableStateFlow(false)
    private val generatingFlow = MutableStateFlow(false)

    val previewAmplitudes: StateFlow<FloatArray> = previewPlayer.amplitudes

    val uiState: StateFlow<UiState> = combine(
        catalogRepository.catalog,
        voiceRepository.installed,
        downloadRepository.states,
        defaultsRepository.defaults,
        selectedSidFlow,
        previewingFlow,
        generatingFlow,
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val catalog = values[0] as List<VoiceCard>
        @Suppress("UNCHECKED_CAST")
        val installed = values[1] as List<InstalledVoice>
        @Suppress("UNCHECKED_CAST")
        val downloads = values[2] as Map<String, DownloadState>
        @Suppress("UNCHECKED_CAST")
        val defaults = values[3] as Map<String, String>
        val sid = values[4] as Int
        val previewing = values[5] as Boolean
        val generating = values[6] as Boolean
        UiState(
            card = catalog.firstOrNull { it.id == voiceId },
            installed = installed.firstOrNull { it.voiceId == voiceId },
            downloadState = downloads[voiceId] ?: DownloadState.Idle,
            defaults = defaults,
            previewing = previewing,
            generating = generating,
            selectedSid = sid,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), UiState())

    fun install() {
        val card = uiState.value.card ?: return
        if (!card.available) return
        downloadRepository.enqueue(card)
    }

    fun cancel() = downloadRepository.cancel(voiceId)

    fun uninstall() {
        viewModelScope.launch { voiceRepository.uninstall(voiceId) }
    }

    fun setDefault(locale: String) {
        viewModelScope.launch {
            val current = uiState.value.defaults[locale]
            if (current == voiceId) {
                defaultsRepository.clearDefault(locale)
            } else {
                defaultsRepository.setDefault(locale, voiceId)
            }
        }
    }

    fun setSpeaker(sid: Int) {
        selectedSidFlow.value = sid
    }

    fun play(text: String) {
        if (!uiState.value.isInstalled) return
        previewJob?.cancel()
        // generating == true until the runtime returns audio; then it
        // flips to previewing == true for the playback. The UI uses
        // both flags to label the button (Generating… → Stop) and to
        // gate the waveform animation (only animates during playback).
        generatingFlow.value = true
        previewingFlow.value = false
        previewJob = viewModelScope.launch {
            try {
                val output = previewPlayer.synthesizeTuned(
                    voiceId = voiceId,
                    text = text,
                    sid = uiState.value.selectedSid,
                    tuning = dev.ahmedmohamed.hayaitts.data.playground.VoiceTuning.Default,
                )
                generatingFlow.value = false
                if (output == null) return@launch
                previewingFlow.value = true
                previewPlayer.playSamples(output.samples, output.sampleRate)
            } finally {
                generatingFlow.value = false
                previewingFlow.value = false
                previewJob = null
            }
        }
    }

    fun stop() {
        previewJob?.cancel()
        previewJob = null
        previewingFlow.value = false
        generatingFlow.value = false
        previewPlayer.stop()
    }

    override fun onCleared() {
        super.onCleared()
        stop()
    }
}
