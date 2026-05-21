package dev.ahmedmohamed.hayaitts.domain.usecase

import dev.ahmedmohamed.hayaitts.core.result.AppError
import dev.ahmedmohamed.hayaitts.core.result.Outcome
import dev.ahmedmohamed.hayaitts.core.result.runCatchingOutcome
import dev.ahmedmohamed.hayaitts.domain.repo.CatalogRepository

/**
 * Forces a one-shot catalog refresh from the network. Used by the manual
 * "Check for new voices" button and by the periodic UpdateCheckWorker.
 *
 * Returns the new voice count (`Success(count)`) or a typed `Failure`. The
 * UI maps `Network` to "No internet" and `Catalog` to "Manifest invalid".
 */
class RefreshCatalogUseCase(
    private val catalog: CatalogRepository,
) {
    suspend operator fun invoke(): Outcome<Int> = runCatchingOutcome(
        classify = { AppError.Network },
    ) {
        catalog.refresh()
        catalog.catalog.value.size
    }
}
