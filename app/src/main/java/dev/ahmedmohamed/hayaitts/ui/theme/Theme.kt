@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package dev.ahmedmohamed.hayaitts.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// HayaiTTS theme contract: every M3 slot is explicitly bound to a Neutral
// step from Color.kt. We deliberately do NOT compose on top of
// `expressiveLightColorScheme()` or `darkColorScheme()` defaults, because the
// inherited slots (tertiary*, secondaryContainer, surfaceContainer*, outline,
// inversePrimary, etc.) ship tinted with greens and blues. Any slot we leave
// unset would re-introduce that tint, and a stray `colorScheme.tertiary`
// reference deep in a third-party component would break the monochrome
// promise.
//
// surfaceTint is forced to Transparent: M3 raises elevated surfaces by
// blending `primary` over them, which would pull the only accent we keep
// (near-black on light, near-white on dark) into every Card / Sheet. With
// surfaceTint transparent, surfaces stay on the neutral ramp regardless of
// elevation.
//
// `error` is the one semantic accent we keep — destructive actions need a
// universally legible signal.

private val LightScheme: ColorScheme = lightColorScheme(
    primary = Neutral95,
    onPrimary = Neutral00,
    primaryContainer = Neutral20,
    onPrimaryContainer = Neutral95,
    inversePrimary = Neutral25,

    secondary = Neutral50,
    onSecondary = Neutral00,
    secondaryContainer = Neutral20,
    onSecondaryContainer = Neutral95,

    // Tertiary is identical to secondary — every leftover `colorScheme.tertiary`
    // reference resolves to the same neutral as secondary so callers we missed
    // still render monochrome.
    tertiary = Neutral50,
    onTertiary = Neutral00,
    tertiaryContainer = Neutral20,
    onTertiaryContainer = Neutral95,

    background = Neutral05,
    onBackground = Neutral95,
    surface = Neutral05,
    onSurface = Neutral95,

    surfaceVariant = Neutral15,
    onSurfaceVariant = Neutral60,
    surfaceTint = Color.Transparent,

    inverseSurface = Neutral95,
    inverseOnSurface = Neutral05,

    surfaceBright = Neutral00,
    surfaceDim = Neutral10,
    surfaceContainerLowest = Neutral00,
    surfaceContainerLow = Neutral10,
    surfaceContainer = Neutral15,
    surfaceContainerHigh = Neutral20,
    surfaceContainerHighest = Neutral25,

    outline = Neutral40,
    outlineVariant = Neutral20,

    error = ErrorLight,
    onError = OnErrorLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,

    scrim = Neutral100,
)

private val DarkScheme: ColorScheme = darkColorScheme(
    primary = Neutral05,
    onPrimary = Neutral95,
    primaryContainer = Neutral80,
    onPrimaryContainer = Neutral05,
    inversePrimary = Neutral70,

    secondary = Neutral30,
    onSecondary = Neutral95,
    secondaryContainer = Neutral80,
    onSecondaryContainer = Neutral05,

    tertiary = Neutral30,
    onTertiary = Neutral95,
    tertiaryContainer = Neutral80,
    onTertiaryContainer = Neutral05,

    background = Neutral95,
    onBackground = Neutral05,
    surface = Neutral95,
    onSurface = Neutral05,

    surfaceVariant = Neutral85,
    onSurfaceVariant = Neutral30,
    surfaceTint = Color.Transparent,

    inverseSurface = Neutral05,
    inverseOnSurface = Neutral95,

    surfaceBright = Neutral80,
    surfaceDim = Neutral100,
    surfaceContainerLowest = Neutral100,
    surfaceContainerLow = Neutral92,
    surfaceContainer = Neutral90,
    surfaceContainerHigh = Neutral85,
    surfaceContainerHighest = Neutral80,

    outline = Neutral50,
    outlineVariant = Neutral80,

    error = ErrorDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,

    scrim = Neutral100,
)

@Composable
fun HayaiTtsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    // dynamicColor is intentionally not a parameter: the design contract is
    // monochrome regardless of the system wallpaper. If we ever need wallpaper
    // theming, it goes behind a power-user setting that flips to a different
    // theme function — not back into this one.
    val colorScheme = if (darkTheme) DarkScheme else LightScheme

    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        motionScheme = MotionScheme.expressive(),
        typography = AppTypography,
        content = content,
    )
}
