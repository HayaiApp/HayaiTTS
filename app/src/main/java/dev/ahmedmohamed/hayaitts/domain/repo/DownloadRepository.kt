package dev.ahmedmohamed.hayaitts.domain.repo

import dev.ahmedmohamed.hayaitts.domain.model.DownloadState
import dev.ahmedmohamed.hayaitts.domain.model.VoiceCard
import kotlinx.coroutines.flow.Flow

/**
 * Voice download lifecycle. The flow [states] maps voiceId -> [DownloadState];
 * a voice missing from the map has never been enqueued (treat as
 * [DownloadState.Idle]).
 */
interface DownloadRepository {
    val states: Flow<Map<String, DownloadState>>
    fun enqueue(voiceCard: VoiceCard)
    fun cancel(voiceId: String)

    /**
     * Wipe a single tracked download row. The Downloads Manager screen calls
     * this when the user picks "Remove from history" on a completed or failed
     * entry. No effect on an active download.
     */
    suspend fun clearOne(voiceId: String)

    /**
     * Drop every non-active row (Done / Failed / Cancelled). Active rows
     * (Queued / Running / Extracting) are left alone so a "Clear completed"
     * tap during an ongoing download does not orphan the worker.
     */
    suspend fun clearCompleted()
}
