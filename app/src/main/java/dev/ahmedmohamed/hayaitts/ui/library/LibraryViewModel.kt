package dev.ahmedmohamed.hayaitts.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.ahmedmohamed.hayaitts.domain.model.InstalledVoice
import dev.ahmedmohamed.hayaitts.domain.repo.CatalogRepository
import dev.ahmedmohamed.hayaitts.domain.repo.DefaultsRepository
import dev.ahmedmohamed.hayaitts.domain.repo.DownloadRepository
import dev.ahmedmohamed.hayaitts.domain.repo.VoiceRepository
import dev.ahmedmohamed.hayaitts.ui.library.preferences.LibraryUiPreferences
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Backing state for the Library tab: the installed-voice list plus the
 * per-locale defaults map, the user-chosen reorder, and the favorites set.
 *
 * The defaults map is exposed as a `Map<voiceId, Set<locale>>` so the card can
 * cheaply check `defaults[voice.voiceId]` for chip rendering.
 *
 * Sort policy:
 *  1. Favorites first (within their language group).
 *  2. Inside each group, user-chosen order from [LibraryUiPreferences].
 *  3. Anything not yet in the saved order falls back to install order.
 */
class LibraryViewModel(
    private val voiceRepository: VoiceRepository,
    private val downloadRepository: DownloadRepository,
    private val catalogRepository: CatalogRepository,
    private val defaultsRepository: DefaultsRepository,
    private val uiPreferences: LibraryUiPreferences,
) : ViewModel() {

    data class UiState(
        val installed: List<InstalledVoice> = emptyList(),
        /** locale (BCP-47) -> voiceId currently set as default. */
        val defaults: Map<String, String> = emptyMap(),
        val favorites: Set<String> = emptySet(),
        val voiceOrder: List<String> = emptyList(),
    ) {
        /** Set of locales where this voice is the active default. */
        fun defaultedLocales(voiceId: String): Set<String> =
            defaults.filterValues { it == voiceId }.keys

        /**
         * Final display ordering. Stable, favorites-first, with user reorder
         * acting as the tier-break. Used by the Library list AND by the
         * Featured carousel (it just slices the first 5).
         */
        val orderedInstalled: List<InstalledVoice>
            get() {
                val rank: (String) -> Int = { id ->
                    val idx = voiceOrder.indexOf(id)
                    if (idx >= 0) idx else Int.MAX_VALUE / 2 + installed.indexOfFirst { it.voiceId == id }
                }
                return installed.sortedWith(
                    compareByDescending<InstalledVoice> { it.voiceId in favorites }
                        .thenBy { rank(it.voiceId) },
                )
            }
    }

    val uiState: StateFlow<UiState> = combine(
        voiceRepository.installed,
        defaultsRepository.defaults,
        uiPreferences.favoriteVoices,
        uiPreferences.voiceOrder,
    ) { installed, defaults, favorites, order ->
        UiState(installed = installed, defaults = defaults, favorites = favorites, voiceOrder = order)
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

    fun toggleFavorite(voiceId: String) {
        viewModelScope.launch { uiPreferences.toggleFavorite(voiceId) }
    }

    /**
     * Persist a new ordering for the installed voices. The reorder UI sends
     * the post-drag list of voiceIds; we just write it through.
     */
    fun saveVoiceOrder(orderedIds: List<String>) {
        viewModelScope.launch { uiPreferences.setVoiceOrder(orderedIds) }
    }
}
