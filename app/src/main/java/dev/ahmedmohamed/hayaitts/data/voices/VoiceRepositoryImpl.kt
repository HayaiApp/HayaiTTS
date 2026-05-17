package dev.ahmedmohamed.hayaitts.data.voices

import android.content.Context
import co.touchlab.kermit.Logger
import dev.ahmedmohamed.hayaitts.data.catalog.catalogJson
import dev.ahmedmohamed.hayaitts.data.db.dao.InstalledVoiceDao
import dev.ahmedmohamed.hayaitts.data.db.entities.InstalledVoiceEntity
import dev.ahmedmohamed.hayaitts.domain.model.InstalledVoice
import dev.ahmedmohamed.hayaitts.domain.model.ModelFamily
import dev.ahmedmohamed.hayaitts.domain.model.Speaker
import dev.ahmedmohamed.hayaitts.domain.model.Tier
import dev.ahmedmohamed.hayaitts.domain.model.VoiceCard
import dev.ahmedmohamed.hayaitts.domain.repo.VoiceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import java.io.File
import java.util.Locale

/**
 * Room-backed installed-voices repository. Every voice comes from a
 * downloader / custom-import write — nothing is bundled in the APK, so the
 * first-run flow is empty.
 */
class VoiceRepositoryImpl(
    @Suppress("UNUSED_PARAMETER") context: Context,
    private val dao: InstalledVoiceDao,
) : VoiceRepository {

    private val log = Logger.withTag("VoiceRepository")
    private val speakerListSerializer = ListSerializer(Speaker.serializer())

    override val installed: Flow<List<InstalledVoice>> = dao.getAll().map { rows ->
        rows.map { it.toDomain() }
    }

    override suspend fun isInstalled(voiceId: String): Boolean =
        dao.getById(voiceId) != null

    override suspend fun markInstalled(voice: VoiceCard, path: String) {
        upsertChecked(voice, path, effectiveFamily = null)
    }

    override suspend fun markInstalledCustom(
        voice: VoiceCard,
        path: String,
        effectiveFamily: ModelFamily,
    ) {
        upsertChecked(voice, path, effectiveFamily = effectiveFamily)
    }

    private suspend fun upsertChecked(
        voice: VoiceCard,
        path: String,
        effectiveFamily: ModelFamily?,
    ) {
        val entity = InstalledVoiceEntity(
            voiceId = voice.id,
            family = voice.family,
            title = voice.title,
            languages = voice.languages.joinToString(","),
            speakersJson = catalogJson.encodeToString(speakerListSerializer, voice.speakers),
            sampleRateHz = voice.sampleRateHz,
            installedPath = path,
            tier = voice.tier,
            installedAt = System.currentTimeMillis(),
            effectiveFamily = effectiveFamily?.name?.lowercase(),
        )
        dao.upsert(entity)
        log.i { "Marked installed: ${voice.id} at $path (effective=$effectiveFamily)" }
    }

    override suspend fun uninstall(voiceId: String) {
        val existing = dao.getById(voiceId) ?: return
        // Always recursively wipe the on-disk voice directory. The path stored
        // on the row points to the unpacked bundle (filesDir/voices/<id> for
        // downloaded voices, or .../custom-<uuid> for imports). Skipping this
        // would leak ~80 MB per uninstall for a typical Piper bundle.
        runCatching { File(existing.installedPath).deleteRecursively() }
            .onFailure { log.w(it) { "Failed to delete $voiceId voice dir" } }
        dao.deleteById(voiceId)
        log.i { "Uninstalled $voiceId" }
    }

    /**
     * Snapshot for callers that cannot collect a Flow (e.g. the
     * non-coroutine [android.speech.tts.TextToSpeechService.onGetVoices]).
     */
    suspend fun installedSnapshot(): List<InstalledVoice> =
        dao.getAllSnapshot().map { it.toDomain() }

    private fun InstalledVoiceEntity.toDomain(): InstalledVoice = InstalledVoice(
        voiceId = voiceId,
        family = ModelFamily.fromCatalog(family),
        title = title,
        languages = if (languages.isEmpty()) emptyList() else languages.split(',').map { it.trim() },
        speakers = runCatching {
            catalogJson.decodeFromString(speakerListSerializer, speakersJson)
        }.getOrElse {
            log.w(it) { "Bad speakersJson for $voiceId; using empty list" }
            emptyList()
        },
        sampleRateHz = sampleRateHz,
        installedPath = installedPath,
        tier = Tier.fromCatalog(tier),
        installedAt = installedAt,
        bundled = false,
        effectiveFamily = effectiveFamily?.let(ModelFamily::fromCatalog),
    )
}

/**
 * Best-effort BCP-47 -> [java.util.Locale] parser. Returns [Locale.ROOT] for
 * malformed input rather than throwing so the TTS service never crashes on
 * an unexpected catalog entry.
 */
internal fun parseLocale(tag: String): Locale = runCatching {
    Locale.forLanguageTag(tag)
}.getOrDefault(Locale.ROOT)
