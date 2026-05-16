package dev.ahmedmohamed.hayaitts.domain.repo

import kotlinx.coroutines.flow.Flow

/**
 * Per-locale default voice mapping. Phase 4a does not yet read these inside
 * [dev.ahmedmohamed.hayaitts.tts.HayaiTtsService]; Phase 4b's voice picker UI
 * will write them, and the resolver lookup will happen in Phase 5.
 */
interface DefaultsRepository {
    /** Flow of `locale (BCP-47)` -> `voiceId`. */
    val defaults: Flow<Map<String, String>>
    suspend fun setDefault(locale: String, voiceId: String)
    suspend fun clearDefault(locale: String)
}
