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
import androidx.compose.ui.graphics.vector.ImageVector
import dev.ahmedmohamed.hayaitts.domain.model.ModelFamily

/**
 * Per-family icon. v2 design language is intentionally **flat monochrome** —
 * every surface inherits its color from `MaterialTheme.colorScheme`. No
 * per-family accents, no gradients, no tints. The icon glyph IS the family's
 * visual identity; differentiation comes from typography + layout.
 */
data class FamilyIdentity(val icon: ImageVector)

fun ModelFamily.identity(): FamilyIdentity = when (this) {
    ModelFamily.PIPER -> FamilyIdentity(Icons.Outlined.GraphicEq)
    ModelFamily.KOKORO -> FamilyIdentity(Icons.Outlined.AutoAwesome)
    ModelFamily.KITTEN -> FamilyIdentity(Icons.Outlined.Pets)
    ModelFamily.VITS -> FamilyIdentity(Icons.Outlined.WaterDrop)
    ModelFamily.MATCHA -> FamilyIdentity(Icons.Outlined.Spa)
    ModelFamily.ZIPVOICE -> FamilyIdentity(Icons.Outlined.Bolt)
    ModelFamily.POCKET -> FamilyIdentity(Icons.Outlined.MusicNote)
    ModelFamily.SUPERTONIC -> FamilyIdentity(Icons.Outlined.Piano)
    ModelFamily.CUSTOM -> FamilyIdentity(Icons.Outlined.Tune)
}

fun ModelFamily?.identityOrDefault(): FamilyIdentity =
    this?.identity() ?: FamilyIdentity(Icons.Outlined.RecordVoiceOver)
