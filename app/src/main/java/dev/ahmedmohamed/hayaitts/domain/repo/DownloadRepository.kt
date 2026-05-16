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
}
