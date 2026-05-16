@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package dev.ahmedmohamed.hayaitts.ui.components

import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.ahmedmohamed.hayaitts.R
import dev.ahmedmohamed.hayaitts.domain.model.DownloadState
import dev.ahmedmohamed.hayaitts.domain.model.InstalledVoice
import dev.ahmedmohamed.hayaitts.domain.model.VoiceCard

/**
 * Two-mode reusable card. The Browse list uses [Catalog] and the Library list
 * uses [Installed]; both share the same hero treatment (RecordVoiceOver icon
 * + title + chip strip) so swapping between screens does not look discontinuous.
 *
 * Card shape uses [MaterialShapes] morphing between Idle / Running / Done — the
 * Done state lands on a rounder, friendlier shape than the default rounded
 * corner. This is the small piece of M3 Expressive flavour the spec calls for.
 */
object VoiceCardComponent {
    // Marker object — keeps the file-level namespace tidy. Actual composables
    // are top-level functions below so callers can just import them.
}

/** Library variant: an installed voice with default-locale + uninstall affordances. */
@Composable
fun InstalledVoiceCard(
    voice: InstalledVoice,
    defaultedLocales: Set<String>,
    onClick: () -> Unit,
    onToggleDefault: (locale: String) -> Unit,
    onUninstall: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = morphedShape(state = ShapeState.Done)
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        onClick = onClick,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                VoiceAvatar()
                Spacer(Modifier.size(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(voice.title, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = subtitleFor(voice),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
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
                voice.languages.forEach { LanguageChip(it) }
                FamilyChip(voice.family)
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
                                Text(
                                    stringResource(R.string.voice_default_label, locale),
                                )
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
) {
    val shapeState = shapeStateFor(isInstalled, downloadState)
    val shape = morphedShape(state = shapeState)
    val cardModifier = modifier.fillMaxWidth()
    val cardColors = CardDefaults.outlinedCardColors(
        containerColor = MaterialTheme.colorScheme.surface,
    )
    OutlinedCard(
        modifier = cardModifier,
        shape = shape,
        colors = cardColors,
        onClick = onOpen,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                VoiceAvatar()
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
            }
            Spacer(Modifier.height(12.dp))

            val running = downloadState is DownloadState.Running ||
                downloadState is DownloadState.Extracting ||
                downloadState is DownloadState.Queued
            when {
                isInstalled -> {
                    OutlinedButton(onClick = onOpen, enabled = true) {
                        Icon(Icons.Outlined.CheckCircle, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text(stringResource(R.string.action_installed))
                    }
                }
                running -> {
                    DownloadProgress(state = downloadState)
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = onCancel) {
                        Icon(Icons.Outlined.Cancel, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text(stringResource(R.string.action_cancel))
                    }
                }
                else -> {
                    FilledTonalButton(onClick = onInstall) {
                        Icon(Icons.Outlined.CloudDownload, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text(stringResource(R.string.action_install))
                    }
                }
            }
        }
    }
}

// -- internals -----------------------------------------------------------------

@Composable
private fun VoiceAvatar() {
    Box(
        modifier = Modifier.size(40.dp),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.RecordVoiceOver,
            contentDescription = null,
            modifier = Modifier.size(36.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipStrip(content: @Composable () -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) { content() }
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

private enum class ShapeState { Idle, Running, Done }

private fun shapeStateFor(isInstalled: Boolean, state: DownloadState): ShapeState = when {
    isInstalled || state is DownloadState.Done -> ShapeState.Done
    state is DownloadState.Running || state is DownloadState.Extracting || state is DownloadState.Queued ->
        ShapeState.Running
    else -> ShapeState.Idle
}

/**
 * Animates the card's corner radius between three M3 Expressive presets as the
 * install lifecycle progresses. Polygon shapes from [androidx.compose.material3.MaterialShapes]
 * are intentionally avoided as container shapes here because they would clip
 * rectangular card content into a cookie/pill outline.
 */
@Composable
private fun morphedShape(state: ShapeState): Shape {
    val target: Dp = when (state) {
        ShapeState.Idle -> 16.dp
        ShapeState.Running -> 24.dp
        ShapeState.Done -> 28.dp
    }
    val radius by animateDpAsState(targetValue = target, label = "voice-card-radius")
    return remember(radius) { RoundedCornerShape(radius) }
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
