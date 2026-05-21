package dev.ahmedmohamed.hayaitts.domain.model

/**
 * Release channels the auto-updater can subscribe to. Lives in `domain/` so
 * the [dev.ahmedmohamed.hayaitts.domain.repo.SettingsRepository] interface can
 * reference it without dragging data-layer types into the contract.
 *
 * Tag-name convention fixed by HayaiTTS' release workflow:
 *  - Stable    : `vX.Y.Z`           (GitHub `prerelease=false`, no `-b` suffix)
 *  - Beta      : `vX.Y.Z-bN`        (`prerelease=true`)
 *  - Nightly   : `rNN` (any prefix) (`prerelease=true`, ad-hoc commit builds)
 *
 * STABLE only matches the first row. BETA matches stable + beta (opt-in to
 * earlier access without nightly churn). NIGHTLY accepts anything the
 * workflow publishes — the user is signing up for breakage.
 */
enum class UpdateChannel {
    STABLE,
    BETA,
    NIGHTLY,
}
