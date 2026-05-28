package dev.ahmedmohamed.hayaitts.data.update

import co.touchlab.kermit.Logger
import dev.ahmedmohamed.hayaitts.BuildConfig
import dev.ahmedmohamed.hayaitts.core.dispatchers.DispatcherProvider
import dev.ahmedmohamed.hayaitts.domain.model.UpdateChannel
import dev.ahmedmohamed.hayaitts.domain.repo.SettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Polls GitHub Releases for a newer HayaiTTS build that matches the user's
 * configured [UpdateChannel].
 *
 * The endpoint is the unauthenticated `releases` list (latest 30 sorted by
 * `created_at` desc) — that window is wide enough to catch the latest nightly
 * `rNN` build even when the workflow has produced a few in a row, while still
 * fitting in one round-trip. Hitting GitHub anonymously has a 60 req/IP/hour
 * limit; the 6h debounce in [dev.ahmedmohamed.hayaitts.ui.update.UpdateViewModel]
 * keeps us comfortably below it.
 *
 * The "Available" decision uses [versionCompare]:
 *   - tag like `v1.2.3` is parsed as semver and compared numeric-tuple-wise.
 *   - tag like `r42` is treated as a single-integer "build number".
 *   - The user's installed version comes from [BuildConfig.VERSION_NAME].
 *
 * A non-2xx response, network error, or JSON parse failure produces
 * [UpdateStatus.Failed] — never crashes the auto-check coroutine.
 */
