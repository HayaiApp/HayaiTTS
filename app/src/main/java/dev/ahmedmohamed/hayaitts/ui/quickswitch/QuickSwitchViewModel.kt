package dev.ahmedmohamed.hayaitts.ui.quickswitch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.ahmedmohamed.hayaitts.domain.model.InstalledVoice
import dev.ahmedmohamed.hayaitts.domain.repo.DefaultsRepository
import dev.ahmedmohamed.hayaitts.domain.repo.VoiceRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Backs the global voice-switcher bottom sheet. Groups installed voices by
 * language so the picker can render one collapsible section per locale, and
 * surfaces the currently-default voiceId so the UI can put a check next to it.
 *
 * Tapping a voice writes [DefaultsRepository.setDefault] for every language tag
 * the voice claims — the next TTS request that resolves a default for one of
 * those locales picks up the new mapping immediately. The runtime's voice
 * cache is per-voiceId so there's nothing to "swap" — synthesizing with the
 * new id is enough.
 */
class QuickSwitchViewModel(
    private val voiceRepository: VoiceRepository,
    private val defaultsRepository: DefaultsRepository,
) : ViewModel() {

    data class UiState(
        val installed: List<InstalledVoice> = emptyList(),
        val defaults: Map<String, String> = emptyMap(),
    ) {
        /** Grouped by primary language tag (first entry in `voice.languages`). */
        val grouped: List<Pair<String, List<InstalledVoice>>>
            get() = installed.groupBy { it.languages.firstOrNull() ?: "—" }
                .toList()
                .sortedBy { (locale, _) -> locale }

        /** True if [voiceId] is the default for any locale right now. */
        fun isDefaultedSomewhere(voiceId: String): Boolean =
            defaults.values.any { it == voiceId }
    }

    val uiState: StateFlow<UiState> = combine(
        voiceRepository.installed,
        defaultsRepository.defaults,
    ) { installed, defaults ->
        UiState(installed = installed, defaults = defaults)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), UiState())

    /**
     * Sets [voice] as default for every language tag it supports. We do all
     * locales the voice claims because the quick-switcher is meant to be a
     * one-tap "use this voice everywhere it can speak" affordance.
     */
    fun pickVoice(voice: InstalledVoice) {
        viewModelScope.launch {
            voice.languages.forEach { locale ->
                defaultsRepository.setDefault(locale, voice.voiceId)
            }
        }
    }
}
