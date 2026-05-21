@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package dev.ahmedmohamed.hayaitts.ui.detail

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Female
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.ahmedmohamed.hayaitts.R
import kotlinx.coroutines.launch
import dev.ahmedmohamed.hayaitts.domain.model.DownloadState
import dev.ahmedmohamed.hayaitts.domain.model.Speaker
import dev.ahmedmohamed.hayaitts.ui.components.DownloadProgress
import dev.ahmedmohamed.hayaitts.ui.components.FamilyChip
import dev.ahmedmohamed.hayaitts.ui.components.HayaiRichTooltipBox
import dev.ahmedmohamed.hayaitts.ui.components.LanguageChip
import dev.ahmedmohamed.hayaitts.ui.components.TierChip
import dev.ahmedmohamed.hayaitts.ui.components.identityOrDefault
import dev.ahmedmohamed.hayaitts.ui.speaker.SpeakerPickerActivity
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Per-voice detail screen reached from Browse or Library.
 *
 * Layout (top-down):
 *   1. [LargeFlexibleTopAppBar] — back + title + subtitle. Actions slot holds
 *      Quick-Switch + a 3-dot overflow with Playground / Uninstall.
 *   2. Hero block — family glyph, title, chip strip, license caption.
 *   3. Speakers row.
 *   4. Preview block — waveform + text + play/stop.
 *   5. Optional "Use as default for X" grouped section (only when installed
 *      and the voice ships multiple locales).
 *   6. Sticky-feeling bottom [PrimaryAction] — one button that switches
 *      between Install / Cancel-download / Set-default / Coming-soon based
 *      on state.
 *
 * Every secondary affordance (Playground, Uninstall, Quick Switch) is in the
 * top-bar's actions slot so the body has one and only one primary CTA.
 */
