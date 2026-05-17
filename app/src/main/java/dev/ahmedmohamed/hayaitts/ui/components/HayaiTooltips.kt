@file:OptIn(ExperimentalMaterial3Api::class)

package dev.ahmedmohamed.hayaitts.ui.components

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.RichTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable

/**
 * Thin wrapper that anchors an M3 [RichTooltip] to any composable. Long-press
 * triggers the tooltip (default [TooltipBox] behaviour) — we never auto-show.
 */
@Composable
fun HayaiRichTooltipBox(
    title: String,
    description: String? = null,
    content: @Composable () -> Unit,
) {
    val state = rememberTooltipState(isPersistent = false)
    TooltipBox(
        positionProvider = TooltipDefaults.rememberRichTooltipPositionProvider(),
        tooltip = {
            RichTooltip(
                title = { Text(title) },
                text = {
                    if (description != null) Text(description)
                },
            )
        },
        state = state,
        content = content,
    )
}
