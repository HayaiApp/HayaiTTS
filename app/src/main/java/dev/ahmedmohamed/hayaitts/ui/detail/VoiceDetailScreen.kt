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
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
            LargeFlexibleTopAppBar(
                title = {
                    Text(text = installed?.title ?: card?.title ?: voiceId)
                },
                subtitle = {
                    val languages = card?.languages ?: installed?.languages.orEmpty()
                    Text(text = languages.joinToString())
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
    val identity = family.identityOrDefault()
    val languages = card?.languages ?: installed?.languages.orEmpty()
    val license = card?.license
    val size = card?.approxSizeMb
    val sampleRate = card?.sampleRateHz ?: installed?.sampleRateHz
    val speakers = installed?.speakers?.size ?: card?.speakers?.size ?: 1

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(20.dp),
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = identity.icon,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
                Spacer(Modifier.size(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = installed?.title ?: card?.title ?: "—",
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Text(
                        text = stringResource(
                            R.string.voice_detail_speakers_count,
                            speakers,
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (sampleRate != null) {
                        Text(
                            text = stringResource(R.string.voice_detail_sample_rate, sampleRate),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
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
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
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
    val bars = 20
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
                    colors = AssistChipDefaults.assistChipColors(
                        disabledContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        disabledLabelColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    ),
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
