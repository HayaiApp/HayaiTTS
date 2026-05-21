package dev.ahmedmohamed.hayaitts.domain.usecase

import dev.ahmedmohamed.hayaitts.core.result.AppError
import dev.ahmedmohamed.hayaitts.core.result.Outcome
import dev.ahmedmohamed.hayaitts.core.result.asFailure
import dev.ahmedmohamed.hayaitts.core.result.asSuccess
import dev.ahmedmohamed.hayaitts.domain.model.VoiceCard
import dev.ahmedmohamed.hayaitts.domain.repo.CatalogRepository
import dev.ahmedmohamed.hayaitts.domain.repo.DownloadRepository

/**
 * Resolves a [voiceId] in the catalog and enqueues the download.
 *
 * Surfaces a typed [Outcome] so the caller (Voice Detail VM, Browse VM,
 * Studio VM) can render an inline error without re-implementing string
 * mapping per call site.
 */
class InstallVoiceUseCase(
    private val catalog: CatalogRepository,
    private val downloads: DownloadRepository,
) {
    suspend operator fun invoke(voiceId: String): Outcome<VoiceCard> {
        val card = catalog.catalog.value.firstOrNull { it.id == voiceId }
            ?: return AppError.Catalog("Voice $voiceId not in catalog").asFailure()
        if (!card.available) {
            return AppError.Runtime(card.family).asFailure()
        }
        downloads.enqueue(card)
        return card.asSuccess()
    }
}
