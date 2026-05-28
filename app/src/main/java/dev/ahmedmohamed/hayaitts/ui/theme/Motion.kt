package dev.ahmedmohamed.hayaitts.ui.theme

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith

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

    // --- AnimatedContent transitions ----------------------------------
    //
    // M3 Expressive's default crossfade clocks in around 220ms fade-out
    // followed by a 220ms fade-in (with a 90ms overlap), which reads as
    // sluggish on a high-density mobile screen — especially for binary
    // state toggles like an icon flipping play/pause. The helpers below
    // standardise durations across the app and add lateral motion so
    // transitions feel directional rather than just dissolving in place.
    //
    // Durations sit around the 160–200ms band: long enough that the eye
    // registers the change, short enough that the next interaction can
    // start immediately afterward.

    /** Snappy crossfade for icon swaps inside a button (play/pause, etc.). */
    fun <S> swap(): AnimatedContentTransitionScope<S>.() -> ContentTransform = {
        fadeIn(tween(durationMillis = 140, easing = FastOutSlowInEasing)) togetherWith
            fadeOut(tween(durationMillis = 100, easing = FastOutSlowInEasing))
    }

    /**
     * Horizontal slide forward — incoming content enters from the right by
     * an eighth of the width while fading in, outgoing slides to the left
     * by the same amount while fading out. Right for any forward-looking
     * state change (idle → running, empty → results, "next pane").
     */
    fun <S> slideForward(): AnimatedContentTransitionScope<S>.() -> ContentTransform = {
        (slideInHorizontally(tween(200, easing = FastOutSlowInEasing)) { it / 8 } +
            fadeIn(tween(160, easing = FastOutSlowInEasing)))
            .togetherWith(
                slideOutHorizontally(tween(160, easing = FastOutSlowInEasing)) { -it / 8 } +
                    fadeOut(tween(120, easing = FastOutSlowInEasing)),
            )
    }

    /** Mirror of [slideForward] for backward transitions (results → empty, etc.). */
    fun <S> slideBack(): AnimatedContentTransitionScope<S>.() -> ContentTransform = {
        (slideInHorizontally(tween(200, easing = FastOutSlowInEasing)) { -it / 8 } +
            fadeIn(tween(160, easing = FastOutSlowInEasing)))
            .togetherWith(
                slideOutHorizontally(tween(160, easing = FastOutSlowInEasing)) { it / 8 } +
                    fadeOut(tween(120, easing = FastOutSlowInEasing)),
            )
    }

    /**
     * Vertical lift — incoming content slides up from a sixth of its height
     * while fading in. Good for "expanded" state changes like a progress
     * row appearing beneath an action button.
     */
    fun <S> slideLift(): AnimatedContentTransitionScope<S>.() -> ContentTransform = {
        (slideInVertically(tween(200, easing = FastOutSlowInEasing)) { it / 6 } +
            fadeIn(tween(160, easing = FastOutSlowInEasing)))
            .togetherWith(
                slideOutVertically(tween(160, easing = FastOutSlowInEasing)) { -it / 6 } +
                    fadeOut(tween(120, easing = FastOutSlowInEasing)),
            )
    }
}
