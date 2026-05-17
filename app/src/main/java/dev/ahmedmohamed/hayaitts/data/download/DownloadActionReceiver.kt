package dev.ahmedmohamed.hayaitts.data.download

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import co.touchlab.kermit.Logger
import dev.ahmedmohamed.hayaitts.data.catalog.catalogJson
import dev.ahmedmohamed.hayaitts.data.db.dao.DownloadStateDao
import dev.ahmedmohamed.hayaitts.domain.model.VoiceCard
import dev.ahmedmohamed.hayaitts.domain.repo.CatalogRepository
import dev.ahmedmohamed.hayaitts.domain.repo.DownloadRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext

/**
 * Handles Cancel + Retry actions on download notifications. Manifest-registered
 * so it survives process death; Koin's GlobalContext resolves dependencies.
 */
class DownloadActionReceiver : BroadcastReceiver() {

    private val log = Logger.withTag("DownloadActionReceiver")

    override fun onReceive(context: Context, intent: Intent) {
        val voiceId = intent.getStringExtra(EXTRA_VOICE_ID).orEmpty()
        if (voiceId.isBlank()) {
            log.w { "Ignoring action ${intent.action} with no voiceId" }
            return
        }
        val pending = goAsync()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            try {
                val koin = runCatching { GlobalContext.get() }.getOrNull()
                if (koin == null) {
                    log.w { "Koin not initialised; ignoring ${intent.action}" }
                    return@launch
                }
                val downloads: DownloadRepository = koin.get()
                when (intent.action) {
                    ACTION_CANCEL -> {
                        log.i { "User cancelled download $voiceId via notification" }
                        downloads.cancel(voiceId)
                        DownloadNotifications.cancelActive(context)
                    }
                    ACTION_RETRY -> {
                        log.i { "User retried download $voiceId via notification" }
                        val card = resolveVoiceCard(koin, voiceId)
                        if (card != null) downloads.enqueue(card)
                        else log.w { "Cannot retry $voiceId - voiceCard not found" }
                    }
                    else -> log.w { "Unknown action ${intent.action}" }
                }
            } catch (t: Throwable) {
                log.e(t) { "Failed to handle ${intent.action} for $voiceId" }
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun resolveVoiceCard(koin: org.koin.core.Koin, voiceId: String): VoiceCard? {
        val catalog: CatalogRepository = koin.get()
        val live = catalog.catalog.value.firstOrNull { it.id == voiceId }
        if (live != null) return live
        val dao: DownloadStateDao = koin.get()
        val raw = dao.getById(voiceId)?.errorMessage
        if (raw.isNullOrBlank() || !raw.trimStart().startsWith("{")) return null
        return runCatching { catalogJson.decodeFromString(VoiceCard.serializer(), raw) }.getOrNull()
    }

    companion object {
        const val ACTION_CANCEL = "dev.ahmedmohamed.hayaitts.action.CANCEL_DOWNLOAD"
        const val ACTION_RETRY = "dev.ahmedmohamed.hayaitts.action.RETRY_DOWNLOAD"
        const val EXTRA_VOICE_ID = "voiceId"
    }
}
