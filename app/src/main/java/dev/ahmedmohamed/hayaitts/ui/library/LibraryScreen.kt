@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package dev.ahmedmohamed.hayaitts.ui.library

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.Done
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Reorder
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MediumFlexibleTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.ahmedmohamed.hayaitts.R
import dev.ahmedmohamed.hayaitts.domain.model.DownloadState
import dev.ahmedmohamed.hayaitts.domain.model.InstalledVoice
import dev.ahmedmohamed.hayaitts.domain.repo.DownloadRepository
import dev.ahmedmohamed.hayaitts.ui.components.EmptyState
import dev.ahmedmohamed.hayaitts.ui.components.FeaturedVoiceCard
import dev.ahmedmohamed.hayaitts.ui.components.HayaiRichTooltipBox
import dev.ahmedmohamed.hayaitts.ui.components.InstalledVoiceCard
import dev.ahmedmohamed.hayaitts.ui.settings.SettingsActivity
import dev.ahmedmohamed.hayaitts.ui.speaker.SpeakerPickerActivity
import kotlinx.coroutines.flow.map
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

/**
 * The home tab. Hosts:
 *  - A [MediumFlexibleTopAppBar] with title + quick-switcher / import actions.
 *  - A featured carousel ([FeaturedVoiceCard]) showing up to 5 favorites /
 *    bundled / top installed voices.
 *  - A reorderable list of [InstalledVoiceCard]s. Long-press a card to flip
 *    the screen into reorder mode and drag cards into place; tap Done to save.
 *  - A bottom [HorizontalFloatingToolbar] with Browse + Import + quick switch
 *    — replaces the legacy ExtendedFloatingActionButton.
 */
