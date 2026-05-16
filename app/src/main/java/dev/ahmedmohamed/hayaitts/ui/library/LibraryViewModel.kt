package dev.ahmedmohamed.hayaitts.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.ahmedmohamed.hayaitts.domain.model.InstalledVoice
import dev.ahmedmohamed.hayaitts.domain.repo.CatalogRepository
import dev.ahmedmohamed.hayaitts.domain.repo.DefaultsRepository
import dev.ahmedmohamed.hayaitts.domain.repo.DownloadRepository
import dev.ahmedmohamed.hayaitts.domain.repo.VoiceRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Backing state for the Library tab: the installed-voice list plus the
 * per-locale defaults map. Phase 4a's smoke-test enqueue/cancel helpers are
 * gone — installation now goes through Browse + Voice Detail.
 *
 * The defaults map is exposed as a `Map<voiceId, Set<locale>>` so the card can
 * cheaply check `defaults[voice.voiceId]` for chip rendering.
 */
class LibraryViewModel(
    private val voiceRepository: VoiceRepository,
    private val downloadRepository: DownloadRepository,
    private val catalogRepository: CatalogRepository,
    private val defaultsRepository: DefaultsRepository,
) : ViewModel() {

    data class UiState(
        val installed: List<InstalledVoice> = emptyList(),
        /** locale (BCP-47) -> voiceId currently set as default. */
        val defaults: Map<String, String> = emptyMap(),
    ) {
        /** Set of locales where this voice is the active default. */
        fun defaultedLocales(voiceId: String): Set<String> =
            defaults.filterValues { it == voiceId }.keys
    }

    val uiState: StateFlow<UiState> = combine(
        voiceRepository.installed,
        defaultsRepository.defaults,
    ) { installed, defaults ->
        UiState(installed = installed, defaults = defaults)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), UiState())

    /** Toggle whether [voiceId] is the default for [locale]. */
    fun toggleDefault(locale: String, voiceId: String) {
        viewModelScope.launch {
            val current = uiState.value.defaults[locale]
            if (current == voiceId) {
                defaultsRepository.clearDefault(locale)
            } else {
                defaultsRepository.setDefault(locale, voiceId)
            }
        }
    }

    fun uninstall(voiceId: String) {
        viewModelScope.launch {
            voiceRepository.uninstall(voiceId)
        }
    }
}
