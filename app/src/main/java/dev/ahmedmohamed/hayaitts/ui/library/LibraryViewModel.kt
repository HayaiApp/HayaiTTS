package dev.ahmedmohamed.hayaitts.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.ahmedmohamed.hayaitts.domain.model.DownloadState
import dev.ahmedmohamed.hayaitts.domain.model.InstalledVoice
import dev.ahmedmohamed.hayaitts.domain.model.VoiceCard
import dev.ahmedmohamed.hayaitts.domain.repo.CatalogRepository
import dev.ahmedmohamed.hayaitts.domain.repo.DownloadRepository
import dev.ahmedmohamed.hayaitts.domain.repo.VoiceRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * ViewModel for the (still minimal) Library screen. Exposes the installed
 * list, the per-voice download state, and a one-shot enqueue helper for the
 * Phase 4a smoke-test button.
 *
 * Phase 4b will turn the smoke-test logic into the real Browse / Detail flow
 * — this ViewModel will then back the per-detail card directly.
 */
class LibraryViewModel(
    private val voiceRepository: VoiceRepository,
    private val downloadRepository: DownloadRepository,
    private val catalogRepository: CatalogRepository,
) : ViewModel() {

    data class UiState(
        val installed: List<InstalledVoice> = emptyList(),
        val catalog: List<VoiceCard> = emptyList(),
        val downloadStates: Map<String, DownloadState> = emptyMap(),
    )

    val uiState: StateFlow<UiState> = combine(
        voiceRepository.installed,
        catalogRepository.catalog,
        downloadRepository.states,
    ) { installed, catalog, downloads ->
        UiState(installed = installed, catalog = catalog, downloadStates = downloads)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), UiState())

    fun enqueueSmokeTest() {
        val card = uiState.value.catalog.firstOrNull { it.id == SMOKE_TEST_VOICE_ID }
            ?: return
        downloadRepository.enqueue(card)
    }

    fun cancelSmokeTest() {
        downloadRepository.cancel(SMOKE_TEST_VOICE_ID)
    }

    companion object {
        const val SMOKE_TEST_VOICE_ID = "vits-piper-en_GB-alan-low"
    }
}
