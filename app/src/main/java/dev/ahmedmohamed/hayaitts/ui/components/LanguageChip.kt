@file:OptIn(ExperimentalMaterial3Api::class)

package dev.ahmedmohamed.hayaitts.ui.components

import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import java.util.Locale

/**
 * Small assist chip that renders a BCP-47 language tag as its human-readable
 * display name (e.g. `"en-US"` -> `"English (United States)"`). Falls back to
 * the raw tag if [Locale.forLanguageTag] produces an empty display name.
 */
@Composable
fun LanguageChip(
    tag: String,
    modifier: Modifier = Modifier,
) {
    val display = remember(tag) { displayName(tag) }
    AssistChip(
        onClick = {},
        enabled = false,
        modifier = modifier,
        label = { Text(display) },
        colors = AssistChipDefaults.assistChipColors(
            disabledContainerColor = MaterialTheme.colorScheme.secondaryContainer,
            disabledLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
    )
}

internal fun displayName(tag: String): String {
    val locale = runCatching { Locale.forLanguageTag(tag) }.getOrNull()
        ?: return tag
    val raw = locale.getDisplayName(Locale.getDefault())
    return raw.ifBlank { tag }
}
