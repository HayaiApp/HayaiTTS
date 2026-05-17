@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package dev.ahmedmohamed.hayaitts.ui.quickswitch

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.ahmedmohamed.hayaitts.R
import dev.ahmedmohamed.hayaitts.domain.model.InstalledVoice
import dev.ahmedmohamed.hayaitts.ui.components.displayName
import dev.ahmedmohamed.hayaitts.ui.components.identity
import org.koin.androidx.compose.koinViewModel

/**
 * Global voice-picker sheet. Hoisted by `MainActivity` so any screen can pop
 * it from its top-bar without each screen owning its own state.
 *
 * The picker groups installed voices by primary language and surfaces three
 * actions: tap a voice -> set as default + dismiss; "Manage" -> Library; "Add
 * voice" -> Browse. Selection is haptic so users feel the swap on touch.
 */
@Composable
fun VoiceQuickSwitcher(
    visible: Boolean,
    onDismiss: () -> Unit,
    onManage: () -> Unit,
    onAddVoice: () -> Unit,
    viewModel: QuickSwitchViewModel = koinViewModel(),
) {
    if (!visible) return
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val haptics = LocalHapticFeedback.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { SheetDragHandle() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Text(
                text = stringResource(R.string.quick_switch_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.quick_switch_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))

            AnimatedVisibility(
                visible = state.installed.isEmpty(),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                EmptyQuickSwitch()
            }

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                state.grouped.forEach { (locale, voices) ->
                    item(key = "header_$locale") {
                        LocaleHeader(locale)
                    }
                    items(voices, key = { it.voiceId }) { voice ->
                        VoicePickerRow(
                            voice = voice,
                            isDefault = state.defaults.values.any { it == voice.voiceId },
                            onPick = {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.pickVoice(voice)
                                onDismiss()
                            },
                        )
                    }
                }
            }

            BottomActionRow(
                onManage = onManage,
                onAddVoice = onAddVoice,
            )
        }
    }
}

@Composable
private fun LocaleHeader(locale: String) {
    Text(
        text = displayName(locale),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 4.dp),
    )
}

@Composable
private fun VoicePickerRow(
    voice: InstalledVoice,
    isDefault: Boolean,
    onPick: () -> Unit,
) {
    val identity = (voice.effectiveFamily ?: voice.family).identity()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onPick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(identity.container),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = identity.icon,
                contentDescription = null,
                tint = identity.onContainer,
            )
        }
        Spacer(Modifier.size(12.dp))
        Column(modifier = Modifier.padding(end = 8.dp)) {
            Text(voice.title, style = MaterialTheme.typography.titleSmall)
            Text(
                text = voice.languages.joinToString(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        if (isDefault) {
            Icon(
                imageVector = Icons.Outlined.CheckCircle,
                contentDescription = stringResource(R.string.quick_switch_current),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun BottomActionRow(
    onManage: () -> Unit,
    onAddVoice: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedButton(onClick = onManage, modifier = Modifier.weight(1f)) {
            Icon(Icons.Outlined.LibraryMusic, contentDescription = null)
            Spacer(Modifier.size(8.dp))
            Text(stringResource(R.string.quick_switch_manage))
        }
        FilledTonalButton(onClick = onAddVoice, modifier = Modifier.weight(1f)) {
            Icon(Icons.Outlined.Add, contentDescription = null)
            Spacer(Modifier.size(8.dp))
            Text(stringResource(R.string.quick_switch_add))
        }
    }
}

@Composable
private fun EmptyQuickSwitch() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.quick_switch_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SheetDragHandle() {
    Box(
        modifier = Modifier
            .padding(top = 12.dp, bottom = 4.dp)
            .size(width = 36.dp, height = 4.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.outlineVariant),
    )
}
