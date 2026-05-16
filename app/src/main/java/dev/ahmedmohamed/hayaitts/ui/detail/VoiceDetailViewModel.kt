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
 * player gets cleanly released on screen exit ([onCleared]).
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
    ) {
        val isInstalled: Boolean get() = installed != null
        fun defaultedLocales(): Set<String> =
            defaults.filterValues { it == installed?.voiceId }.keys
    }

    private var previewJob: Job? = null

    val uiState: StateFlow<UiState> = combine(
        catalogRepository.catalog,
        voiceRepository.installed,
        downloadRepository.states,
        defaultsRepository.defaults,
    ) { catalog, installed, downloads, defaults ->
        UiState(
            card = catalog.firstOrNull { it.id == voiceId },
            installed = installed.firstOrNull { it.voiceId == voiceId },
            downloadState = downloads[voiceId] ?: DownloadState.Idle,
            defaults = defaults,
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

    fun play(text: String) {
        if (!uiState.value.isInstalled) return
        previewJob?.cancel()
        previewJob = viewModelScope.launch {
            try {
                previewPlayer.play(voiceId, text)
            } finally {
                previewJob = null
            }
        }
    }

    fun stop() {
        previewJob?.cancel()
        previewJob = null
        previewPlayer.stop()
    }

    override fun onCleared() {
        super.onCleared()
        stop()
    }
}
