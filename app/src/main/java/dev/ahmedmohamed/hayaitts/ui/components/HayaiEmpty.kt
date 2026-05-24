@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package dev.ahmedmohamed.hayaitts.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.ahmedmohamed.hayaitts.ui.theme.Spacing

/**
 * The one and only empty / loading / error component used across HayaiTTS.
 *
 * M3 Expressive characteristics:
 *  - Hero shape is a slowly-morphing rounded polygon (one corner radius is
 *    animated continuously between two values, giving the surface a "live"
 *    breath without inviting a fake loading spinner).
 *  - Typography uses `headlineSmall` + `bodyLarge` (Expressive emphasis).
 *  - The CTA is a primary `Button` so the action is unambiguous.
 *
 * Three modes ([HayaiEmptyMode]) cover every empty / loading / error
 * scenario in the app. Subsurfaces (e.g. a single empty section inside a
 * larger screen) should not roll their own variants.
 */
sealed interface HayaiEmptyMode {
    data class Empty(
        val icon: ImageVector,
        val title: String,
        val subtitle: String,
        val cta: Pair<String, () -> Unit>? = null,
    ) : HayaiEmptyMode

    data class Loading(
        val label: String,
    ) : HayaiEmptyMode

    data class Error(
        val icon: ImageVector,
        val title: String,
        val subtitle: String,
        val retry: Pair<String, () -> Unit>? = null,
    ) : HayaiEmptyMode
}

@Composable
fun HayaiEmpty(
    mode: HayaiEmptyMode,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        when (mode) {
            is HayaiEmptyMode.Empty -> EmptyContent(mode)
            is HayaiEmptyMode.Loading -> LoadingContent(mode)
            is HayaiEmptyMode.Error -> ErrorContent(mode)
        }
    }
}

@Composable
private fun EmptyContent(mode: HayaiEmptyMode.Empty) {
    MorphingHero(
        icon = mode.icon,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        iconColor = MaterialTheme.colorScheme.onSurface,
    )
    Spacer(Modifier.height(Spacing.sectionVertical))
    Text(
        text = mode.title,
        style = MaterialTheme.typography.headlineSmall,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(8.dp))
    Text(
        text = mode.subtitle,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
    val cta = mode.cta
    if (cta != null) {
        Spacer(Modifier.height(Spacing.sectionVertical))
        Button(onClick = cta.second) {
            Text(cta.first)
        }
    }
}

@Composable
private fun LoadingContent(mode: HayaiEmptyMode.Loading) {
    ContainedLoadingIndicator()
    Spacer(Modifier.height(Spacing.sectionVertical))
    Text(
        text = mode.label,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun ErrorContent(mode: HayaiEmptyMode.Error) {
    MorphingHero(
        icon = mode.icon,
        containerColor = MaterialTheme.colorScheme.errorContainer,
        iconColor = MaterialTheme.colorScheme.onErrorContainer,
    )
    Spacer(Modifier.height(Spacing.sectionVertical))
    Text(
        text = mode.title,
        style = MaterialTheme.typography.headlineSmall,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(8.dp))
    Text(
        text = mode.subtitle,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
    val retry = mode.retry
    if (retry != null) {
        Spacer(Modifier.height(Spacing.sectionVertical))
        Button(onClick = retry.second) {
            Text(retry.first)
        }
    }
}

/**
 * Slowly-rotating, slowly-morphing rounded polygon hero. The shape never
 * stops moving but moves so slowly (~6s per full corner cycle, ~14s per
 * rotation) that it reads as "alive" rather than "loading" — the distinction
 * that mattered to the original brief.
 */
@Composable
private fun MorphingHero(
    icon: ImageVector,
    containerColor: androidx.compose.ui.graphics.Color,
    iconColor: androidx.compose.ui.graphics.Color,
) {
    val transition = rememberInfiniteTransition(label = "empty-morph")
    val cornerPct by transition.animateFloat(
        initialValue = 28f,
        targetValue = 48f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6_000),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "empty-morph-corner",
    )
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 14_000),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "empty-morph-angle",
    )
    Box(
        modifier = Modifier
            .size(144.dp)
            .rotate(angle)
            .clip(RoundedCornerShape(cornerPct))
            .background(containerColor),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(64.dp),
        )
    }
}
