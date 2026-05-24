@file:OptIn(ExperimentalMaterial3Api::class)

package dev.ahmedmohamed.hayaitts.ui.components

import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.ahmedmohamed.hayaitts.R
import dev.ahmedmohamed.hayaitts.domain.model.ModelFamily

@Composable
fun FamilyChip(
    family: ModelFamily,
    modifier: Modifier = Modifier,
) {
    val label = stringResource(family.displayRes())
    AssistChip(
        onClick = {},
        enabled = false,
        modifier = modifier,
        label = { Text(label) },
        colors = AssistChipDefaults.assistChipColors(
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            disabledLabelColor = MaterialTheme.colorScheme.onSurface,
        ),
    )
}

fun ModelFamily.displayRes(): Int = when (this) {
    ModelFamily.PIPER -> R.string.family_piper
    ModelFamily.KOKORO -> R.string.family_kokoro
    ModelFamily.VITS -> R.string.family_vits
    ModelFamily.MATCHA -> R.string.family_matcha
    ModelFamily.KITTEN -> R.string.family_kitten
    ModelFamily.ZIPVOICE -> R.string.family_zipvoice
    ModelFamily.POCKET -> R.string.family_pocket
    ModelFamily.SUPERTONIC -> R.string.family_supertonic
    ModelFamily.CUSTOM -> R.string.family_custom
}
