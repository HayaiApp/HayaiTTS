@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package dev.ahmedmohamed.hayaitts.ui.playground

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Female
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.ahmedmohamed.hayaitts.R
import dev.ahmedmohamed.hayaitts.data.db.entities.PlaygroundSampleEntity
import dev.ahmedmohamed.hayaitts.data.playground.VoiceTuning
import dev.ahmedmohamed.hayaitts.domain.model.Speaker
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlin.math.roundToInt

/** Per-voice testing surface with speed/pitch/length sliders and bounded sample history. */
@Composable
fun PlaygroundScreen(
    voiceId: String,
    onBack: () -> Unit,
) {
    val viewModel: PlaygroundViewModel = koinViewModel { parametersOf(voiceId) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val amplitudes by viewModel.previewAmplitudes.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    DisposableEffect(Unit) { onDispose { viewModel.stop() } }

    LaunchedEffect(state.error) {
        val err = state.error
        if (err != null) {
            scope.launch { snackbarHostState.showSnackbar(err) }
            viewModel.dismissError()
        }
    }

    val accent = MaterialTheme.colorScheme.primary

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(stringResource(R.string.playground_title)) },
                subtitle = { Text(state.title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        PlaygroundBody(
            state = state, amplitudes = amplitudes, accent = accent, paddingValues = padding,
            onTextChange = viewModel::onTextChange,
            onPickSpeaker = viewModel::setSpeaker,
            onSpeedChange = viewModel::setSpeed,
            onPitchChange = viewModel::setPitch,
            onLengthChange = viewModel::setLengthScale,
            onResetSpeed = viewModel::resetSpeed,
            onResetPitch = viewModel::resetPitch,
            onResetLength = viewModel::resetLengthScale,
            onGenerate = viewModel::generate,
            onStop = viewModel::stop,
            onReplay = viewModel::replay,
            onDelete = viewModel::deleteSample,
        )
    }
}

@Composable
private fun PlaygroundBody(
    state: PlaygroundViewModel.UiState,
    amplitudes: FloatArray,
    accent: Color,
    paddingValues: PaddingValues,
    onTextChange: (String) -> Unit,
    onPickSpeaker: (Int) -> Unit,
    onSpeedChange: (Float) -> Unit,
    onPitchChange: (Float) -> Unit,
    onLengthChange: (Float) -> Unit,
    onResetSpeed: () -> Unit,
    onResetPitch: () -> Unit,
    onResetLength: () -> Unit,
    onGenerate: () -> Unit,
    onStop: () -> Unit,
    onReplay: (PlaygroundSampleEntity) -> Unit,
    onDelete: (PlaygroundSampleEntity) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(paddingValues).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(Modifier.height(4.dp))
        ComposerSection(state.text, state.isInstalled, onTextChange)
        TuningPanel(
            tuning = state.tuning, speakers = state.speakers, selectedSid = state.selectedSid,
            onPickSpeaker = onPickSpeaker,
            onSpeedChange = onSpeedChange, onPitchChange = onPitchChange, onLengthChange = onLengthChange,
            onResetSpeed = onResetSpeed, onResetPitch = onResetPitch, onResetLength = onResetLength,
            accent = accent,
        )
        GenerateRow(
            playing = state.playing, generating = state.generating,
            enabled = state.isInstalled && state.text.isNotBlank(),
            onGenerate = onGenerate, onStop = onStop,
        )
        WaveformBars(amplitudes = amplitudes, playing = state.playing, accent = accent)
        HistorySection(entries = state.history, onReplay = onReplay, onDelete = onDelete)
        if (!state.isInstalled) {
            Text(
                stringResource(R.string.playground_not_installed),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun ComposerSection(value: String, enabled: Boolean, onChange: (String) -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.playground_text_header), style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            modifier = Modifier.fillMaxWidth().heightIn(min = 144.dp),
            shape = RoundedCornerShape(20.dp),
            placeholder = { Text(stringResource(R.string.playground_text_placeholder)) },
            enabled = enabled,
            maxLines = 8,
            minLines = 5,
            supportingText = { Text("${value.length} / ${PlaygroundViewModel.MAX_INPUT}") },
        )
    }
}

@Composable
private fun TuningPanel(
    tuning: VoiceTuning,
    speakers: List<Speaker>,
    selectedSid: Int,
    onPickSpeaker: (Int) -> Unit,
    onSpeedChange: (Float) -> Unit,
    onPitchChange: (Float) -> Unit,
    onLengthChange: (Float) -> Unit,
    onResetSpeed: () -> Unit,
    onResetPitch: () -> Unit,
    onResetLength: () -> Unit,
    accent: Color,
) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.size(8.dp))
                Text(stringResource(R.string.playground_tuning_header), style = MaterialTheme.typography.titleMedium)
            }
            if (speakers.size > 1) SpeakerStrip(speakers, selectedSid, onPickSpeaker, accent)
            TuningSlider(stringResource(R.string.playground_slider_speed), tuning.speed,
                VoiceTuning.SPEED_MIN, VoiceTuning.SPEED_MAX, onSpeedChange, onResetSpeed)
            TuningSlider(stringResource(R.string.playground_slider_pitch), tuning.pitch,
                VoiceTuning.PITCH_MIN, VoiceTuning.PITCH_MAX, onPitchChange, onResetPitch,
                trailing = { PitchTooltip() })
            TuningSlider(stringResource(R.string.playground_slider_length), tuning.lengthScale,
                VoiceTuning.LENGTH_MIN, VoiceTuning.LENGTH_MAX, onLengthChange, onResetLength)
        }
    }
}

