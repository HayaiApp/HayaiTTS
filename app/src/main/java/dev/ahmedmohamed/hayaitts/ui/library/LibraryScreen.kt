@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package dev.ahmedmohamed.hayaitts.ui.library

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.ahmedmohamed.hayaitts.R
import dev.ahmedmohamed.hayaitts.domain.model.InstalledVoice
import dev.ahmedmohamed.hayaitts.ui.components.EmptyState
import dev.ahmedmohamed.hayaitts.ui.components.InstalledVoiceCard
import org.koin.androidx.compose.koinViewModel

/**
 * The home tab. Lists installed voices and surfaces a single thumb-reach FAB to
 * jump into Browse. An empty list switches the body to a centered empty-state
 * with the same Browse CTA. A secondary top-bar action opens SAF for the Phase
 * 6 custom-voice import wizard.
 */
@Composable
fun LibraryScreen(
    onBrowse: () -> Unit,
    onVoiceClick: (voiceId: String) -> Unit,
    onImport: (rawUri: String) -> Unit,
    viewModel: LibraryViewModel = koinViewModel(),
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var pendingUninstall by remember { mutableStateOf<InstalledVoice?>(null) }

    val pickFile = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        // The user can cancel without picking; uri is null in that case.
        if (uri != null) onImport(uri.toString())
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.library_title)) },
                actions = {
                    IconButton(
                        onClick = {
                            // Broad MIME filter: tar.bz2 has no canonical
                            // MIME, zip has its own, bare .onnx files surface
                            // as octet-stream. We pass all three plus `*/*`
                            // as a fallback so file pickers that whitelist
                            // strictly still let the user through.
                            pickFile.launch(IMPORT_MIME_TYPES)
                        },
                    ) {
                        Icon(
                            Icons.Outlined.UploadFile,
                            contentDescription = stringResource(R.string.library_import_action),
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = { Text(stringResource(R.string.library_browse_action)) },
                icon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                onClick = onBrowse,
            )
        },
    ) { innerPadding ->
        // Phase 7 motion: fade between Empty and Populated so the first install
        // animates the list in rather than swapping the layout instantly.
        AnimatedContent(
            targetState = state.installed.isEmpty(),
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "library-empty",
        ) { isEmpty ->
            if (isEmpty) {
                EmptyState(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    icon = Icons.Outlined.LibraryMusic,
                    title = stringResource(R.string.library_empty_title),
                    subtitle = stringResource(R.string.library_empty_subtitle),
                    ctaLabel = stringResource(R.string.library_browse_action),
                    onCta = onBrowse,
                )
            } else {
                LibraryList(
                    voices = state.installed,
                    defaultedLocalesFor = { state.defaultedLocales(it.voiceId) },
                    onToggleDefault = { voice, locale ->
                        viewModel.toggleDefault(locale, voice.voiceId)
                    },
                    onUninstall = { pendingUninstall = it },
                    onClickVoice = { onVoiceClick(it.voiceId) },
                    contentPadding = innerPadding,
                )
            }
        }
    }

    val target = pendingUninstall
    if (target != null) {
        AlertDialog(
            onDismissRequest = { pendingUninstall = null },
            title = { Text(stringResource(R.string.uninstall_confirm_title)) },
            text = { Text(stringResource(R.string.uninstall_confirm_message)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.uninstall(target.voiceId)
                    pendingUninstall = null
                }) {
                    Text(stringResource(R.string.action_uninstall))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingUninstall = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

@Composable
private fun LibraryList(
    voices: List<InstalledVoice>,
    defaultedLocalesFor: (InstalledVoice) -> Set<String>,
    onToggleDefault: (InstalledVoice, String) -> Unit,
    onUninstall: (InstalledVoice) -> Unit,
    onClickVoice: (InstalledVoice) -> Unit,
    contentPadding: PaddingValues,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding(),
            bottom = contentPadding.calculateBottomPadding() + 96.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(items = voices, key = { it.voiceId }) { voice ->
            InstalledVoiceCard(
                voice = voice,
                defaultedLocales = defaultedLocalesFor(voice),
                onClick = { onClickVoice(voice) },
                onToggleDefault = { locale -> onToggleDefault(voice, locale) },
                onUninstall = { onUninstall(voice) },
                modifier = Modifier,
            )
        }
    }
}

private val IMPORT_MIME_TYPES = arrayOf(
    "application/x-tar",
    "application/x-bzip2",
    "application/zip",
    "application/octet-stream",
    "*/*",
)
