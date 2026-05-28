package dev.ahmedmohamed.hayaitts.ui.settings

import android.annotation.SuppressLint
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.ahmedmohamed.hayaitts.core.dispatchers.DispatcherProvider
import dev.ahmedmohamed.hayaitts.data.storage.StorageMigrator
import dev.ahmedmohamed.hayaitts.domain.model.InstalledVoice
import dev.ahmedmohamed.hayaitts.domain.model.MoveProgress
import dev.ahmedmohamed.hayaitts.domain.model.StorageLocation
import dev.ahmedmohamed.hayaitts.domain.repo.CatalogRepository
import dev.ahmedmohamed.hayaitts.domain.repo.DefaultsRepository
import dev.ahmedmohamed.hayaitts.domain.repo.SettingsRepository
import dev.ahmedmohamed.hayaitts.domain.repo.VoiceRepository
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Drives [SettingsActivity]'s Compose screen. Joins user preferences (wifi-only,
 * storage location), the installed-voices list, and the per-locale defaults
 * map into one [SettingsUiState] so the screen can render without juggling
 * three separate flows.
 *
 * Storage size computation is debounced — every recomposition that lands on
 * the Storage section refreshes the on-disk total in IO and pushes the
 * result back through [recomputedSizes].
 */
@SuppressLint("StaticFieldLeak") // Koin wires `context` with androidContext() — guaranteed Application — so the field-leak rule fires on a false positive.
class SettingsViewModel(
    private val context: Context,
    private val settings: SettingsRepository,
    private val voices: VoiceRepository,
    private val defaults: DefaultsRepository,
    private val migrator: StorageMigrator,
    private val dispatchers: DispatcherProvider,
    private val catalog: CatalogRepository,
) : ViewModel() {

    data class SettingsUiState(
        val wifiOnly: Boolean = true,
        val storageLocation: StorageLocation = StorageLocation.INTERNAL,
        val hasExternalStorage: Boolean = false,
        val installed: List<InstalledVoice> = emptyList(),
        val defaultsByLocale: Map<String, String> = emptyMap(),
        val totalInstalledBytes: Long = 0L,
        val moveProgress: MoveProgress = MoveProgress.Idle,
        val useNnapi: Boolean = false,
        val synthesisThreads: Int = 2,
        val maxNumSentences: Int = 2,
    ) {
        /** Locales covered by the union of installed voices. */
        val installedLocales: List<String>
            get() = installed
                .flatMap { it.languages }
                .distinct()
                .sorted()

        /** True while voices are being relocated — the screen disables the toggle. */
        val isMoving: Boolean
            get() = moveProgress is MoveProgress.Moving
    }

    private val totalBytes = MutableStateFlow(0L)
    private val moveProgress = MutableStateFlow<MoveProgress>(MoveProgress.Idle)

    val uiState: StateFlow<SettingsUiState> = combine(
        settings.wifiOnly,
        settings.storageLocation,
        voices.installed,
        defaults.defaults,
        totalBytes,
        moveProgress,
        settings.useNnapi,
        settings.synthesisThreads,
        settings.maxNumSentences,
    ) { values ->
        SettingsUiState(
            wifiOnly = values[0] as Boolean,
            storageLocation = values[1] as StorageLocation,
            hasExternalStorage = migrator.hasExternalStorage(),
            installed = @Suppress("UNCHECKED_CAST") (values[2] as List<InstalledVoice>),
            defaultsByLocale = @Suppress("UNCHECKED_CAST") (values[3] as Map<String, String>),
            totalInstalledBytes = values[4] as Long,
            moveProgress = values[5] as MoveProgress,
            useNnapi = values[6] as Boolean,
            synthesisThreads = values[7] as Int,
            maxNumSentences = values[8] as Int,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = SettingsUiState(),
    )

    private val cacheClearedEvents = MutableStateFlow<Long?>(null)
    val cacheClearedBytes: StateFlow<Long?> = cacheClearedEvents.asStateFlow()

    /** Every BCP-47 language tag present anywhere in the catalog, alpha-sorted. */
    val catalogLanguages: StateFlow<List<String>> = catalog.catalog
        .map { c -> c.flatMap { it.languages }.distinct().sorted() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), emptyList())

    val allowedLanguages: StateFlow<Set<String>> = settings.allowedLanguages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), emptySet())

    fun setAllowedLanguages(value: Set<String>) {
        viewModelScope.launch { settings.setAllowedLanguages(value) }
    }

    init {
        refreshInstalledSize()
    }

    fun setWifiOnly(value: Boolean) {
        viewModelScope.launch { settings.setWifiOnly(value) }
    }

    fun setUseNnapi(value: Boolean) {
        viewModelScope.launch { settings.setUseNnapi(value) }
    }

    fun setSynthesisThreads(value: Int) {
        viewModelScope.launch { settings.setSynthesisThreads(value) }
    }

    fun setMaxNumSentences(value: Int) {
        viewModelScope.launch { settings.setMaxNumSentences(value) }
    }

    /**
     * Asks the migrator to physically relocate every non-bundled installed
     * voice to the directory governed by [value]. The migrator only persists
     * the preference once every voice has moved successfully — partial
     * failures leave the source location authoritative so subsequent
     * downloads keep going there until the user retries.
     */
    fun setStorageLocation(value: StorageLocation) {
        if (uiState.value.isMoving) return
        viewModelScope.launch {
            migrator.moveAllVoices(value).collect { moveProgress.value = it }
            // Recompute the on-disk total since the source dir was wiped.
            refreshInstalledSize()
        }
    }

    /** Acknowledge a terminal [MoveProgress.Done] or [MoveProgress.Failed] state. */
    fun consumeMoveResult() {
        val current = moveProgress.value
        if (current is MoveProgress.Done || current is MoveProgress.Failed) {
            moveProgress.value = MoveProgress.Idle
        }
    }

    fun setDefault(locale: String, voiceId: String?) {
        viewModelScope.launch {
            if (voiceId == null) defaults.clearDefault(locale)
            else defaults.setDefault(locale, voiceId)
        }
    }

    /**
     * Walks `filesDir/voices/` plus the bundled mirror to compute the total
     * size on disk. Called on demand by the Storage section's recomposition
     * so we don't pay the cost on every settings open.
     */
    fun refreshInstalledSize() {
        viewModelScope.launch(dispatchers.io) {
            val voicesRoot = File(context.filesDir, "voices")
            val total = if (voicesRoot.isDirectory) {
                voicesRoot.walkBottomUp().filter { it.isFile }.sumOf { it.length() }
            } else 0L
            totalBytes.value = total
        }
    }

    /**
     * Wipes `cacheDir/downloads/` — used by the worker for partial tarballs.
     * Returns the freed bytes through [cacheClearedBytes] so the screen can
     * surface a snackbar confirmation.
     */
    fun clearDownloadCache() {
        viewModelScope.launch {
            val freed = withContext(dispatchers.io) {
                val dir = File(context.cacheDir, "downloads")
                if (!dir.isDirectory) return@withContext 0L
                val bytes = dir.walkBottomUp().filter { it.isFile }.sumOf { it.length() }
                dir.deleteRecursively()
                bytes
            }
            cacheClearedEvents.value = freed
        }
    }

    fun consumeCacheClearedEvent() {
        cacheClearedEvents.value = null
    }

}