@Composable
fun VoiceDetailScreen(
    voiceId: String,
    onBack: () -> Unit,
    onOpenQuickSwitch: () -> Unit,
    onOpenPlayground: () -> Unit = {},
) {
    val viewModel: VoiceDetailViewModel = koinViewModel { parametersOf(voiceId) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val amplitudes by viewModel.previewAmplitudes.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    DisposableEffect(Unit) { onDispose { viewModel.stop() } }

    val accent = MaterialTheme.colorScheme.primary
    val context = LocalContext.current
    val installed = state.installed
    val card = state.card
    var overflowOpen by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Text(text = installed?.title ?: card?.title ?: voiceId)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onOpenQuickSwitch) {
                        Icon(
                            Icons.Outlined.RecordVoiceOver,
                            contentDescription = stringResource(R.string.quick_switch_title),
                        )
                    }
                    Box {
                        IconButton(onClick = { overflowOpen = true }) {
                            Icon(
                                Icons.Outlined.MoreVert,
                                contentDescription = stringResource(R.string.voice_detail_actions_overflow),
                            )
                        }
                        DropdownMenu(
                            expanded = overflowOpen,
                            onDismissRequest = { overflowOpen = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.voice_detail_open_playground)) },
                                leadingIcon = {
                                    Icon(Icons.Outlined.Tune, contentDescription = null)
                                },
                                enabled = state.isInstalled,
                                onClick = {
                                    overflowOpen = false
                                    onOpenPlayground()
                                },
                            )
                            if (state.isInstalled && installed != null && !installed.bundled) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.voice_detail_uninstall_action)) },
                                    leadingIcon = {
                                        Icon(Icons.Outlined.Delete, contentDescription = null)
                                    },
                                    onClick = {
                                        overflowOpen = false
                                        viewModel.uninstall()
                                    },
                                )
                            }
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            HeroBlock(state = state)
            SpeakersSection(
                speakers = state.installed?.speakers ?: state.card?.speakers.orEmpty(),
                selectedSid = state.selectedSid,
                onPickSpeaker = viewModel::setSpeaker,
                onChooseDefault = if (state.isInstalled && (state.installed?.speakers?.size ?: 0) > 1) {
                    { context.startActivity(SpeakerPickerActivity.intent(context, voiceId)) }
                } else null,
                accent = accent,
            )
            PreviewSection(
                enabled = state.isInstalled,
                playing = state.previewing,
                amplitudes = amplitudes,
                accent = accent,
                onPlay = viewModel::play,
                onStop = viewModel::stop,
            )
            // v2: hosted sample audition. Streams a short upstream-hosted
            // clip via MediaPlayer so the user can hear the voice *before*
            // committing to the model download. Falls back to the demo URL
            // (HuggingFace Space) when no direct sample is catalogued.
            val sampleUrl = state.card?.sampleAudioUrl
            val demoUrl = state.card?.demoUrl
            if (!state.isInstalled && (sampleUrl != null || demoUrl != null)) {
                SampleAuditionRow(sampleUrl = sampleUrl, demoUrl = demoUrl, context = context)
            }
            if (state.isInstalled && installed != null) {
                DefaultLocaleSection(
                    locales = installed.languages,
                    defaulted = state.defaultedLocales(),
                    onToggleDefault = viewModel::setDefault,
                )
            }
            PrimaryAction(
                state = state,
                onInstall = viewModel::install,
                onCancel = viewModel::cancel,
            )
            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun HeroBlock(state: VoiceDetailViewModel.UiState) {
    val card = state.card
    val installed = state.installed
    val tier = card?.tierEnum ?: installed?.tier
    val family = card?.modelFamily ?: installed?.family
    val languages = card?.languages ?: installed?.languages.orEmpty()
    val license = card?.license
    val size = card?.approxSizeMb
    val sampleRate = card?.sampleRateHz ?: installed?.sampleRateHz
    val speakers = installed?.speakers?.size ?: card?.speakers?.size ?: 1

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        Text(
            text = installed?.title ?: card?.title ?: "—",
            style = MaterialTheme.typography.displaySmall,
        )
        Spacer(Modifier.height(4.dp))
        val subtitleLine = buildString {
            append(stringResource(R.string.voice_detail_speakers_count, speakers))
            if (sampleRate != null) {
                append(" · ")
                append(stringResource(R.string.voice_detail_sample_rate, sampleRate))
            }
        }
        Text(
            text = subtitleLine,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        ChipRow {
            languages.forEach { LanguageChip(it) }
            family?.let { FamilyChip(it) }
            tier?.let { TierChip(tier = it, sizeMb = size) }
        }
        if (license != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.voice_detail_license, license),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SpeakersSection(
    speakers: List<Speaker>,
    selectedSid: Int,
    onPickSpeaker: (Int) -> Unit,
    onChooseDefault: (() -> Unit)?,
    accent: Color,
) {
    if (speakers.isEmpty()) return
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.voice_detail_speakers),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            if (onChooseDefault != null) {
                TextButton(onClick = onChooseDefault) {
                    Text(stringResource(R.string.voice_choose_default_speaker))
                }
            }
        }
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(items = speakers, key = { it.id }) { sp ->
                SpeakerAvatar(
                    speaker = sp,
                    selected = sp.id == selectedSid,
                    onPick = { onPickSpeaker(sp.id) },
                    accent = accent,
                )
            }
        }
    }
}

