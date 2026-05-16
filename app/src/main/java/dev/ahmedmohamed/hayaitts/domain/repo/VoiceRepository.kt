package dev.ahmedmohamed.hayaitts.domain.repo

import dev.ahmedmohamed.hayaitts.domain.model.InstalledVoice
import dev.ahmedmohamed.hayaitts.domain.model.VoiceCard
import kotlinx.coroutines.flow.Flow

/**
 * CRUD over the installed-voices Room table plus the in-APK bundled voice.
 * The flow [installed] always emits the bundled voice as its first element so
 * callers do not need to special-case it.
 */
interface VoiceRepository {
    val installed: Flow<List<InstalledVoice>>
    suspend fun isInstalled(voiceId: String): Boolean
    suspend fun markInstalled(voice: VoiceCard, path: String)
    suspend fun uninstall(voiceId: String)
}
