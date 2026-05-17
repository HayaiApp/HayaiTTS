package dev.ahmedmohamed.hayaitts.domain.recommendation

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import androidx.core.content.getSystemService
import dev.ahmedmohamed.hayaitts.domain.model.Tier

/**
 * Cheap heuristic that maps the host device into one of the [Tier] buckets.
 * The Browse screen uses it to sort recommended voices first and to render a
 * "Recommended for your device" pill.
 *
 * Strategy:
 *   1. If [Build.SOC_MODEL] (API 31+) matches a known flagship SoC, return HIGH.
 *   2. Otherwise fall back to total RAM:
 *        - >= 7.5 GB on API 33+ => HIGH (covers Tensor G2+, SD8G2+, etc.)
 *        - >= 3.5 GB           => MID
 *        - < 3.5 GB            => LOW
 *
 * The RAM thresholds intentionally bracket a generation late so even a
 * conservative reading still steers Pixel 6 / Galaxy S21 class hardware to
 * HIGH. The SoC allowlist is kept small — it costs maintenance and the RAM
 * fallback catches everything that isn't on the list.
 */
fun recommendedTier(context: Context): Tier {
    val am = context.getSystemService<ActivityManager>() ?: return Tier.MID
    val mi = ActivityManager.MemoryInfo().also(am::getMemoryInfo)
    val totalMb = mi.totalMem / (1024L * 1024L)

    val socIsTopTier = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
        Build.SOC_MODEL in TOP_TIER_SOCS) ||
        (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && totalMb >= 7_500)

    return when {
        socIsTopTier -> Tier.HIGH
        totalMb >= 3_500 -> Tier.MID
        else -> Tier.LOW
    }
}

/**
 * Flagship Qualcomm / MediaTek / Tensor SoC model IDs (Android Build.SOC_MODEL,
 * available on API 31+). The list is intentionally small — the RAM fallback
 * in [recommendedTier] catches anything that ships >=7.5 GB on API 33+.
 */
private val TOP_TIER_SOCS = setOf(
    // Qualcomm Snapdragon 8 Gen 1 / 2 / 3 / 8 Elite
    "SM8450", "SM8475", "SM8550", "SM8650", "SM8750",
    // Snapdragon 8s Gen 3 + 7+ Gen 3 (flagship-killers)
    "SM7675", "SM7475",
    // MediaTek Dimensity 9000 / 9200 / 9300 / 9400
    "MT6983", "MT6985", "MT6989", "MT6991",
    // Google Tensor G2 / G3 / G4 / G5
    "Tensor G2", "Tensor G3", "Tensor G4", "Tensor G5",
)
