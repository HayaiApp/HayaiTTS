@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package dev.ahmedmohamed.hayaitts.ui.studio

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.ahmedmohamed.hayaitts.R
import dev.ahmedmohamed.hayaitts.ui.components.HayaiEmpty
import dev.ahmedmohamed.hayaitts.ui.components.HayaiEmptyMode
import dev.ahmedmohamed.hayaitts.ui.library.LibraryViewModel
import dev.ahmedmohamed.hayaitts.ui.playground.PlaygroundScreen
import org.koin.androidx.compose.koinViewModel

/**
 * v2 Studio tab. Hosts the Playground for one installed voice at a time,
 * with a swap-icon affordance in the top bar that opens the quick-switcher
 * sheet so users can change which voice they're tuning without leaving
 * Studio. The first installed voice is the initial pick.
 */
@Composable
fun StudioScreen(
    onBack: () -> Unit,
    onOpenQuickSwitch: () -> Unit,
) {
    val libraryVm: LibraryViewModel = koinViewModel()
    val state by libraryVm.uiState.collectAsStateWithLifecycle()
    val firstVoiceId = state.orderedInstalled.firstOrNull()?.voiceId
        ?: state.installed.firstOrNull()?.voiceId

    if (firstVoiceId == null) {
        StudioEmpty(onBrowse = onBack)
        return
    }

    // Remember the picked voice locally; reset when the first installed voice
    // identity changes (e.g. user uninstalled the currently-selected one).
    var selectedVoiceId by remember(firstVoiceId) { mutableStateOf(firstVoiceId) }

    var pickerOpen by remember { mutableStateOf(false) }
    PlaygroundScreen(
        voiceId = selectedVoiceId,
        onBack = onBack,
        extraTopBarActions = {
            // Visible model picker — tap opens an installed-voices sheet
            // local to Studio (not the shared quick-switcher, which is meant
            // for system-wide default switching). Updating `selectedVoiceId`
            // recreates the per-voice PlaygroundViewModel via the
            // parametersOf(voiceId) lookup, so the knobs reload for the new
            // voice's last-saved tuning.
            IconButton(onClick = { pickerOpen = true }) {
                Icon(
                    Icons.Outlined.SwapHoriz,
                    contentDescription = stringResource(R.string.studio_swap_voice),
                )
            }
        },
    )
    if (pickerOpen) {
        StudioVoicePicker(
            voices = state.orderedInstalled.ifEmpty { state.installed },
            selectedId = selectedVoiceId,
            onPick = { id ->
                selectedVoiceId = id
                pickerOpen = false
            },
            onDismiss = { pickerOpen = false },
        )
    }
}

@Composable
private fun StudioVoicePicker(
    voices: List<dev.ahmedmohamed.hayaitts.domain.model.InstalledVoice>,
    selectedId: String,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = androidx.compose.material3.rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
    )
    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        androidx.compose.foundation.layout.Column(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            androidx.compose.material3.Text(
                text = stringResource(R.string.studio_swap_voice),
                style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
            )
            voices.forEach { voice ->
                androidx.compose.material3.ListItem(
                    headlineContent = {
                        androidx.compose.material3.Text(voice.title)
                    },
                    supportingContent = {
                        androidx.compose.material3.Text(
                            voice.languages.joinToString(" · ") +
                                "  ·  " +
                                voice.family.name.lowercase()
                                    .replaceFirstChar { it.uppercase() },
                        )
                    },
                    trailingContent = {
                        androidx.compose.material3.RadioButton(
                            selected = voice.voiceId == selectedId,
                            onClick = { onPick(voice.voiceId) },
                        )
                    },
                    modifier = Modifier.clickable { onPick(voice.voiceId) },
                )
            }
        }
    }
}

@Composable
private fun StudioEmpty(onBrowse: () -> Unit) {
    dev.ahmedmohamed.hayaitts.ui.components.HayaiScreenChrome(
        title = stringResource(R.string.studio_title),
    ) { topInset ->
        HayaiEmpty(
            mode = HayaiEmptyMode.Empty(
                icon = Icons.Outlined.Tune,
                title = stringResource(R.string.studio_title),
                subtitle = stringResource(R.string.studio_no_voices),
                cta = stringResource(R.string.nav_browse) to onBrowse,
            ),
            modifier = Modifier
                .fillMaxSize()
                .padding(top = topInset),
        )
    }
}
