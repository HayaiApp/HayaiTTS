package dev.ahmedmohamed.hayaitts.ui.browse

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.ahmedmohamed.hayaitts.domain.model.DownloadState
import dev.ahmedmohamed.hayaitts.domain.model.InstalledVoice
import dev.ahmedmohamed.hayaitts.domain.model.ModelFamily
import dev.ahmedmohamed.hayaitts.domain.model.Tier
import dev.ahmedmohamed.hayaitts.domain.model.VoiceCard
import dev.ahmedmohamed.hayaitts.domain.recommendation.recommendedTier
import dev.ahmedmohamed.hayaitts.domain.repo.CatalogRepository
import dev.ahmedmohamed.hayaitts.domain.repo.DownloadRepository
import dev.ahmedmohamed.hayaitts.domain.repo.VoiceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/**
 * Drives the Browse screen. Holds the active filter state (lang / tier /
 * family / search) and joins it against the catalog, the installed list, and
 * the download-state map to produce a single [BrowseUiState].
 *
 * Filter semantics:
 *  - languages: empty = "any", non-empty = OR-match against `card.languages`.
 *  - tier:      null = "any", otherwise exact.
 *  - families:  empty = "any", non-empty = OR-match against `card.modelFamily`.
 *  - search:    case-insensitive substring on `card.title`.
 *
 * Sort: installed-first, then alphabetical by title.
 */
class BrowseViewModel(
    context: Context,
    private val catalogRepository: CatalogRepository,
    private val voiceRepository: VoiceRepository,
    private val downloadRepository: DownloadRepository,
) : ViewModel() {

    /**
     * Cached at VM construction so the Browse sort doesn't repeatedly call
     * into ActivityManager. The recommendation never changes during a session.
     */
    private val recommended: Tier = recommendedTier(context)

    data class Filters(
        val languages: Set<String> = emptySet(),
        val tier: Tier? = null,
        val families: Set<ModelFamily> = emptySet(),
        /**
         * Selected speaker genders. Values are normalised lower-case tokens
         * ("f", "m", "unknown") — see [normaliseGender] for the mapping from
         * raw catalog strings. Empty set = no filter.
         */
        val genders: Set<String> = emptySet(),
        val query: String = "",
    ) {
        val activeCount: Int get() {
            var n = 0
            if (languages.isNotEmpty()) n++
            if (tier != null) n++
            if (families.isNotEmpty()) n++
            if (genders.isNotEmpty()) n++
            if (query.isNotBlank()) n++
            return n
        }
    }

    data class BrowseUiState(
        val cards: List<VoiceCard> = emptyList(),
        val installedIds: Set<String> = emptySet(),
        val downloads: Map<String, DownloadState> = emptyMap(),
        val availableLanguages: List<String> = emptyList(),
        val availableFamilies: List<ModelFamily> = emptyList(),
        val availableGenders: List<String> = emptyList(),
        val filters: Filters = Filters(),
        /**
         * Voice tier the device is rated for. Browse sorts cards matching
         * this tier first and surfaces a "Recommended" pill on them.
         */
        val recommendedTier: Tier = Tier.MID,
    )

    private val filters = MutableStateFlow(Filters())

    val uiState: StateFlow<BrowseUiState> = combine(
        catalogRepository.catalog,
        voiceRepository.installed,
        downloadRepository.states,
        filters,
    ) { catalog, installed, downloads, f ->
        val installedIds = installed.mapTo(mutableSetOf()) { it.voiceId }
        val filtered = catalog.applyFilters(f)
        val sorted = filtered.sortedWith(
            // Installed > recommended-tier > alphabetical. The
            // recommended-tier tier-break keeps the user's existing voices
            // pinned to the top and steers new picks toward the right
            // perf bucket for their device.
            compareByDescending<VoiceCard> { it.id in installedIds }
                .thenByDescending { it.tierEnum == recommended }
                .thenBy { it.title.lowercase() },
        )
        BrowseUiState(
            cards = sorted,
            installedIds = installedIds,
            downloads = downloads,
            availableLanguages = catalog.flatMap { it.languages }.distinct().sorted(),
            availableFamilies = catalog.map { it.modelFamily }.distinct(),
            availableGenders = catalog
                .flatMap { card -> card.speakers.map { normaliseGender(it.gender) } }
                .distinct()
                // Stable order: F, M, then unknown last.
                .sortedBy { g -> when (g) { "f" -> 0; "m" -> 1; else -> 2 } },
            filters = f,
            recommendedTier = recommended,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), BrowseUiState())

    fun setQuery(q: String) = filters.update { it.copy(query = q) }
    fun setTier(t: Tier?) = filters.update { it.copy(tier = t) }
    fun toggleLanguage(tag: String) = filters.update {
        val next = it.languages.toMutableSet()
        if (!next.add(tag)) next.remove(tag)
        it.copy(languages = next)
    }
    fun toggleFamily(fam: ModelFamily) = filters.update {
        val next = it.families.toMutableSet()
        if (!next.add(fam)) next.remove(fam)
        it.copy(families = next)
    }
    fun toggleGender(gender: String) = filters.update {
        val normalised = normaliseGender(gender)
        val next = it.genders.toMutableSet()
        if (!next.add(normalised)) next.remove(normalised)
        it.copy(genders = next)
    }
    fun clearLanguages() = filters.update { it.copy(languages = emptySet()) }
    fun clearFamilies() = filters.update { it.copy(families = emptySet()) }
    fun clearAllFilters() = filters.update { Filters() }

    fun enqueue(card: VoiceCard) {
        // Defensive: the UI also disables Install for unavailable cards but a
        // direct call from e.g. a future deep link should not silently kick
        // off a 700 MB download for a voice the engine cannot synthesise.
        if (!card.available) return
        downloadRepository.enqueue(card)
    }
    fun cancel(voiceId: String) = downloadRepository.cancel(voiceId)

    private fun List<VoiceCard>.applyFilters(f: Filters): List<VoiceCard> = filter { card ->
        (f.languages.isEmpty() || card.languages.any { it in f.languages }) &&
            (f.tier == null || card.tierEnum == f.tier) &&
            (f.families.isEmpty() || card.modelFamily in f.families) &&
            (f.genders.isEmpty() ||
                card.speakers.any { normaliseGender(it.gender) in f.genders }) &&
            (f.query.isBlank() || card.title.contains(f.query.trim(), ignoreCase = true))
    }

    /**
     * Catalog speaker `gender` strings are inconsistent across sources — some
     * use single-letter codes ("F"/"M"), some use full words, some are blank
     * or "unknown". Collapse them onto a stable 3-bucket token so the filter
     * compares cleanly.
     */
    private fun normaliseGender(raw: String): String = when (raw.trim().lowercase()) {
        "f", "female" -> "f"
        "m", "male" -> "m"
        else -> "unknown"
    }

    @Suppress("unused")
    private fun pickInstalled(installed: List<InstalledVoice>, id: String): Boolean =
        installed.any { it.voiceId == id }
}
