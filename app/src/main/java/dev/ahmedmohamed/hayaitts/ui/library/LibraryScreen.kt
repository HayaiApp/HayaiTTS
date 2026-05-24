@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package dev.ahmedmohamed.hayaitts.ui.library

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.Done
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Reorder
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
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
import dev.ahmedmohamed.hayaitts.domain.model.InstalledVoice
import dev.ahmedmohamed.hayaitts.ui.components.FeaturedVoiceCard
import dev.ahmedmohamed.hayaitts.ui.components.HayaiEmpty
import dev.ahmedmohamed.hayaitts.ui.components.HayaiEmptyMode
import dev.ahmedmohamed.hayaitts.ui.components.HayaiTopBar
import dev.ahmedmohamed.hayaitts.ui.components.InstalledVoiceCard
import dev.ahmedmohamed.hayaitts.ui.speaker.SpeakerPickerActivity
import org.koin.androidx.compose.koinViewModel

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

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            HayaiTopBar(
                title = stringResource(R.string.library_title),
                actions = {
                    IconButton(onClick = onOpenQuickSwitch) {
                        Icon(
                            Icons.Outlined.SwapHoriz,
                            contentDescription = stringResource(R.string.quick_switch_title),
                        )
                    }
                    IconButton(onClick = { pickFile.launch(IMPORT_MIME_TYPES) }) {
                        Icon(
                            Icons.Outlined.UploadFile,
                            contentDescription = stringResource(R.string.library_import_action),
                        )
                    }
                    AnimatedVisibility(visible = state.installed.isNotEmpty()) {
                        IconButton(onClick = {
                            if (reorderMode) {
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
                HayaiEmpty(
                    mode = HayaiEmptyMode.Empty(
                        icon = Icons.Outlined.LibraryMusic,
                        title = stringResource(R.string.library_empty_title),
                        subtitle = stringResource(R.string.library_empty_subtitle),
                        cta = stringResource(R.string.library_browse_action) to onBrowse,
                    ),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
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
                    text = stringResource(R.string.library_featured).uppercase(),
                    style = androidx.compose.material3.MaterialTheme.typography.labelMedium.copy(
                        letterSpacing = androidx.compose.ui.unit.TextUnit(1.2f, androidx.compose.ui.unit.TextUnitType.Sp),
                    ),
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
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
                        FeaturedVoiceCard(
                            voice = voice,
                            onClick = { onClickVoice(voice) },
                            onPlayPreview = { onPlayPreview(voice) },
                            modifier = Modifier.animateItem(),
                        )
                    }
                }
            }
            item("installed_header") {
                Text(
                    text = stringResource(R.string.library_installed_header).uppercase(),
                    style = androidx.compose.material3.MaterialTheme.typography.labelMedium.copy(
                        letterSpacing = androidx.compose.ui.unit.TextUnit(1.2f, androidx.compose.ui.unit.TextUnitType.Sp),
                    ),
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 16.dp),
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
    // Accumulate drag delta so a single sustained gesture only fires one swap
    // per row-height worth of travel. Without this, every frame's delta is
    // checked against the threshold independently, which collapses the list
    // on any drag larger than 24px per frame.
    var dragAccum by remember(voice.voiceId, reorderMode) { mutableFloatStateOf(0f) }
    val reorderModifier = if (reorderMode) {
        Modifier.pointerInput(voice.voiceId) {
            detectDragGesturesAfterLongPress(
                onDragStart = {
                    dragAccum = 0f
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                },
                onDrag = { _, drag ->
                    dragAccum += drag.y
                    if (dragAccum > REORDER_THRESHOLD_PX) {
                        onMoveDown?.invoke()
                        dragAccum = 0f
                    } else if (dragAccum < -REORDER_THRESHOLD_PX) {
                        onMoveUp?.invoke()
                        dragAccum = 0f
                    }
                },
                onDragEnd = {
                    dragAccum = 0f
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

// Roughly one card-row of vertical travel. Higher than 24px so a single
// frame of inertia never accidentally swaps; low enough that intentional
// drags feel responsive.
private const val REORDER_THRESHOLD_PX = 72f

private val IMPORT_MIME_TYPES = arrayOf(
    "application/x-tar",
    "application/x-bzip2",
    "application/zip",
    "application/octet-stream",
    "*/*",
)