@Composable
private fun SpeakerAvatar(
    speaker: Speaker,
    selected: Boolean,
    onPick: () -> Unit,
    accent: Color,
) {
    val ringDp: Dp by animateDpAsState(
        targetValue = if (selected) 4.dp else 0.dp,
        animationSpec = spring(stiffness = 320f),
        label = "speaker-ring",
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .then(
                    if (ringDp > 0.dp) {
                        Modifier.border(
                            width = ringDp,
                            color = accent,
                            shape = CircleShape,
                        )
                    } else Modifier,
                )
                .clickable(onClick = onPick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = when (speaker.gender.lowercase()) {
                    "f" -> Icons.Outlined.Female
                    "m" -> Icons.Outlined.Person
                    else -> Icons.Outlined.RecordVoiceOver
                },
                contentDescription = null,
                tint = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(36.dp),
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = speaker.name,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PreviewSection(
    enabled: Boolean,
    playing: Boolean,
    amplitudes: FloatArray,
    accent: Color,
    onPlay: (String) -> Unit,
    onStop: () -> Unit,
) {
    val defaultText = stringResource(R.string.voice_detail_preview_text)
    var text by remember { mutableStateOf(defaultText) }

    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.voice_detail_preview),
            style = MaterialTheme.typography.titleMedium,
        )
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            maxLines = 3,
        )
        if (!enabled) {
            Text(
                text = stringResource(R.string.voice_detail_preview_not_installed),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        WaveformBars(
            amplitudes = amplitudes,
            accent = accent,
            playing = playing,
        )
        HayaiRichTooltipBox(
            title = stringResource(R.string.tooltip_preview_play_title),
            description = stringResource(R.string.tooltip_preview_play_body),
        ) {
            FilledTonalButton(
                onClick = {
                    if (playing) onStop() else onPlay(text)
                },
                enabled = enabled && text.isNotBlank(),
            ) {
                AnimatedContent(
                    targetState = playing,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "play-icon",
                ) { isPlaying ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Outlined.Stop else Icons.Outlined.PlayArrow,
                            contentDescription = null,
                        )
                        Spacer(Modifier.size(8.dp))
                        Text(stringResource(if (isPlaying) R.string.action_stop else R.string.action_play))
                    }
                }
            }
        }
    }
}

@Composable
private fun WaveformBars(
    amplitudes: FloatArray,
    accent: Color,
    playing: Boolean,
) {
    // Matches the 32-bin RMS envelope published by VoicePreviewPlayer.amplitudes.
    // Keeping the bar count equal to the source resolution skips the downsample
    // step in WaveformBars and lets the UI render each bin 1:1.
    val bars = 32
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(bars) { i ->
            val sourceIdx = (i.toFloat() / bars * amplitudes.size).toInt().coerceIn(0, amplitudes.lastIndex.coerceAtLeast(0))
            val target = if (amplitudes.isEmpty()) 0.1f else amplitudes[sourceIdx]
            val baseline = if (playing) 0.18f else 0.08f
            val height by animateFloatAsState(
                targetValue = (target.coerceAtLeast(baseline)).coerceIn(0.05f, 1f),
                animationSpec = spring(stiffness = 600f),
                label = "wave-bar-$i",
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(height)
                    .clip(RoundedCornerShape(50))
                    .background(accent),
            )
        }
    }
}

/**
 * Groups the per-locale "Set default" toggles into one labeled section. Each
 * locale renders as a [FilterChip]-style assist chip — selected when this
 * voice is the current default for that locale.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DefaultLocaleSection(
    locales: List<String>,
    defaulted: Set<String>,
    onToggleDefault: (locale: String) -> Unit,
) {
    if (locales.isEmpty()) return
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.voice_detail_set_default_header),
            style = MaterialTheme.typography.titleMedium,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            locales.forEach { locale ->
                val isDefault = locale in defaulted
                AssistChip(
                    onClick = { onToggleDefault(locale) },
                    label = {
                        Text(
                            stringResource(
                                if (isDefault) R.string.voice_default_label
                                else R.string.voice_set_default,
                                locale,
                            ),
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
                    colors = AssistChipDefaults.assistChipColors(),
                )
            }
        }
    }
}

/**
 * One primary action button anchored at the bottom of the scroll body. The
 * label and behaviour switches between Install / Cancel-download /
 * Re-install-available / Coming-soon based on the current state. Download
 * progress (when running) is rendered above the button so we don't stack a
 * progress strip beside an action.
 */
