package dev.ahmedmohamed.hayaitts.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring

/**
 * Centralised motion constants for v2. Per `docs/ARCHITECTURE.md`, no screen
 * should hard-code `spring(stiffness = ...)` literals — express the *intent*
 * here and let MotionScheme drive the actual stiffness.
 *
 * M3 Expressive ships a `MotionScheme.expressive()` accessor that screens
 * pull through `LocalMotionScheme.current`; this file is the home for the
 * project-specific named variations (waveform bars, ring badges, etc.) that
 * sit on top of that base.
 */
object HayaiMotion {
    /** Quick selection/affordance state changes — chip selected, ring expand. */
    val quickSpring get() = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium,
    )

    /** Live audio-amplitude animations: tight, fast, no overshoot. */
    val waveSpring get() = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessHigh,
    )

    /** Container morphs — Card expand/collapse, FAB menu open/close. */
    val containerSpring get() = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMediumLow,
    )
}