@Composable
fun LibraryScreen(
    onBrowse: () -> Unit,
    onVoiceClick: (voiceId: String) -> Unit,
    onImport: (rawUri: String) -> Unit,
    onOpenQuickSwitch: () -> Unit,
    onOpenDownloads: () -> Unit = {},
    viewModel: LibraryViewModel = koinViewModel(),
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val haptics = LocalHapticFeedback.current

    // P2: count active downloads for the top-bar badge. Sourced from Koin so
    // we do not have to grow the LibraryViewModel.
    val downloadRepository: DownloadRepository = koinInject()
    val activeDownloadCount by remember(downloadRepository) {
        downloadRepository.states.map { snapshot ->
            snapshot.values.count { s ->
                s is DownloadState.Queued ||
                    s is DownloadState.Running ||
                    s is DownloadState.Extracting
            }
        }
    }.collectAsStateWithLifecycle(initialValue = 0)

    var pendingUninstall by remember { mutableStateOf<InstalledVoice?>(null) }
    var reorderMode by remember { mutableStateOf(false) }

    // Local reorder buffer. Reset whenever we exit reorder mode or the upstream
    // list changes. Persisted via `viewModel.saveVoiceOrder` on commit.
    val orderBuffer: SnapshotStateList<InstalledVoice> = remember(state.orderedInstalled, reorderMode) {
        state.orderedInstalled.toMutableStateList()
    }

    val pickFile = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri -> if (uri != null) onImport(uri.toString()) }

    val context = LocalContext.current
    val openSettings = {
        context.startActivity(Intent(context, SettingsActivity::class.java))
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MediumFlexibleTopAppBar(
                title = { Text(stringResource(R.string.library_title)) },
                subtitle = {
                    Text(
                        text = stringResource(
                            R.string.library_subtitle,
                            state.installed.size,
                        ),
                    )
                },
                actions = {
                    HayaiRichTooltipBox(
                        title = stringResource(R.string.tooltip_quick_switch_title),
                        description = stringResource(R.string.tooltip_quick_switch_body),
                    ) {
                        IconButton(onClick = onOpenQuickSwitch) {
                            Icon(
                                Icons.Outlined.SwapHoriz,
                                contentDescription = stringResource(R.string.quick_switch_title),
                            )
                        }
                    }
                    HayaiRichTooltipBox(
                        title = stringResource(R.string.tooltip_import_title),
                        description = stringResource(R.string.tooltip_import_body),
                    ) {
                        IconButton(
                            onClick = {
                                // Broad MIME filter — tar.bz2 has no canonical type;
                                // .onnx files surface as octet-stream; the */* is a
                                // last-resort for strict pickers.
                                pickFile.launch(IMPORT_MIME_TYPES)
                            },
                        ) {
                            Icon(
                                Icons.Outlined.UploadFile,
                                contentDescription = stringResource(R.string.library_import_action),
                            )
                        }
                    }
                    AnimatedVisibility(visible = state.installed.isNotEmpty()) {
                        IconButton(onClick = {
                            if (reorderMode) {
                                // Commit: persist the buffer's current order.
                                viewModel.saveVoiceOrder(orderBuffer.map { it.voiceId })
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                            reorderMode = !reorderMode
                        }) {
                            Icon(
                                imageVector = if (reorderMode) Icons.Outlined.Done else Icons.Outlined.Reorder,
                                contentDescription = stringResource(
                                    if (reorderMode) R.string.action_done else R.string.action_reorder,
                                ),
                            )
                        }
                    }
                    // Downloads entry: badge shows active count when > 0.
                    BadgedBox(
                        badge = {
                            if (activeDownloadCount > 0) {
                                Badge { Text("$activeDownloadCount") }
                            }
                        },
                    ) {
                        IconButton(onClick = onOpenDownloads) {
                            Icon(
                                Icons.Outlined.CloudDownload,
                                contentDescription = stringResource(R.string.downloads_open),
                            )
                        }
                    }
                    IconButton(onClick = openSettings) {
                        Icon(
                            Icons.Outlined.Settings,
                            contentDescription = stringResource(R.string.library_open_settings),
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
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
                LibraryBody(
                    voices = orderBuffer,
                    favorites = state.favorites,
                    defaultedLocalesFor = { state.defaultedLocales(it.voiceId) },
                    reorderMode = reorderMode,
                    onToggleDefault = { voice, locale ->
                        viewModel.toggleDefault(locale, voice.voiceId)
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    },
                    onToggleFavorite = { voice ->
                        viewModel.toggleFavorite(voice.voiceId)
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    onUninstall = { pendingUninstall = it },
                    onChooseSpeaker = { voice ->
                        context.startActivity(SpeakerPickerActivity.intent(context, voice.voiceId))
                    },
                    onClickVoice = { onVoiceClick(it.voiceId) },
                    onPlayPreview = { onVoiceClick(it.voiceId) },
                    contentPadding = innerPadding,
                )
            }
        }
    }

    LibraryFloatingActions(
        onBrowse = onBrowse,
        onQuickSwitch = onOpenQuickSwitch,
        onImport = { pickFile.launch(IMPORT_MIME_TYPES) },
        visible = !reorderMode,
    )

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
                }) { Text(stringResource(R.string.action_uninstall)) }
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
private fun LibraryBody(
    voices: SnapshotStateList<InstalledVoice>,
    favorites: Set<String>,
    defaultedLocalesFor: (InstalledVoice) -> Set<String>,
    reorderMode: Boolean,
    onToggleDefault: (InstalledVoice, String) -> Unit,
    onToggleFavorite: (InstalledVoice) -> Unit,
    onUninstall: (InstalledVoice) -> Unit,
    onChooseSpeaker: (InstalledVoice) -> Unit,
    onClickVoice: (InstalledVoice) -> Unit,
    onPlayPreview: (InstalledVoice) -> Unit,
    contentPadding: PaddingValues,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding(),
            bottom = contentPadding.calculateBottomPadding() + 120.dp,
            start = 0.dp,
            end = 0.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Featured carousel — only when we have anything to show. Empty when
        // there's a single voice (bundled-only) since "Featured" would be a
        // single-card row which feels lonely.
        if (voices.size > 1) {
            item("featured_header") {
                Text(
                    text = stringResource(R.string.library_featured),
                    style = androidx.compose.material3.MaterialTheme.typography.titleSmall,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
            item("featured_row") {
                val featured = remember(voices, favorites) {
                    voices
                        .sortedByDescending { it.voiceId in favorites }
                        .distinctBy { it.family }
                        .take(5)
                }
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                ) {
                    items(featured, key = { "feat_" + it.voiceId }) { voice ->
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn(spring()) + scaleIn(spring(stiffness = 320f)),
                        ) {
                            FeaturedVoiceCard(
                                voice = voice,
                                onClick = { onClickVoice(voice) },
                                onPlayPreview = { onPlayPreview(voice) },
                            )
                        }
                    }
                }
            }
            item("installed_header") {
                Text(
                    text = stringResource(R.string.library_installed_header),
                    style = androidx.compose.material3.MaterialTheme.typography.titleSmall,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
                )
            }
        }

        itemsIndexed(voices) { index, voice ->
            ReorderableVoiceCard(
                voice = voice,
                isFavorite = voice.voiceId in favorites,
                reorderMode = reorderMode,
                defaultedLocales = defaultedLocalesFor(voice),
                onClick = { onClickVoice(voice) },
                onToggleDefault = { locale -> onToggleDefault(voice, locale) },
                onToggleFavorite = { onToggleFavorite(voice) },
                onUninstall = { onUninstall(voice) },
                onChooseSpeaker = { onChooseSpeaker(voice) },
                onMoveUp = if (index > 0) {
                    {
                        voices.swap(index, index - 1)
                    }
                } else null,
                onMoveDown = if (index < voices.lastIndex) {
                    { voices.swap(index, index + 1) }
                } else null,
            )
        }
    }
}

/**
 * Plain `LazyListScope.items` produces non-indexed callbacks; the reorder UI
 * needs the index for swap helpers, so we wrap [items] in a small forwarder.
 */
private fun androidx.compose.foundation.lazy.LazyListScope.itemsIndexed(
    voices: SnapshotStateList<InstalledVoice>,
    itemContent: @Composable (index: Int, voice: InstalledVoice) -> Unit,
) {
    items(count = voices.size, key = { idx -> "voice_" + voices[idx].voiceId }) { idx ->
        itemContent(idx, voices[idx])
    }
}

private fun SnapshotStateList<InstalledVoice>.swap(a: Int, b: Int) {
    if (a == b || a !in indices || b !in indices) return
    val tmp = this[a]
    this[a] = this[b]
    this[b] = tmp
}

@Composable
private fun ReorderableVoiceCard(
    voice: InstalledVoice,
    isFavorite: Boolean,
    reorderMode: Boolean,
    defaultedLocales: Set<String>,
    onClick: () -> Unit,
    onToggleDefault: (locale: String) -> Unit,
    onToggleFavorite: () -> Unit,
    onUninstall: () -> Unit,
    onChooseSpeaker: () -> Unit,
    onMoveUp: (() -> Unit)?,
    onMoveDown: (() -> Unit)?,
) {
    val haptics = LocalHapticFeedback.current
    val reorderModifier = if (reorderMode) {
        Modifier.pointerInput(voice.voiceId) {
            detectDragGesturesAfterLongPress(
                onDragStart = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                },
                onDrag = { _, drag ->
                    // Threshold-based reorder: any drag > 24px in a direction
                    // triggers a swap with the neighbour. Simpler than ringing
                    // up a real reorderable LazyColumn library and matches the
                    // spec ("use Modifier.draggable since there's no first-
                    // party reorder helper").
                    if (drag.y > 24f) onMoveDown?.invoke()
                    else if (drag.y < -24f) onMoveUp?.invoke()
                },
                onDragEnd = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                },
            )
        }
    } else Modifier

    Column(modifier = Modifier
        .padding(horizontal = 16.dp)
        .then(reorderModifier),
    ) {
        InstalledVoiceCard(
            voice = voice,
            defaultedLocales = defaultedLocales,
            isFavorite = isFavorite,
            onClick = if (reorderMode) ({}) else onClick,
            onToggleDefault = onToggleDefault,
            onToggleFavorite = onToggleFavorite,
            onUninstall = onUninstall,
            onChooseSpeaker = onChooseSpeaker.takeIf { voice.speakers.size > 1 },
        )
        AnimatedVisibility(
            visible = reorderMode,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            ReorderRow(onMoveUp = onMoveUp, onMoveDown = onMoveDown)
        }
    }
}