@Composable
private fun PitchTooltip() {
    val tooltipState = rememberTooltipState(isPersistent = true)
    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = { PlainTooltip { Text(stringResource(R.string.playground_pitch_tooltip)) } },
        state = tooltipState,
    ) {
        val scope = rememberCoroutineScope()
        IconButton(onClick = { scope.launch { tooltipState.show() } }) {
            Icon(Icons.Outlined.HelpOutline,
                contentDescription = stringResource(R.string.playground_pitch_tooltip),
                tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun TuningSlider(
    label: String,
    value: Float,
    min: Float,
    max: Float,
    onValueChange: (Float) -> Unit,
    onReset: () -> Unit,
    trailing: @Composable (() -> Unit)? = null,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
            trailing?.invoke()
            Text(formatMultiplier(value), style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.size(8.dp))
            TextButton(onClick = onReset) {
                Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.size(4.dp))
                Text(stringResource(R.string.playground_reset))
            }
        }
        Slider(value = value.coerceIn(min, max), onValueChange = onValueChange,
            valueRange = min..max, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun SpeakerStrip(speakers: List<Speaker>, selectedSid: Int, onPick: (Int) -> Unit, accent: Color) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(items = speakers, key = { it.id }) { sp ->
            SpeakerChip(sp, sp.id == selectedSid, { onPick(sp.id) }, accent)
        }
    }
}

@Composable
private fun SpeakerChip(speaker: Speaker, selected: Boolean, onPick: () -> Unit, accent: Color) {
    val borderWidth by animateFloatAsState(
        targetValue = if (selected) 2f else 0f,
        animationSpec = spring(stiffness = 320f), label = "speaker-chip-border",
    )
    Row(
        modifier = Modifier.clip(RoundedCornerShape(50))
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
            .then(if (borderWidth > 0f) Modifier.border(borderWidth.dp, accent, RoundedCornerShape(50)) else Modifier)
            .clickable(onClick = onPick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = when (speaker.gender.lowercase()) {
                "f" -> Icons.Outlined.Female
                "m" -> Icons.Outlined.Person
                else -> Icons.Outlined.RecordVoiceOver
            },
            contentDescription = null, modifier = Modifier.size(18.dp),
            tint = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.size(6.dp))
        Text(speaker.name, style = MaterialTheme.typography.labelMedium,
            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun GenerateRow(playing: Boolean, generating: Boolean, enabled: Boolean, onGenerate: () -> Unit, onStop: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Button(
            onClick = if (playing || generating) onStop else onGenerate,
            enabled = enabled || playing || generating,
            modifier = Modifier.weight(1f),
        ) {
            if (generating) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary)
                Spacer(Modifier.size(8.dp))
                Text(stringResource(R.string.playground_generating))
            } else {
                Icon(if (playing) Icons.Outlined.Stop else Icons.Outlined.PlayArrow, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text(stringResource(if (playing) R.string.action_stop else R.string.playground_play))
            }
        }
    }
}

@Composable
private fun WaveformBars(amplitudes: FloatArray, playing: Boolean, accent: Color) {
    val bars = 24
    Row(modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically) {
        repeat(bars) { i ->
            val sourceIdx = (i.toFloat() / bars * amplitudes.size).toInt()
                .coerceIn(0, amplitudes.lastIndex.coerceAtLeast(0))
            val target = if (amplitudes.isEmpty()) 0.1f else amplitudes[sourceIdx]
            val baseline = if (playing) 0.18f else 0.08f
            val h by animateFloatAsState(
                targetValue = (target.coerceAtLeast(baseline)).coerceIn(0.05f, 1f),
                animationSpec = spring(stiffness = 600f), label = "playground-wave-$i",
            )
            Box(modifier = Modifier.weight(1f).fillMaxHeight(h).clip(RoundedCornerShape(50)).background(accent))
        }
    }
}

@Composable
private fun HistorySection(
    entries: List<PlaygroundSampleEntity>,
    onReplay: (PlaygroundSampleEntity) -> Unit,
    onDelete: (PlaygroundSampleEntity) -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.playground_history_header), style = MaterialTheme.typography.titleMedium)
        if (entries.isEmpty()) {
            Text(stringResource(R.string.playground_history_empty),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 280.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(items = entries, key = { it.id }) { entry ->
                    HistoryRow(entry, { onReplay(entry) }, { onDelete(entry) })
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(entry: PlaygroundSampleEntity, onReplay: () -> Unit, onDelete: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth()
        .clip(RoundedCornerShape(16.dp))
        .background(MaterialTheme.colorScheme.surfaceContainerLow)
        .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(entry.text.takeIf { it.isNotBlank() } ?: "—",
                style = MaterialTheme.typography.bodyMedium, maxLines = 1)
            Row(modifier = Modifier.padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically) {
                AssistChip(
                    onClick = {}, enabled = false,
                    label = {
                        Text(
                            stringResource(R.string.playground_history_chip,
                                formatMultiplier(entry.speed),
                                formatMultiplier(entry.pitch),
                                formatMultiplier(entry.lengthScale)),
                            style = MaterialTheme.typography.labelSmall,
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                )
                Text(formatRelativeTime(entry.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        IconButton(onClick = onReplay) {
            Icon(Icons.Outlined.PlayArrow, contentDescription = stringResource(R.string.playground_history_play))
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.playground_history_delete))
        }
    }
}

private fun formatMultiplier(value: Float): String {
    val rounded = (value * 10f).roundToInt() / 10f
    return "${rounded}x"
}

@Composable
private fun formatRelativeTime(timestampMs: Long): String {
    val deltaSeconds = ((System.currentTimeMillis() - timestampMs) / 1000L).coerceAtLeast(0L)
    val deltaMinutes = deltaSeconds / 60L
    val deltaHours = deltaMinutes / 60L
    val deltaDays = deltaHours / 24L
    return when {
        deltaSeconds < 45L -> stringResource(R.string.playground_time_just_now)
        deltaMinutes < 60L -> stringResource(R.string.playground_time_minutes, deltaMinutes.toInt())
        deltaHours < 24L -> stringResource(R.string.playground_time_hours, deltaHours.toInt())
        else -> stringResource(R.string.playground_time_days, deltaDays.toInt())
    }
}
