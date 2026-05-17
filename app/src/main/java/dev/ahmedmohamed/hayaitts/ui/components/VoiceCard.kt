@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package dev.ahmedmohamed.hayaitts.ui.components

import androidx.compose.animation.AnimatedContent
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Recommend
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
 * Library + Browse cards. Flat M3 surfaces — every color comes from
 * `MaterialTheme.colorScheme`, never from per-family palette entries. The
 * family is communicated by a single icon glyph on a secondaryContainer
 * background, and by the FamilyChip rendered in the chip strip.
 *
 * During downloads, a [CircularWavyProgressIndicator] wraps the family badge
 * so progress is legible at the card's hero spot.
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
    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
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
                MoreMenu(canUninstall = !voice.bundled, onUninstall = onUninstall)
            }
            Spacer(Modifier.height(8.dp))
            ChipStrip {
                if (voice.bundled) {
                    AssistChip(
                        onClick = {},
                        enabled = false,
                        label = { Text(stringResource(R.string.voice_chip_bundled)) },
                    )
                }
                if (voice.family == ModelFamily.CUSTOM) {
                    SuggestionChip(
                        onClick = {},
                        label = { Text(stringResource(R.string.custom_chip)) },
                        icon = { Icon(Icons.Outlined.Tune, contentDescription = null) },
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
     * "Recommended for your device" chip.
     */
    recommendedTier: Tier? = null,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = onOpen,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
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

private enum class ActionState { Idle, Running, Installed, Unavailable }

/**
 * Leading-slot circular family badge in secondaryContainer with an icon tint
 * of onSecondaryContainer. During downloads, a wavy circular progress arc
 * wraps the badge.
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

    Box(modifier = Modifier.size(size + 8.dp), contentAlignment = Alignment.Center) {
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
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = identity.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size((size.value * 0.55f).dp),
            )
        }
    }
}

/**
 * Featured card used in the Library carousel. Flat tonal card, family glyph
 * inside a circular secondaryContainer badge, title + first language below.
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
    Card(
        modifier = modifier.size(width = 168.dp, height = 168.dp),
        shape = RoundedCornerShape(24.dp),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = identity.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
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
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp),
                    )
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
