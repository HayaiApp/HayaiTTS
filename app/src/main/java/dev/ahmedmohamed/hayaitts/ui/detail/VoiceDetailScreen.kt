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
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.ahmedmohamed.hayaitts.R
import dev.ahmedmohamed.hayaitts.domain.model.DownloadState
import dev.ahmedmohamed.hayaitts.domain.model.Speaker
import dev.ahmedmohamed.hayaitts.ui.components.DownloadProgress
import dev.ahmedmohamed.hayaitts.ui.components.FamilyChip
import dev.ahmedmohamed.hayaitts.ui.components.LanguageChip
import dev.ahmedmohamed.hayaitts.ui.components.TierChip
import dev.ahmedmohamed.hayaitts.ui.components.heroBrush
import dev.ahmedmohamed.hayaitts.ui.components.identityOrDefault
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Per-voice detail screen reached from Browse or Library. The top half is an
 * immersive hero block: family-tinted gradient + a polygonal family glyph +
 * the voice title and chip strip. Below the hero, the preview section shows
 * a live waveform fed by [VoiceDetailViewModel.previewAmplitudes], a 32-bar
 * bar graph whose heights are RMS bins streamed in 60 ms windows during
 * playback. Speakers render as circular avatars; tapping switches the synth
 * sid via [VoiceDetailViewModel.setSpeaker].
 *
 * The screen uses [LargeFlexibleTopAppBar] (M3 Expressive's "Large" variant
 * with an optional subtitle slot + flexible collapse motion).
 */
@Composable
fun VoiceDetailScreen(
    voiceId: String,
    onBack: () -> Unit,
    onOpenQuickSwitch: () -> Unit,
) {
    val viewModel: VoiceDetailViewModel = koinViewModel { parametersOf(voiceId) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val amplitudes by viewModel.previewAmplitudes.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    DisposableEffect(Unit) { onDispose { viewModel.stop() } }

    val card = state.card
    val installed = state.installed
    val family = card?.modelFamily ?: installed?.family
    val identity = family.identityOrDefault()

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
                speakers = installed?.speakers ?: card?.speakers.orEmpty(),
                selectedSid = state.selectedSid,
                onPickSpeaker = viewModel::setSpeaker,
                accent = identity.seed,
            )
            PreviewSection(
                enabled = state.isInstalled,
                playing = state.previewing,
                amplitudes = amplitudes,
                accent = identity.seed,
                onPlay = viewModel::play,
                onStop = viewModel::stop,
            )
            ActionRow(
                state = state,
                onInstall = viewModel::install,
                onCancel = viewModel::cancel,
                onUninstall = viewModel::uninstall,
                onSetDefault = viewModel::setDefault,
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
            .background(identity.heroBrush())
            .padding(20.dp),
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(identity.seed.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = identity.icon,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = identity.onContainer,
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
    accent: Color,
) {
    if (speakers.isEmpty()) return
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.voice_detail_speakers),
            style = MaterialTheme.typography.titleMedium,
        )
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

/**
 * 20-bar waveform. Each bar height is driven by the matching amplitude bin
 * (we map the 32 RMS bins down to 20 visible bars by taking strides). Idle
 * state shows a flat baseline; the "playing" parameter just animates the
 * baseline a touch higher so the row reads as alive.
 */
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

@Composable
private fun ActionRow(
    state: VoiceDetailViewModel.UiState,
    onInstall: () -> Unit,
    onCancel: () -> Unit,
    onUninstall: () -> Unit,
    onSetDefault: (locale: String) -> Unit,
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
                OutlinedButton(onClick = onCancel) {
                    Icon(Icons.Outlined.Cancel, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text(stringResource(R.string.action_cancel))
                }
            }
            state.isInstalled -> {
                val installed = state.installed!!
                val defaulted = state.defaultedLocales()
                Text(
                    text = stringResource(R.string.voice_detail_set_default_header),
                    style = MaterialTheme.typography.titleMedium,
                )
                installed.languages.forEach { locale ->
                    val isDefault = locale in defaulted
                    FilledTonalButton(
                        onClick = { onSetDefault(locale) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(
                            imageVector = if (isDefault) Icons.Outlined.CheckCircle else Icons.Outlined.PlayArrow,
                            contentDescription = null,
                        )
                        Spacer(Modifier.size(8.dp))
                        Text(
                            stringResource(
                                if (isDefault) R.string.voice_default_label
                                else R.string.voice_set_default,
                                locale,
                            ),
                        )
                    }
                }
                if (!installed.bundled) {
                    OutlinedButton(onClick = onUninstall) {
                        Icon(Icons.Outlined.Delete, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text(stringResource(R.string.action_uninstall))
                    }
                }
            }
            state.card != null && state.card.available -> {
                FilledTonalButton(onClick = onInstall) {
                    Icon(Icons.Outlined.CloudDownload, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text(stringResource(R.string.action_install))
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
