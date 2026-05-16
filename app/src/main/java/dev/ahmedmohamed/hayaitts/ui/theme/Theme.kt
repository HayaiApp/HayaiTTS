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
import androidx.compose.ui.platform.LocalContext

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

// material3 1.5.0-alpha18 promoted `expressiveLightColorScheme()` to public API
// but did NOT promote a dark counterpart — `expressiveDarkColorScheme` is still
// `internal` in that release. Fall back to the standard `darkColorScheme()` for
// dark mode; the rest of the design line (motion, shapes, expressive
// components) still flows through `MaterialExpressiveTheme` below.
private val DarkScheme: ColorScheme = darkColorScheme(
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
    dynamicColor: Boolean = true,
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
        content = content,
    )
}
