@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package dev.ahmedmohamed.hayaitts.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.util.Locale

/**
 * Compact dropdown for picking which language to audition on a multi-language
 * voice (e.g. Kokoro multi-lang ships en-US, zh-CN, ja-JP, etc.). Hidden when
 * the voice only has one language.
 */
@Composable
fun HayaiLanguagePickerInline(
    languages: List<String>,
    selected: String,
    onPick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (languages.size <= 1) return
    var open by remember { mutableStateOf(false) }
    OutlinedButton(onClick = { open = true }, modifier = modifier) {
        Icon(
            imageVector = Icons.Outlined.Language,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.size(8.dp))
        Text(localeLabel(selected), maxLines = 1)
        Spacer(Modifier.size(4.dp))
        Icon(
            imageVector = Icons.Outlined.KeyboardArrowDown,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
        )
    }
    DropdownMenu(
        expanded = open,
        onDismissRequest = { open = false },
    ) {
        languages.forEach { lang ->
            DropdownMenuItem(
                text = { Text(localeLabel(lang)) },
                onClick = {
                    open = false
                    onPick(lang)
                },
            )
        }
    }
}

private fun localeLabel(tag: String): String {
    val locale = Locale.forLanguageTag(tag)
    val name = locale.getDisplayName(Locale.getDefault()).ifBlank { tag }
    return name + "  ·  $tag"
}
