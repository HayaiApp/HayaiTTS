@file:OptIn(ExperimentalMaterial3Api::class)

package dev.ahmedmohamed.hayaitts.ui.components

import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import dev.ahmedmohamed.hayaitts.R
import dev.ahmedmohamed.hayaitts.domain.model.Tier

/**
 * Renders the catalog "tier" + size hint as a colored [AssistChip]. We hand-pick
 * container colors per tier (green = low, amber = mid, red-ish = high) so the
 * download cost is legible at a glance without leaning on the dynamic palette.
 *
 * The chip is purely informational, so [onClick] is a no-op and the chip is
 * marked as disabled — that keeps the colors stable across light/dark.
 */
@Composable
fun TierChip(
    tier: Tier,
    sizeMb: Int?,
    modifier: Modifier = Modifier,
) {
    val (container, content) = when (tier) {
        Tier.LOW -> Color(0xFF1B5E20) to Color(0xFFC8E6C9)
        Tier.MID -> Color(0xFF5D4037) to Color(0xFFFFE0B2)
        Tier.HIGH -> Color(0xFF8C1D18) to Color(0xFFFFDAD6)
    }
    val tierLabel = stringResource(
        when (tier) {
            Tier.LOW -> R.string.browse_tier_low
            Tier.MID -> R.string.browse_tier_mid
            Tier.HIGH -> R.string.browse_tier_high
        },
    )
    val label = if (sizeMb != null) {
        "$tierLabel · ${stringResource(R.string.tier_size_label, sizeMb)}"
    } else tierLabel

    AssistChip(
        onClick = {},
        enabled = false,
        modifier = modifier,
        label = { Text(label) },
        colors = AssistChipDefaults.assistChipColors(
            disabledContainerColor = container,
            disabledLabelColor = content,
        ),
    )
}
