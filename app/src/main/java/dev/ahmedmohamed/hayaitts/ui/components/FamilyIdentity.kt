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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import dev.ahmedmohamed.hayaitts.domain.model.ModelFamily

/**
 * Per-family icon + accent seed. The M3 surface colors stay theme-driven; the
 * [accent] is used at low alpha for hairline borders, gradient sweeps on the
 * featured carousel, and the family badge ring on installed-voice cards.
 *
 * Accents are chosen to be distinguishable in both light and dark variants of
 * the voice-teal scheme — see [docs/ARCHITECTURE.md] for theming policy.
 */
data class FamilyIdentity(val icon: ImageVector, val accent: Color)

fun ModelFamily.identity(): FamilyIdentity = when (this) {
    ModelFamily.PIPER -> FamilyIdentity(Icons.Outlined.GraphicEq, Color(0xFF0E7C86))    // voice teal
    ModelFamily.KOKORO -> FamilyIdentity(Icons.Outlined.AutoAwesome, Color(0xFF7C4DFF)) // violet
    ModelFamily.KITTEN -> FamilyIdentity(Icons.Outlined.Pets, Color(0xFFFFB300))         // amber
    ModelFamily.VITS -> FamilyIdentity(Icons.Outlined.WaterDrop, Color(0xFF00B8D4))      // cyan
    ModelFamily.MATCHA -> FamilyIdentity(Icons.Outlined.Spa, Color(0xFF43A047))          // green
    ModelFamily.ZIPVOICE -> FamilyIdentity(Icons.Outlined.Bolt, Color(0xFF3D5AFE))       // indigo
    ModelFamily.POCKET -> FamilyIdentity(Icons.Outlined.MusicNote, Color(0xFFE91E63))    // rose
    ModelFamily.SUPERTONIC -> FamilyIdentity(Icons.Outlined.Piano, Color(0xFFFFC107))    // gold
    ModelFamily.CUSTOM -> FamilyIdentity(Icons.Outlined.Tune, Color(0xFF607D8B))         // blue-grey
}

fun ModelFamily?.identityOrDefault(): FamilyIdentity =
    this?.identity() ?: FamilyIdentity(Icons.Outlined.RecordVoiceOver, Color(0xFF0E7C86))
