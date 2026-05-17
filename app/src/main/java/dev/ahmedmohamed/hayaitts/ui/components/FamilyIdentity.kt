package dev.ahmedmohamed.hayaitts.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.outlined.Piano
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import dev.ahmedmohamed.hayaitts.domain.model.ModelFamily

/**
 * Per-family visual identity used everywhere a voice card or hero is rendered.
 *
 * Each family is bound to a hand-picked seed color matching the spec — teal for
 * Piper, violet for Kokoro, etc. We expose the seed plus a paired soft container
 * tone and an icon so callers can paint a gradient + render a glyph without
 * touching MaterialTheme.colorScheme. Cards still inherit M3 Expressive surface
 * roles for borders and text; the family color only drives the soft tinted
 * background + the family icon.
 */
data class FamilyIdentity(
    val seed: Color,
    val container: Color,
    val onContainer: Color,
    val icon: ImageVector,
)

private val PiperIdentity = FamilyIdentity(
    seed = Color(0xFF0E7C86),
    container = Color(0xFFB7ECEF),
    onContainer = Color(0xFF002023),
    icon = Icons.Outlined.GraphicEq,
)

private val KokoroIdentity = FamilyIdentity(
    seed = Color(0xFF7E57C2),
    container = Color(0xFFE6DCFB),
    onContainer = Color(0xFF1F0F4A),
    icon = Icons.Outlined.AutoAwesome,
)

private val KittenIdentity = FamilyIdentity(
    seed = Color(0xFFE59C2F),
    container = Color(0xFFFCE3B7),
    onContainer = Color(0xFF3E2900),
    icon = Icons.Outlined.Pets,
)

private val VitsIdentity = FamilyIdentity(
    seed = Color(0xFF1976D2),
    container = Color(0xFFBBE0FB),
    onContainer = Color(0xFF001E33),
    icon = Icons.Outlined.WaterDrop,
)

private val MatchaIdentity = FamilyIdentity(
    seed = Color(0xFF388E3C),
    container = Color(0xFFC6E8C7),
    onContainer = Color(0xFF052A05),
    icon = Icons.Outlined.Spa,
)

private val ZipVoiceIdentity = FamilyIdentity(
    seed = Color(0xFF3F51B5),
    container = Color(0xFFCDD3F2),
    onContainer = Color(0xFF0C124A),
    icon = Icons.Outlined.Bolt,
)

private val PocketIdentity = FamilyIdentity(
    seed = Color(0xFFD81B60),
    container = Color(0xFFFBD0E0),
    onContainer = Color(0xFF3B0019),
    icon = Icons.Outlined.MusicNote,
)

private val SupertonicIdentity = FamilyIdentity(
    seed = Color(0xFFB8860B),
    container = Color(0xFFF3E1A8),
    onContainer = Color(0xFF3B2A00),
    icon = Icons.Outlined.Piano,
)

private val CustomIdentity = FamilyIdentity(
    seed = Color(0xFF455A64),
    container = Color(0xFFCFD8DC),
    onContainer = Color(0xFF13252B),
    icon = Icons.Outlined.Tune,
)

fun ModelFamily.identity(): FamilyIdentity = when (this) {
    ModelFamily.PIPER -> PiperIdentity
    ModelFamily.KOKORO -> KokoroIdentity
    ModelFamily.KITTEN -> KittenIdentity
    ModelFamily.VITS -> VitsIdentity
    ModelFamily.MATCHA -> MatchaIdentity
    ModelFamily.ZIPVOICE -> ZipVoiceIdentity
    ModelFamily.POCKET -> PocketIdentity
    ModelFamily.SUPERTONIC -> SupertonicIdentity
    ModelFamily.CUSTOM -> CustomIdentity
}

fun ModelFamily?.identityOrDefault(): FamilyIdentity =
    this?.identity() ?: FamilyIdentity(
        seed = Color(0xFF0E7C86),
        container = Color(0xFFB7ECEF),
        onContainer = Color(0xFF002023),
        icon = Icons.Outlined.RecordVoiceOver,
    )

/** Diagonal gradient from container -> a softer container tone for the hero + cards. */
@Composable
fun FamilyIdentity.cardBrush(): Brush {
    val mix = container.copy(alpha = 0.65f).compositeOverSurface()
    return Brush.linearGradient(listOf(container, mix))
}

/** Wider radial gradient anchored upper-left, used by the detail hero block. */
@Composable
fun FamilyIdentity.heroBrush(): Brush {
    val mix = container.copy(alpha = 0.55f).compositeOverSurface()
    return Brush.linearGradient(
        colors = listOf(seed.copy(alpha = 0.22f), container, mix),
    )
}

/**
 * Composites the supplied tinted color over the current MaterialTheme surface,
 * so the family chrome never darkens the page in dark mode and never bleaches
 * out in light mode. Returns a fully opaque color.
 */
@Composable
private fun Color.compositeOverSurface(): Color {
    val surface = MaterialTheme.colorScheme.surface
    val a = alpha
    val inv = 1f - a
    return Color(
        red = red * a + surface.red * inv,
        green = green * a + surface.green * inv,
        blue = blue * a + surface.blue * inv,
        alpha = 1f,
    )
}
