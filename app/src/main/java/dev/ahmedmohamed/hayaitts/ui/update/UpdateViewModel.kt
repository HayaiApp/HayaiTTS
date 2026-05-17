package dev.ahmedmohamed.hayaitts.ui.update

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.ahmedmohamed.hayaitts.data.update.DownloadProgress
import dev.ahmedmohamed.hayaitts.data.update.UpdateChannel
import dev.ahmedmohamed.hayaitts.data.update.UpdateChecker
import dev.ahmedmohamed.hayaitts.data.update.UpdateInstaller
import dev.ahmedmohamed.hayaitts.data.update.UpdateStatus
import dev.ahmedmohamed.hayaitts.domain.repo.SettingsRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Owns the auto-updater's UI state.
 *
 * One instance is shared between [dev.ahmedmohamed.hayaitts.ui.MainActivity]
 * (which hosts the launch-time dialog) and the Updates section of the Settings
 * screen — both call [koinViewModel] which resolves to the same Koin definition.
 */
class UpdateViewModel(
    private val settings: SettingsRepository,
    private val checker: UpdateChecker,
    private val installer: UpdateInstaller,
) : ViewModel() {

    private val _status = MutableStateFlow<UpdateStatus>(UpdateStatus.Idle)
    val status: StateFlow<UpdateStatus> = _status.asStateFlow()

    private val _download = MutableStateFlow<DownloadProgress?>(null)
    val download: StateFlow<DownloadProgress?> = _download.asStateFlow()

    /** Whether the user has dismissed the "Available" dialog this process. */
    private val _dialogDismissed = MutableStateFlow(false)
    val dialogDismissed: StateFlow<Boolean> = _dialogDismissed.asStateFlow()

    val updateChannel: StateFlow<UpdateChannel> = settings.updateChannel
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), UpdateChannel.STABLE)

    val lastChecked: StateFlow<Long> = settings.lastUpdateCheckMillis
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), 0L)

    private var downloadJob: Job? = null

    /**
     * Runs a check. Always sets [_status] to [UpdateStatus.Checking] first so
     * the Settings snackbar can show a spinner; the 6h debounce is enforced
     * inside [UpdateChecker.check] unless [force] is true.
     */
    fun checkNow(force: Boolean = true) {
        viewModelScope.launch {
            _status.value = UpdateStatus.Checking
            _status.value = checker.check(force = force)
            _dialogDismissed.value = false
        }
    }

    /** Quiet check used by the launch-time auto-poll. Honors the 6h debounce. */
    fun autoCheck() {
        viewModelScope.launch {
            val current = _status.value
            // Don't clobber an in-flight check, an existing Available status,
            // or a Failed result the user has not seen yet.
            if (current is UpdateStatus.Checking || current is UpdateStatus.Available) return@launch
            val result = checker.check(force = false)
            // Only surface success states from the silent auto-check — a
            // network blip on launch should not show a scary banner.
            if (result is UpdateStatus.Available || result is UpdateStatus.UpToDate) {
                _status.value = result
            }
        }
    }

    fun setChannel(channel: UpdateChannel) {
        viewModelScope.launch { settings.setUpdateChannel(channel) }
    }

    /** Start downloading the universal APK for the currently-Available release. */
    fun startInstall() {
        val available = _status.value as? UpdateStatus.Available ?: return
        downloadJob?.cancel()
        _download.value = DownloadProgress.Running(0L, -1L)
        downloadJob = viewModelScope.launch {
            installer.download(available.universalApkUrl, available.tag).collect { progress ->
                _download.value = progress
                if (progress is DownloadProgress.Done) {
                    installer.install(progress.apkFile)
                }
            }
        }
    }

    fun cancelInstall() {
        downloadJob?.cancel()
        downloadJob = null
        _download.value = DownloadProgress.Cancelled
    }

    fun dismissDialog() {
        _dialogDismissed.value = true
        // Keep `status` so Settings can still display "Update available".
        if (_download.value !is DownloadProgress.Running) {
            _download.value = null
        }
    }

    fun consumeStatus() {
        _status.value = UpdateStatus.Idle
    }
}
