package dev.ahmedmohamed.hayaitts.data.update

/**
 * Terminal outcome of an [UpdateChecker.check] call. Held in the UpdateViewModel
 * as a [kotlinx.coroutines.flow.StateFlow] so the launch-time auto-check and the
 * Settings "Check now" button both feed the same dialog host.
 */
sealed interface UpdateStatus {

    /** Initial value before any check has run this process. */
    data object Idle : UpdateStatus

    /** A check is currently in flight. */
    data object Checking : UpdateStatus

    /** The installed build is at or ahead of the latest matching release. */
    data object UpToDate : UpdateStatus

    /**
     * A newer release exists on the configured channel.
     *
     * [universalApkUrl] is the `browser_download_url` of the universal-ABI APK
     * asset (named `hayaitts-v<tag>.apk` by the release workflow). Per-ABI
     * variants are intentionally ignored — sideloading a single universal APK
     * is the simplest path for an in-app update flow and avoids the "wrong ABI"
     * failure mode that would otherwise need an ABI detector here.
     */
    data class Available(
        val tag: String,
        val name: String,
        val body: String,
        val publishedAt: String,
        val universalApkUrl: String,
        val channel: UpdateChannel,
    ) : UpdateStatus

    /** Network or parse error. Surfaces in the Settings snackbar verbatim. */
    data class Failed(val reason: String) : UpdateStatus
}
