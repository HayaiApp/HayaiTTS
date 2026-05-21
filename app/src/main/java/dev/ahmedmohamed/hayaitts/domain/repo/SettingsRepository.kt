package dev.ahmedmohamed.hayaitts.domain.repo

import dev.ahmedmohamed.hayaitts.domain.model.StorageLocation
import dev.ahmedmohamed.hayaitts.domain.model.UpdateChannel
import kotlinx.coroutines.flow.Flow

/**
 * DataStore-backed user preferences that the downloader and runtime react to.
 */
interface SettingsRepository {
    val wifiOnly: Flow<Boolean>
    suspend fun setWifiOnly(value: Boolean)

    val storageLocation: Flow<StorageLocation>
    suspend fun setStorageLocation(value: StorageLocation)

    /** Release channel the auto-updater polls. Defaults to STABLE. */
    val updateChannel: Flow<UpdateChannel>
    suspend fun setUpdateChannel(value: UpdateChannel)

    /**
     * Epoch millis of the last successful (non-failed) update check. `0` until
     * the first check completes. Stored in DataStore so the "Last checked X
     * min ago" line in Settings survives process restarts.
     */
    val lastUpdateCheckMillis: Flow<Long>
    suspend fun setLastUpdateCheckMillis(value: Long)

    val useNnapi: Flow<Boolean>
    suspend fun setUseNnapi(value: Boolean)

    val synthesisThreads: Flow<Int>
    suspend fun setSynthesisThreads(value: Int)

    val maxNumSentences: Flow<Int>
    suspend fun setMaxNumSentences(value: Int)

    /**
     * Per-voice default speaker id. Multi-speaker voices use this when the
     * framework calls onSynthesizeText without an explicit speaker, and the
     * Playground / VoiceDetail screens read it to highlight the current pick.
     * Keys are voice ids, values are sherpa-onnx speaker indices.
     */
    val defaultSpeakerByVoice: Flow<Map<String, Int>>
    suspend fun setDefaultSpeaker(voiceId: String, speakerId: Int)
    suspend fun clearDefaultSpeaker(voiceId: String)
}
