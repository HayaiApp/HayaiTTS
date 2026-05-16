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
import dev.ahmedmohamed.hayaitts.tts.SherpaTtsRuntime
import dev.ahmedmohamed.hayaitts.tts.SherpaTtsRuntime.Companion.BUNDLED_VOICE_ID
import dev.ahmedmohamed.hayaitts.domain.repo.VoiceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import java.io.File
import java.util.Locale

/**
 * Stitches the in-APK bundled voice on top of the Room-backed installed list.
 * The bundled voice always sorts first; downloaded voices follow in
 * `installedAt DESC` order (newest first), matching the DAO.
 */
class VoiceRepositoryImpl(
    private val context: Context,
    private val dao: InstalledVoiceDao,
) : VoiceRepository {

    private val log = Logger.withTag("VoiceRepository")
    private val speakerListSerializer = ListSerializer(Speaker.serializer())

    override val installed: Flow<List<InstalledVoice>> = dao.getAll().map { rows ->
        buildList {
            add(bundledVoice())
            rows.forEach { add(it.toDomain()) }
        }
    }

    override suspend fun isInstalled(voiceId: String): Boolean {
        if (voiceId == BUNDLED_VOICE_ID) return true
        return dao.getById(voiceId) != null
    }

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
        if (voice.id == BUNDLED_VOICE_ID) {
            log.w { "Refusing to overwrite bundled voice metadata for ${voice.id}" }
            return
        }
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
        if (voiceId == BUNDLED_VOICE_ID) {
            log.w { "Refusing to uninstall bundled voice $voiceId" }
            return
        }
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
    suspend fun installedSnapshot(): List<InstalledVoice> = buildList {
        add(bundledVoice())
        dao.getAllSnapshot().forEach { add(it.toDomain()) }
    }

    private fun bundledVoice(): InstalledVoice = InstalledVoice(
        voiceId = BUNDLED_VOICE_ID,
        family = ModelFamily.PIPER,
        title = "Amy",
        languages = listOf("en-US"),
        speakers = listOf(Speaker(id = 0, name = "amy", gender = "F")),
        sampleRateHz = SherpaTtsRuntime.BUNDLED_VOICE_SAMPLE_RATE,
        // Path resolves at runtime to filesDir/voices/en_US-amy-low after the
        // SherpaTtsRuntime asset mirror runs; we surface the post-mirror path
        // here so consumers see a stable absolute path even before mirroring.
        installedPath = File(context.filesDir, SherpaTtsRuntime.VOICE_ASSET_SUBDIR).absolutePath,
        tier = Tier.LOW,
        installedAt = 0L,
        bundled = true,
    )

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