@Composable
private fun ReorderRow(
    onMoveUp: (() -> Unit)?,
    onMoveDown: (() -> Unit)?,
) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.End,
    ) {
        FilledIconButton(onClick = { onMoveUp?.invoke() }, enabled = onMoveUp != null) {
            Icon(
                imageVector = Icons.Outlined.KeyboardArrowUp,
                contentDescription = stringResource(R.string.action_move_up),
            )
        }
        Spacer(Modifier.padding(end = 4.dp))
        FilledIconButton(onClick = { onMoveDown?.invoke() }, enabled = onMoveDown != null) {
            Icon(
                imageVector = Icons.Outlined.KeyboardArrowDown,
                contentDescription = stringResource(R.string.action_move_down),
            )
        }
    }
}

/**
 * Bottom-anchored M3 Expressive [HorizontalFloatingToolbar]. Hidden during
 * reorder so it doesn't compete with the on-card move buttons.
 */
@Composable
private fun LibraryFloatingActions(
    onBrowse: () -> Unit,
    onQuickSwitch: () -> Unit,
    onImport: () -> Unit,
    visible: Boolean,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 24.dp),
            contentAlignment = Alignment.BottomCenter,
        ) {
            HorizontalFloatingToolbar(expanded = true) {
                IconButton(onClick = onBrowse) {
                    Icon(
                        Icons.Outlined.Search,
                        contentDescription = stringResource(R.string.library_browse_action),
                    )
                }
                IconButton(onClick = onImport) {
                    Icon(
                        Icons.Outlined.UploadFile,
                        contentDescription = stringResource(R.string.library_import_action),
                    )
                }
                IconButton(onClick = onQuickSwitch) {
                    Icon(
                        Icons.Outlined.SwapHoriz,
                        contentDescription = stringResource(R.string.quick_switch_title),
                    )
                }
                IconButton(onClick = onBrowse) {
                    Icon(
                        Icons.Outlined.Add,
                        contentDescription = stringResource(R.string.library_browse_action),
                    )
                }
            }
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
