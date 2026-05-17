package dev.ahmedmohamed.hayaitts.data.catalog

import android.content.Context
import co.touchlab.kermit.Logger
import dev.ahmedmohamed.hayaitts.domain.model.CatalogManifest
import dev.ahmedmohamed.hayaitts.domain.model.VoiceCard
import dev.ahmedmohamed.hayaitts.domain.repo.CatalogRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Loads the catalog from the bundled asset on construction, then attempts a
 * one-shot network refresh in the background. [refresh] re-runs the network
 * fetch on demand.
 *
 * The remote URL is a `raw.githubusercontent.com` link to the same JSON we
 * bundle as an asset; once the catalog is hosted there it can be updated
 * without shipping a new APK. If the URL 404s (which it currently does — the
 * catalog/v1/ tree is not pushed yet) we silently fall back to the asset.
 */
class CatalogRepositoryImpl(
    private val context: Context,
    private val okHttp: OkHttpClient,
    private val externalScope: CoroutineScope,
) : CatalogRepository {

    private val log = Logger.withTag("CatalogRepository")
    private val _catalog = MutableStateFlow<List<VoiceCard>>(emptyList())
    override val catalog: StateFlow<List<VoiceCard>> = _catalog.asStateFlow()

    init {
        // Bundled asset is always present in the APK — load it synchronously
        // so the first emission of [catalog] is non-empty before any consumer
        // can subscribe.
        runCatching { loadFromAsset() }
            .onSuccess { _catalog.value = it }
            .onFailure { log.e(it) { "Failed to load bundled catalog asset" } }

        externalScope.launch {
            runCatching { refresh() }
                .onFailure { log.w(it) { "Background catalog refresh failed; using bundled copy" } }
        }
    }

    override suspend fun refresh() {
        val remote = withContext(Dispatchers.IO) { fetchFromNetwork() }
        if (remote.isNullOrEmpty()) {
            log.i { "Remote catalog empty/unreachable; keeping current ${_catalog.value.size} entries" }
            return
        }
        log.i { "Catalog refreshed from network: ${remote.size} voices" }
        _catalog.value = remote
    }

    private fun loadFromAsset(): List<VoiceCard> {
        val raw = context.assets.open(ASSET_PATH).bufferedReader().use { it.readText() }
        val manifest = catalogJson.decodeFromString<CatalogManifest>(raw)
        log.i { "Loaded bundled catalog (v${manifest.version}, ${manifest.voices.size} voices)" }
        return manifest.voices
    }

    private fun fetchFromNetwork(): List<VoiceCard>? = try {
        val request = Request.Builder().url(REMOTE_URL).build()
        okHttp.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                log.i { "Catalog refresh HTTP ${response.code} — keeping bundled" }
                null
            } else {
                val body = response.body?.string() ?: return@use null
                // Stamp every remote-sourced voice with fromRemote=true so the
                // downloader can apply the strict checksum policy (see
                // VoiceDownloadWorker). The JSON itself never carries this
                // field; we synthesize it here from the transport.
                catalogJson.decodeFromString<CatalogManifest>(body)
                    .voices
                    .map { it.copy(fromRemote = true) }
            }
        }
    } catch (t: Throwable) {
        log.w(t) { "Catalog refresh threw" }
        null
    }

    companion object {
        private const val ASSET_PATH = "catalog/v1/models.json"
        // The remote URL points at the GitHub `HayaiApp/HayaiTTS` repository on its
        // default branch. Until the repo is pushed for the first time this 404s
        // and the bundled asset fallback wins (cf. `loadRemote()`); both paths
        // are silent on failure so users never see a network error toast.
        private const val REMOTE_URL =
            "https://raw.githubusercontent.com/HayaiApp/HayaiTTS/main/catalog/v1/models.json"
    }
}
