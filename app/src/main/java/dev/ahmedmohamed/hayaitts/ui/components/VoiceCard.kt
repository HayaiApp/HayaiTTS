@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package dev.ahmedmohamed.hayaitts.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Recommend
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.ahmedmohamed.hayaitts.R
import dev.ahmedmohamed.hayaitts.domain.model.DownloadState
import dev.ahmedmohamed.hayaitts.domain.model.InstalledVoice
import dev.ahmedmohamed.hayaitts.domain.model.ModelFamily
import dev.ahmedmohamed.hayaitts.domain.model.Tier
import dev.ahmedmohamed.hayaitts.domain.model.VoiceCard

/**
 * Library + Browse cards. Each card is anchored to a per-family identity
 * (color seed + icon + soft gradient) so different model families read as
 * visually distinct without changing the M3 surface palette.
 *
 * The family badge in the leading slot morphs shape during the download
 * lifecycle: Pill (idle) -> Cookie9Sided (running) -> Pill (done). When the
 * card is downloading, a [CircularWavyProgressIndicator] wraps the badge so
 * progress is legible at the card's hero spot. This is the "shape morphing"
 * the spec calls for — applied to a deterministic-sized leading badge instead
 * of the whole card outline, which would clip the content as it morphs.
 */

/** Library variant: an installed voice with default-locale + uninstall affordances. */
@Composable
fun InstalledVoiceCard(
    voice: InstalledVoice,
    defaultedLocales: Set<String>,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onToggleDefault: (locale: String) -> Unit,
    onToggleFavorite: () -> Unit,
    onUninstall: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val family = voice.effectiveFamily ?: voice.family
    val identity = family.identity()
    val brush = identity.cardBrush()
    val shape = RoundedCornerShape(28.dp)

    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        onClick = onClick,
        colors = CardDefaults.elevatedCardColors(
            // Solid container is overridden by the gradient overlay; keep
            // M3 surface so the elevation shadow still tints correctly.
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(brush)
                .padding(16.dp),
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FamilyBadge(family = family, downloadState = DownloadState.Done)
                    Spacer(Modifier.size(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(voice.title, style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = subtitleFor(voice),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    FavoriteToggle(isFavorite = isFavorite, onToggle = onToggleFavorite)
                    MoreMenu(
                        canUninstall = !voice.bundled,
                        onUninstall = onUninstall,
                    )
                }
                Spacer(Modifier.height(8.dp))
                ChipStrip {
                    if (voice.bundled) {
                        AssistChip(
                            onClick = {},
                            enabled = false,
                            label = { Text(stringResource(R.string.voice_chip_bundled)) },
                            colors = AssistChipDefaults.assistChipColors(
                                disabledContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                disabledLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            ),
                        )
                    }
                    if (voice.family == ModelFamily.CUSTOM) {
                        SuggestionChip(
                            onClick = {},
                            label = { Text(stringResource(R.string.custom_chip)) },
                            icon = { Icon(identity.icon, contentDescription = null) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                labelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                iconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            ),
                        )
                    }
                    voice.languages.forEach { LanguageChip(it) }
                    FamilyChip(voice.effectiveFamily ?: voice.family)
                    TierChip(tier = voice.tier, sizeMb = null)
                }
                if (voice.languages.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        voice.languages.forEach { locale ->
                            val isDefault = locale in defaultedLocales
                            AssistChip(
                                onClick = { onToggleDefault(locale) },
                                label = {
                                    Text(stringResource(R.string.voice_default_label, locale))
                                },
                                leadingIcon = if (isDefault) {
                                    {
                                        Icon(
                                            Icons.Outlined.CheckCircle,
                                            contentDescription = null,
                                        )
                                    }
                                } else null,
                                colors = if (isDefault) {
                                    AssistChipDefaults.assistChipColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        labelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                        leadingIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    )
                                } else AssistChipDefaults.assistChipColors(),
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Browse variant: a catalog entry with state-driven action button. */
@Composable
fun CatalogVoiceCard(
    card: VoiceCard,
    downloadState: DownloadState,
    isInstalled: Boolean,
    onOpen: () -> Unit,
    onInstall: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    /**
     * If non-null and equal to `card.tierEnum`, the card renders a primary
     * "Recommended for your device" chip. Surfaced by Browse on cards that
     * match the heuristic device tier and are not yet installed.
     */
    recommendedTier: Tier? = null,
) {
    val identity = card.modelFamily.identity()
    val brush = identity.cardBrush()
    val shape = RoundedCornerShape(24.dp)

    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        onClick = onOpen,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(brush)
                .padding(16.dp),
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FamilyBadge(family = card.modelFamily, downloadState = downloadState)
                    Spacer(Modifier.size(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(card.title, style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = card.subtitle(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                ChipStrip {
                    card.languages.forEach { LanguageChip(it) }
                    FamilyChip(card.modelFamily)
                    TierChip(tier = card.tierEnum, sizeMb = card.approxSizeMb)
                    val showRecommended = recommendedTier != null &&
                        card.tierEnum == recommendedTier &&
                        !isInstalled &&
                        card.available
                    if (showRecommended) {
                        SuggestionChip(
                            onClick = {},
                            label = { Text(stringResource(R.string.voice_chip_recommended)) },
                            icon = {
                                Icon(
                                    imageVector = Icons.Outlined.Recommend,
                                    contentDescription = null,
                                )
                            },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                labelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                iconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            ),
                        )
                    }
                    if (!card.available) {
                        AssistChip(
                            onClick = {},
                            enabled = false,
                            label = { Text(stringResource(R.string.voice_chip_coming_soon)) },
                            colors = AssistChipDefaults.assistChipColors(
                                disabledContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                disabledLabelColor = MaterialTheme.colorScheme.onTertiaryContainer,
                            ),
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))

                val running = downloadState is DownloadState.Running ||
                    downloadState is DownloadState.Extracting ||
                    downloadState is DownloadState.Queued
                AnimatedContent(
                    targetState = when {
                        !card.available -> ActionState.Unavailable
                        isInstalled -> ActionState.Installed
                        running -> ActionState.Running
                        else -> ActionState.Idle
                    },
                    transitionSpec = {
                        (fadeIn(spring()) togetherWith fadeOut(tween(200)))
                    },
                    label = "catalog-action",
                ) { actionState ->
                    when (actionState) {
                        ActionState.Unavailable -> OutlinedButton(
                            onClick = {},
                            enabled = false,
                        ) { Text(stringResource(R.string.voice_chip_coming_soon)) }

                        ActionState.Installed -> OutlinedButton(onClick = onOpen) {
                            Icon(Icons.Outlined.CheckCircle, contentDescription = null)
                            Spacer(Modifier.size(8.dp))
                            Text(stringResource(R.string.action_installed))
                        }

                        ActionState.Running -> Column {
                            DownloadProgress(state = downloadState)
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(onClick = onCancel) {
                                Icon(Icons.Outlined.Cancel, contentDescription = null)
                                Spacer(Modifier.size(8.dp))
                                Text(stringResource(R.string.action_cancel))
                            }
                        }

                        ActionState.Idle -> FilledTonalButton(onClick = onInstall) {
                            Icon(Icons.Outlined.CloudDownload, contentDescription = null)
                            Spacer(Modifier.size(8.dp))
                            Text(stringResource(R.string.action_install))
                        }
                    }
                }
            }
        }
    }
}

private enum class ActionState { Idle, Running, Installed, Unavailable }

/**
 * Leading-slot family icon. Shape morphs Pill -> Cookie9Sided -> Pill across
 * the download lifecycle, and a wavy circular progress arc wraps the badge
 * while a download is running. Idle/Done states show just the badge.
 */
@Composable
fun FamilyBadge(
    family: ModelFamily,
    downloadState: DownloadState,
    size: Dp = 48.dp,
) {
    val identity = family.identity()
    val isRunning = downloadState is DownloadState.Running ||
        downloadState is DownloadState.Extracting ||
        downloadState is DownloadState.Queued

    val morphTarget = if (isRunning) 1f else 0f
    val morph by animateFloatAsState(
        targetValue = morphTarget,
        animationSpec = spring(stiffness = 300f),
        label = "badge-morph",
    )
    // Compose between a Pill (RoundedPolygon) and a Cookie9 polygon. Both are
    // M3 Expressive `MaterialShapes`; we lerp by alpha-blending two clipped
    // backgrounds so we don't need an animated RoundedPolygon Morph (which
    // material3 alpha18 does not expose publicly).
    val pillShape: Shape = MaterialShapes.Pill.toShape()
    val cookieShape: Shape = MaterialShapes.Cookie9Sided.toShape()
    val activeShape: Shape = if (morph > 0.5f) cookieShape else pillShape

    Box(modifier = Modifier.size(size + 8.dp), contentAlignment = Alignment.Center) {
        // Circular wavy progress arc wraps the badge while downloading.
        AnimatedContent(
            targetState = isRunning,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "badge-progress",
        ) { running ->
            if (running) {
                if (downloadState is DownloadState.Running) {
                    CircularWavyProgressIndicator(
                        progress = { downloadState.pct.coerceIn(0f, 1f) },
                        modifier = Modifier.size(size + 8.dp),
                    )
                } else {
                    CircularWavyProgressIndicator(
                        modifier = Modifier.size(size + 8.dp),
                    )
                }
            } else {
                Spacer(modifier = Modifier.size(size + 8.dp))
            }
        }

        Box(
            modifier = Modifier
                .size(size)
                .clip(activeShape)
                .background(identity.container),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = identity.icon,
                contentDescription = null,
                tint = identity.onContainer,
                modifier = Modifier.size((size.value * 0.55f).dp),
            )
        }
    }
}

/**
 * Big featured card used by the Library carousel. ~160 dp wide, family-tinted
 * gradient, family glyph and voice title. Tap = open detail.
 */
@Composable
fun FeaturedVoiceCard(
    voice: InstalledVoice,
    onClick: () -> Unit,
    onPlayPreview: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val family = voice.effectiveFamily ?: voice.family
    val identity = family.identity()
    val brush: Brush = identity.cardBrush()
    val cardWidth: Dp by animateDpAsState(targetValue = 168.dp, label = "featured-width")
    ElevatedCard(
        modifier = modifier.size(width = cardWidth, height = 168.dp),
        shape = RoundedCornerShape(24.dp),
        onClick = onClick,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(brush)
                .padding(16.dp),
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(MaterialShapes.Cookie9Sided.toShape())
                        .background(identity.seed.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = identity.icon,
                        contentDescription = null,
                        tint = identity.onContainer,
                        modifier = Modifier.size(28.dp),
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = voice.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = voice.languages.firstOrNull() ?: "—",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = onPlayPreview) {
                        Icon(
                            imageVector = Icons.Outlined.Star,
                            contentDescription = null,
                            tint = identity.onContainer,
                            modifier = Modifier
                                .scale(0.92f)
                                .size(24.dp),
                        )
                    }
                }
            }
        }
    }
}

// -- internals -----------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipStrip(content: @Composable () -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) { content() }
}

@Composable
private fun FavoriteToggle(isFavorite: Boolean, onToggle: () -> Unit) {
    IconButton(onClick = onToggle) {
        AnimatedContent(
            targetState = isFavorite,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "fav-toggle",
        ) { fav ->
            Icon(
                imageVector = if (fav) Icons.Outlined.Star else Icons.Outlined.StarBorder,
                contentDescription = stringResource(
                    if (fav) R.string.voice_unfavorite else R.string.voice_favorite,
                ),
                tint = if (fav) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun MoreMenu(
    canUninstall: Boolean,
    onUninstall: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                Icons.Outlined.MoreVert,
                contentDescription = stringResource(R.string.action_more),
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.action_uninstall)) },
                enabled = canUninstall,
                onClick = {
                    expanded = false
                    onUninstall()
                },
            )
            if (!canUninstall) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.bundled_not_uninstallable)) },
                    enabled = false,
                    onClick = {},
                )
            }
        }
    }
}

private fun VoiceCard.subtitle(): String {
    val lang = languages.firstOrNull() ?: "—"
    val family = family.replaceFirstChar { it.uppercase() }
    val speakers = if (speakers.size > 1) " · ${speakers.size} voices" else ""
    return "$lang · $family$speakers · $license"
}

private fun subtitleFor(voice: InstalledVoice): String {
    val lang = voice.languages.firstOrNull() ?: "—"
    val fam = voice.family.name.lowercase().replaceFirstChar { it.uppercase() }
    val tier = voice.tier.name.lowercase().replaceFirstChar { it.uppercase() }
    return "$lang · $fam · $tier"
}
