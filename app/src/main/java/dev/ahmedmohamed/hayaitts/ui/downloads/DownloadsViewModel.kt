package dev.ahmedmohamed.hayaitts.ui.downloads

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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Backing state for the Downloads Manager screen.
 */
class DownloadsViewModel(
    private val downloadRepository: DownloadRepository,
    private val catalogRepository: CatalogRepository,
    private val voiceRepository: VoiceRepository,
    private val history: DownloadsHistory,
) : ViewModel() {

    data class UiState(
        val active: List<Row> = emptyList(),
        val failed: List<Row> = emptyList(),
        val completed: List<Row> = emptyList(),
    ) {
        val isEmpty: Boolean
            get() = active.isEmpty() && failed.isEmpty() && completed.isEmpty()
        val activeCount: Int get() = active.size
    }

    data class Row(
        val voiceId: String,
        val title: String,
        val voiceCard: VoiceCard?,
        val state: DownloadState,
        val completedAtMillis: Long?,
        val installed: Boolean,
    )

    val uiState: StateFlow<UiState> = combine(
        downloadRepository.states,
        catalogRepository.catalog,
        voiceRepository.installed,
        history.entries,
    ) { states, catalog, installed, historyEntries ->
        buildState(states, catalog, installed, historyEntries)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), UiState())

    init {
        downloadRepository.states
            .map { snapshot -> snapshot.filterValues { it is DownloadState.Done }.keys }
            .distinctUntilChanged()
            .onEach { doneIds -> doneIds.forEach { id -> history.recordCompletion(id) } }
            .launchIn(viewModelScope)
    }

    private fun buildState(
        states: Map<String, DownloadState>,
        catalog: List<VoiceCard>,
        installed: List<InstalledVoice>,
        historyEntries: List<DownloadsHistory.Entry>,
    ): UiState {
        val catalogIndex = catalog.associateBy { it.id }
        val installedIds = installed.map { it.voiceId }.toSet()
        val active = mutableListOf<Row>()
        val failed = mutableListOf<Row>()
        val completed = mutableListOf<Row>()
        states.forEach { (voiceId, state) ->
            val card = catalogIndex[voiceId]
            val row = Row(
                voiceId = voiceId,
                title = card?.title ?: voiceId,
                voiceCard = card,
                state = state,
                completedAtMillis = historyEntries.firstOrNull { it.voiceId == voiceId }?.completedAtMillis,
                installed = voiceId in installedIds,
            )
            when (state) {
                is DownloadState.Queued,
                is DownloadState.Running,
                is DownloadState.Extracting -> active += row
                is DownloadState.Failed -> failed += row
                is DownloadState.Done -> completed += row
                is DownloadState.Cancelled -> failed += row
                DownloadState.Idle -> Unit
            }
        }
        val liveCompletedIds = completed.map { it.voiceId }.toSet()
        historyEntries.forEach { entry ->
            if (entry.voiceId in liveCompletedIds) return@forEach
            val card = catalogIndex[entry.voiceId]
            completed += Row(
                voiceId = entry.voiceId,
                title = card?.title ?: entry.voiceId,
                voiceCard = card,
                state = DownloadState.Done,
                completedAtMillis = entry.completedAtMillis,
                installed = entry.voiceId in installedIds,
            )
        }
        return UiState(
            active = active.sortedByDescending { activeRank(it.state) },
            failed = failed.sortedByDescending { it.completedAtMillis ?: 0L },
            completed = completed.sortedByDescending { it.completedAtMillis ?: 0L }.take(20),
        )
    }

    private fun activeRank(state: DownloadState): Int = when (state) {
        is DownloadState.Running -> 3
        is DownloadState.Extracting -> 2
        DownloadState.Queued -> 1
        else -> 0
    }

    fun cancel(voiceId: String) { downloadRepository.cancel(voiceId) }
    fun retry(voiceCard: VoiceCard) { downloadRepository.enqueue(voiceCard) }
    fun retry(voiceId: String) {
        val card = catalogRepository.catalog.value.firstOrNull { it.id == voiceId } ?: return
        downloadRepository.enqueue(card)
    }
    fun removeFromHistory(voiceId: String) {
        viewModelScope.launch {
            history.remove(voiceId)
            downloadRepository.clearOne(voiceId)
        }
    }
    fun clearCompleted() {
        viewModelScope.launch { downloadRepository.clearCompleted() }
    }
}
