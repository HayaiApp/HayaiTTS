package dev.ahmedmohamed.hayaitts.domain.model

/**
 * Domain mirror of `InstalledVoiceEntity`. Returned by [VoiceRepository] so
 * the rest of the app never sees Room types directly.
 */
data class InstalledVoice(
    val voiceId: String,
    val family: ModelFamily,
    val title: String,
    val languages: List<String>,
    val speakers: List<Speaker>,
    val sampleRateHz: Int,
    val installedPath: String,
    val tier: Tier,
    val installedAt: Long,
    /** True for the in-APK bundled voice (Amy). Bundled voices cannot be uninstalled. */
    val bundled: Boolean = false,
)