class UpdateChecker(
    private val okHttp: OkHttpClient,
    private val settings: SettingsRepository,
    private val dispatchers: DispatcherProvider,
) {
    private val log = Logger.withTag("UpdateChecker")

    // Lenient JSON: GitHub adds fields freely and we only read a handful.
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    /**
     * Runs one check. Caller decides whether to debounce — pass [force] = true
     * from the manual "Check now" button so the 6h cooldown is bypassed.
     *
     * Returns [UpdateStatus.UpToDate], [Available][UpdateStatus.Available], or
     * [Failed][UpdateStatus.Failed]. On success (either UpToDate or Available)
     * the timestamp is persisted via [SettingsRepository.setLastUpdateCheckMillis].
     */
    suspend fun check(force: Boolean = false): UpdateStatus = withContext(dispatchers.io) {
        val channel = settings.updateChannel.first()
        if (!force) {
            val last = settings.lastUpdateCheckMillis.first()
            val now = System.currentTimeMillis()
            if (last > 0 && now - last < DEBOUNCE_MILLIS) {
                log.d { "Skipping check, last run ${(now - last) / 60_000} min ago" }
                return@withContext UpdateStatus.UpToDate
            }
        }

        // Stable/Beta releases live on the main repo; nightlies were split
        // out to HayaiApp/HayaiTTS-nightly so the main repo's Releases page
        // stays focused on production-ready builds. Pick the URL based on
        // the user's configured channel.
        val url = when (channel) {
            UpdateChannel.NIGHTLY -> NIGHTLY_RELEASES_URL
            UpdateChannel.STABLE, UpdateChannel.BETA -> RELEASES_URL
        }
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "HayaiTTS/${BuildConfig.VERSION_NAME}")
            .build()

        try {
            okHttp.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext UpdateStatus.Failed("HTTP ${response.code}")
                }
                val body = response.body?.string()
                    ?: return@withContext UpdateStatus.Failed("Empty response body")
                val releases = json.decodeFromString<List<GitHubRelease>>(body)
                val pick = pickLatestMatching(releases, channel)
                    ?: return@withContext UpdateStatus.UpToDate
                settings.setLastUpdateCheckMillis(System.currentTimeMillis())
                val asset = pick.assets.firstOrNull { it.name.endsWith(UNIVERSAL_APK_SUFFIX) && !it.name.contains("-arm64") && !it.name.contains("-armeabi") && !it.name.contains("-x86") }
                    ?: pick.assets.firstOrNull { it.name.endsWith(".apk") }
                    ?: return@withContext UpdateStatus.Failed("Release ${pick.tagName} has no APK asset")
                val installedNewerOrEqual = versionCompare(BuildConfig.VERSION_NAME, pick.tagName.trimStart('v')) >= 0
                if (installedNewerOrEqual) {
                    UpdateStatus.UpToDate
                } else {
                    UpdateStatus.Available(
                        tag = pick.tagName,
                        name = pick.name.ifBlank { pick.tagName },
                        body = pick.body.orEmpty().trim(),
                        publishedAt = pick.publishedAt.orEmpty(),
                        universalApkUrl = asset.browserDownloadUrl,
                        channel = channel,
                    )
                }
            }
        } catch (t: Throwable) {
            log.w(t) { "Update check failed" }
            UpdateStatus.Failed(t.message ?: t::class.simpleName.orEmpty())
        }.also {
            if (it is UpdateStatus.UpToDate) {
                settings.setLastUpdateCheckMillis(System.currentTimeMillis())
            }
        }
    }

    /**
     * Filter the API response by channel rules, then return the newest entry
     * that survives. The list is already ordered newest-first by created_at.
     */
    private fun pickLatestMatching(
        releases: List<GitHubRelease>,
        channel: UpdateChannel,
    ): GitHubRelease? {
        return releases.firstOrNull { release ->
            if (release.draft) return@firstOrNull false
            val tag = release.tagName
            when (channel) {
                UpdateChannel.STABLE ->
                    tag.startsWith("v") && !tag.contains("-b") && !release.prerelease
                UpdateChannel.BETA ->
                    tag.startsWith("v") && (!release.prerelease || tag.contains("-b"))
                UpdateChannel.NIGHTLY -> true
            }
        }
    }

    companion object {
        const val RELEASES_URL =
            "https://api.github.com/repos/HayaiApp/HayaiTTS/releases?per_page=30"

        /**
         * Dedicated nightly repo. The main repo now only hosts stable + beta
         * releases (see `.github/workflows/build_push.yml`); nightly `r###`
         * tags land here so users on the Nightly channel still get pinged.
         */
        const val NIGHTLY_RELEASES_URL =
            "https://api.github.com/repos/HayaiApp/HayaiTTS-nightly/releases?per_page=30"

        // Matches the per-tag universal-ABI asset produced by build_push.yml:
        // `hayaitts-v<tag>.apk` (no ABI infix). The per-ABI files
        // (`hayaitts-arm64-v8a-v<tag>.apk` etc.) embed the ABI before the
        // version and are intentionally skipped — see UpdateStatus.Available.
        private const val UNIVERSAL_APK_SUFFIX = ".apk"

        // 6h matches the spec; the manual "Check now" button passes force=true.
        const val DEBOUNCE_MILLIS = 6L * 60 * 60 * 1000

        /**
         * Compare two version strings of the form `1.2.3`, `1.2.3-b1`, `r42`.
         *
         * Returns a negative int when [installed] is older than [released],
         * zero when equal, positive when newer. Anything we can't parse is
         * treated as "0" so the comparator stays total.
         */
        fun versionCompare(installed: String, released: String): Int {
            val a = parseVersion(installed)
            val b = parseVersion(released)
            val len = maxOf(a.size, b.size)
            for (i in 0 until len) {
                val av = a.getOrElse(i) { 0 }
                val bv = b.getOrElse(i) { 0 }
                if (av != bv) return av - bv
            }
            return 0
        }

        private fun parseVersion(raw: String): List<Int> {
            // Strip a leading `v` and split on `.` and `-b` so `1.0.0-b3`
            // parses to [1,0,0,3] (beta builds compare *below* stable of the
            // same triple because the missing 4th slot defaults to 0).
            val cleaned = raw.removePrefix("v").removePrefix("r")
            return cleaned.split('.', '-', 'b')
                .filter { it.isNotBlank() }
                .map { it.toIntOrNull() ?: 0 }
        }
    }

    @Serializable
    private data class GitHubRelease(
        @SerialName("tag_name") val tagName: String,
        val name: String = "",
        val body: String? = null,
        val draft: Boolean = false,
        val prerelease: Boolean = false,
        @SerialName("published_at") val publishedAt: String? = null,
        val assets: List<GitHubAsset> = emptyList(),
    )

    @Serializable
    private data class GitHubAsset(
        val name: String,
        @SerialName("browser_download_url") val browserDownloadUrl: String,
    )
}
