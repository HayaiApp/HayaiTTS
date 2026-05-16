package dev.ahmedmohamed.hayaitts.domain.repo

import dev.ahmedmohamed.hayaitts.domain.model.VoiceCard
import kotlinx.coroutines.flow.StateFlow

/**
 * Source of truth for the downloadable voice catalog. Implementations seed
 * the flow from the bundled asset and asynchronously refresh from the remote
 * URL when [refresh] is called or when the repository is first constructed.
 */
interface CatalogRepository {
    val catalog: StateFlow<List<VoiceCard>>
    suspend fun refresh()
}
