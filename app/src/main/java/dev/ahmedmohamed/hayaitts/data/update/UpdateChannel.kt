package dev.ahmedmohamed.hayaitts.data.update

/**
 * Release channels the auto-updater can subscribe to.
 *
 * The tag-name convention is fixed by HayaiTTS' release workflow:
 *  - Stable    : `vX.Y.Z`           (GitHub `prerelease=false`, no `-b` suffix)
 *  - Beta      : `vX.Y.Z-bN`        (`prerelease=true`)
 *  - Nightly   : `rNN` (any prefix) (`prerelease=true`, ad-hoc commit builds)
 *
 * STABLE only matches the first row. BETA matches stable + beta (an opt-in to
 * earlier access without nightly churn). NIGHTLY accepts anything the workflow
 * publishes — the user is signing up for breakage.
 *
 * Default is [STABLE] (cf. [dev.ahmedmohamed.hayaitts.data.settings.SettingsRepositoryImpl]).
 */
enum class UpdateChannel {
    STABLE,
    BETA,
    NIGHTLY,
}
