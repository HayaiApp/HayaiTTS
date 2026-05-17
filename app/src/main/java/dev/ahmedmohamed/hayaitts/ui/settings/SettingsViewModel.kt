package dev.ahmedmohamed.hayaitts.ui.settings

import android.content.Context
import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.ahmedmohamed.hayaitts.domain.model.InstalledVoice
import dev.ahmedmohamed.hayaitts.domain.model.StorageLocation
import dev.ahmedmohamed.hayaitts.domain.repo.DefaultsRepository
import dev.ahmedmohamed.hayaitts.domain.repo.SettingsRepository
import dev.ahmedmohamed.hayaitts.domain.repo.VoiceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
class SettingsViewModel(
    private val context: Context,
    private val settings: SettingsRepository,
    private val voices: VoiceRepository,
    private val defaults: DefaultsRepository,
) : ViewModel() {

    data class SettingsUiState(
        val wifiOnly: Boolean = true,
        val storageLocation: StorageLocation = StorageLocation.INTERNAL,
        val hasExternalStorage: Boolean = false,
        val installed: List<InstalledVoice> = emptyList(),
        val defaultsByLocale: Map<String, String> = emptyMap(),
        val totalInstalledBytes: Long = 0L,
    ) {
        /** Locales covered by the union of installed voices. */
        val installedLocales: List<String>
            get() = installed
                .flatMap { it.languages }
                .distinct()
                .sorted()
    }

    private val totalBytes = MutableStateFlow(0L)

    val uiState: StateFlow<SettingsUiState> = combine(
        settings.wifiOnly,
        settings.storageLocation,
        voices.installed,
        defaults.defaults,
        totalBytes,
    ) { wifi, location, installed, defaultsMap, bytes ->
        SettingsUiState(
            wifiOnly = wifi,
            storageLocation = location,
            hasExternalStorage = hasExternalStorage(),
            installed = installed,
            defaultsByLocale = defaultsMap,
            totalInstalledBytes = bytes,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = SettingsUiState(),
    )

    private val cacheClearedEvents = MutableStateFlow<Long?>(null)
    val cacheClearedBytes: StateFlow<Long?> = cacheClearedEvents.asStateFlow()

    init {
        refreshInstalledSize()
    }

    fun setWifiOnly(value: Boolean) {
        viewModelScope.launch { settings.setWifiOnly(value) }
    }

    fun setStorageLocation(value: StorageLocation) {
        viewModelScope.launch { settings.setStorageLocation(value) }
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
        viewModelScope.launch(Dispatchers.IO) {
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
            val freed = withContext(Dispatchers.IO) {
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

    private fun hasExternalStorage(): Boolean {
        // The first entry of getExternalFilesDirs is the primary external dir
        // (often emulated /sdcard). A genuine removable SD card surfaces as a
        // second entry on multi-volume devices. We only consider external
        // storage available if there's actually a second mount point AND it
        // reports MOUNTED.
        val dirs = context.getExternalFilesDirs(null)?.filterNotNull().orEmpty()
        if (dirs.size < 2) return false
        val state = runCatching { Environment.getExternalStorageState(dirs[1]) }.getOrNull()
        return state == Environment.MEDIA_MOUNTED
    }
}
