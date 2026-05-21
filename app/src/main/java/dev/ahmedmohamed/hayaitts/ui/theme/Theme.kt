@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package dev.ahmedmohamed.hayaitts.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.expressiveLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// Material 3 ships `expressiveLightColorScheme()` in material3 1.5.0-alpha19,
// but never shipped a dark counterpart. Author the symmetric dark variant
// locally: the light version softens `on*Container` contrast by moving from
// baseline Tone 10 → Tone 30, so the dark version mirrors that with Tone 90
// → Tone 70. Our voice-teal palette in `Color.kt` already sits on the right
// tonal grid (VoiceTealDarkPrimary = Primary Tone 70), so the overrides we
// `.copy()` on top of this composite cleanly.
private fun expressiveDarkColorScheme() = darkColorScheme(
    onPrimaryContainer = Color(0xFF82D3DE),    // Primary Tone 70
    onSecondaryContainer = Color(0xFFB1CBD0),  // Secondary Tone 70
    onTertiaryContainer = Color(0xFFBACADD),   // Tertiary Tone 70
    onErrorContainer = Color(0xFFFFB4AB),      // Error Tone 70
)

private val LightScheme: ColorScheme = expressiveLightColorScheme().copy(
    primary = VoiceTealLightPrimary,
    onPrimary = VoiceTealLightOnPrimary,
    primaryContainer = VoiceTealLightPrimaryContainer,
    onPrimaryContainer = VoiceTealLightOnPrimaryContainer,
    secondary = VoiceTealLightSecondary,
    background = VoiceTealLightBackground,
    onBackground = VoiceTealLightOnBackground,
    surface = VoiceTealLightBackground,
    onSurface = VoiceTealLightOnBackground,
)

private val DarkScheme: ColorScheme = expressiveDarkColorScheme().copy(
    primary = VoiceTealDarkPrimary,
    onPrimary = VoiceTealDarkOnPrimary,
    primaryContainer = VoiceTealDarkPrimaryContainer,
    onPrimaryContainer = VoiceTealDarkOnPrimaryContainer,
    secondary = VoiceTealDarkSecondary,
    background = VoiceTealDarkBackground,
    onBackground = VoiceTealDarkOnBackground,
    surface = VoiceTealDarkBackground,
    onSurface = VoiceTealDarkOnBackground,
)

@Composable
fun HayaiTtsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // v2: dynamic color is **off by default** so the system wallpaper never
    // bleeds tints into the UI. The monochrome scheme is the design contract.
    // Callers that need wallpaper-derived theming can opt back in explicitly.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme: ColorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkScheme
        else -> LightScheme
    }

    // M3 Expressive is the active design line for material3 1.5.x. The
    // expressive theme + motion + color scheme constructors live behind the
    // `@ExperimentalMaterial3ExpressiveApi` opt-in; we honour it at the file
    // level above.
    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        motionScheme = MotionScheme.expressive(),
        typography = AppTypography,
        content = content,
    )
}
