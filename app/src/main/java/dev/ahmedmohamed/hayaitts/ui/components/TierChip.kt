@file:OptIn(ExperimentalMaterial3Api::class)

package dev.ahmedmohamed.hayaitts.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.ahmedmohamed.hayaitts.R
import dev.ahmedmohamed.hayaitts.domain.model.Tier

/**
 * Renders the catalog "tier" + size hint as an informational [AssistChip].
 *
 * The app is strictly monochrome (see `theme/Color.kt` — error is the only
 * accent), so tiers are *not* hue-coded. We instead climb the neutral
 * surface-container ramp (Low → High = lighter → heavier container) to give a
 * subtle "this one costs more" weight cue while the label carries the actual
 * meaning. The chip is purely informational, so [onClick] is a no-op and the
 * chip is disabled — that also keeps its colors stable across light/dark.
 */
@Composable
fun TierChip(
    tier: Tier,
    sizeMb: Int?,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val (container, content) = when (tier) {
        Tier.LOW -> scheme.surfaceContainer to scheme.onSurfaceVariant
        Tier.MID -> scheme.surfaceContainerHigh to scheme.onSurface
        Tier.HIGH -> scheme.surfaceContainerHighest to scheme.onSurface
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
        border = BorderStroke(1.dp, scheme.outlineVariant),
        colors = AssistChipDefaults.assistChipColors(
            disabledContainerColor = container,
            disabledLabelColor = content,
        ),
    )
}