@Composable
private fun PrimaryAction(
    state: VoiceDetailViewModel.UiState,
    onInstall: () -> Unit,
    onCancel: () -> Unit,
) {
    val ds = state.downloadState
    val running = ds is DownloadState.Running || ds is DownloadState.Extracting || ds is DownloadState.Queued
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        when {
            running -> {
                DownloadProgress(state = ds)
                Button(
                    onClick = onCancel,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Outlined.Cancel, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text(stringResource(R.string.voice_detail_cancel_primary))
                }
            }
            state.isInstalled -> {
                // Primary action is consumed by the per-locale chips above;
                // installed voices have no body-level CTA. The toolbar
                // overflow handles Uninstall and Playground.
            }
            state.card != null && state.card.available -> {
                Button(
                    onClick = onInstall,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Outlined.CloudDownload, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text(stringResource(R.string.voice_detail_install_primary))
                }
            }
            state.card != null && !state.card.available -> {
                AssistChip(
                    onClick = {},
                    enabled = false,
                    label = { Text(stringResource(R.string.voice_chip_coming_soon)) },
                )
                Text(
                    text = stringResource(R.string.voice_detail_coming_soon_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipRow(content: @Composable () -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) { content() }
}

/**
 * Pre-download sample audition. When [sampleUrl] is present we stream the
 * upstream clip via the singleton [dev.ahmedmohamed.hayaitts.data.preview.SampleAudioPlayer]
 * and toggle the same button between Play / Loading / Stop based on state.
 * When only [demoUrl] is present (no direct sample yet catalogued), we fall
 * back to opening the HuggingFace Space in the browser.
 */
@Composable
private fun SampleAuditionRow(
    sampleUrl: String?,
    demoUrl: String?,
    context: android.content.Context,
) {
    val player: dev.ahmedmohamed.hayaitts.data.preview.SampleAudioPlayer =
        org.koin.compose.koinInject()
    val playerState by player.state.collectAsStateWithLifecycle()
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose { player.stop() }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (sampleUrl != null) {
            val isThisPlaying = playerState is dev.ahmedmohamed.hayaitts.data.preview.SampleAudioPlayer.State.Playing &&
                (playerState as dev.ahmedmohamed.hayaitts.data.preview.SampleAudioPlayer.State.Playing).url == sampleUrl
            val isThisLoading = playerState is dev.ahmedmohamed.hayaitts.data.preview.SampleAudioPlayer.State.Loading &&
                (playerState as dev.ahmedmohamed.hayaitts.data.preview.SampleAudioPlayer.State.Loading).url == sampleUrl
            androidx.compose.material3.FilledTonalButton(
                onClick = {
                    if (isThisPlaying || isThisLoading) {
                        player.stop()
                    } else {
                        scope.launch { player.play(sampleUrl) }
                    }
                },
            ) {
                Icon(
                    imageVector = if (isThisPlaying || isThisLoading) {
                        Icons.Outlined.Stop
                    } else {
                        Icons.Outlined.PlayArrow
                    },
                    contentDescription = null,
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    text = stringResource(
                        if (isThisLoading) R.string.voice_detail_sample_loading
                        else if (isThisPlaying) R.string.voice_detail_sample_stop
                        else R.string.voice_detail_sample_play,
                    ),
                )
            }
        }
        if (demoUrl != null) {
            androidx.compose.material3.TextButton(
                onClick = {
                    context.startActivity(
                        android.content.Intent(
                            android.content.Intent.ACTION_VIEW,
                            android.net.Uri.parse(demoUrl),
                        ),
                    )
                },
            ) {
                Icon(
                    Icons.AutoMirrored.Outlined.OpenInNew,
                    contentDescription = null,
                )
                Spacer(Modifier.size(8.dp))
                Text(stringResource(R.string.voice_detail_open_demo))
            }
        }
    }
}
