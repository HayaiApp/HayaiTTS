package dev.ahmedmohamed.hayaitts.ui.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.ahmedmohamed.hayaitts.data.telemetry.SynthesisTelemetryRepository
import dev.ahmedmohamed.hayaitts.domain.model.DownloadState
import dev.ahmedmohamed.hayaitts.domain.repo.CatalogRepository
import dev.ahmedmohamed.hayaitts.domain.repo.DownloadRepository
import dev.ahmedmohamed.hayaitts.domain.repo.VoiceRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * One-stop ViewModel for the Activity tab. Joins downloads, telemetry, and
 * installed-voice cache stats so each pane can rely on a single state
 * snapshot.
 */
class ActivityViewModel(
    private val downloads: DownloadRepository,
    private val voices: VoiceRepository,
    private val catalog: CatalogRepository,
    private val telemetry: SynthesisTelemetryRepository,
) : ViewModel() {

    data class DownloadRow(
        val voiceId: String,
        val title: String,
        val state: DownloadState,
    )

    data class UiState(
        val downloads: List<DownloadRow> = emptyList(),
        val telemetry: List<SynthesisTelemetryRepository.Event> = emptyList(),
        val cacheBytes: Long = 0L,
        val installedCount: Int = 0,
    )

    val uiState: StateFlow<UiState> = combine(
        downloads.states,
        voices.installed,
        telemetry.recent,
    ) { states, installed, recent ->
        val rows = states.map { (voiceId, state) ->
            val title = installed.firstOrNull { it.voiceId == voiceId }?.title
                ?: catalog.catalog.value.firstOrNull { it.id == voiceId }?.title
                ?: voiceId
            DownloadRow(voiceId = voiceId, title = title, state = state)
        }
        val cacheBytes = installed.sumOf { it.sizeBytesOnDisk() }
        UiState(
            downloads = rows,
            telemetry = recent,
            cacheBytes = cacheBytes,
            installedCount = installed.size,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = UiState(),
    )

    /** Best-effort: voices.installed only exposes a path; reading the dir size
     *  off the ViewModel thread is acceptable because the file count is small. */
    private fun dev.ahmedmohamed.hayaitts.domain.model.InstalledVoice.sizeBytesOnDisk(): Long {
        val dir = java.io.File(installedPath)
        if (!dir.exists()) return 0L
        return dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }
}
