@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package dev.ahmedmohamed.hayaitts.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.ahmedmohamed.hayaitts.R
import dev.ahmedmohamed.hayaitts.domain.model.DownloadState
import dev.ahmedmohamed.hayaitts.domain.model.Speaker
import dev.ahmedmohamed.hayaitts.ui.components.DownloadProgress
import dev.ahmedmohamed.hayaitts.ui.components.FamilyChip
import dev.ahmedmohamed.hayaitts.ui.components.LanguageChip
import dev.ahmedmohamed.hayaitts.ui.components.TierChip
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Per-voice detail screen reached from Browse or Library. Shows the hero block,
 * the speakers list, a tappable preview text field, and a context-sensitive
 * action row (Install / Cancel / Uninstall / Set default).
 *
 * Preview synthesis is only enabled when the voice is installed — the runtime
 * cannot load an `.onnx` weight file before the downloader has unpacked it.
 */
@Composable
fun VoiceDetailScreen(
    voiceId: String,
    onBack: () -> Unit,
) {
    val viewModel: VoiceDetailViewModel = koinViewModel { parametersOf(voiceId) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    DisposableEffect(Unit) {
        onDispose { viewModel.stop() }
    }

    val title = state.installed?.title ?: state.card?.title ?: voiceId

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            HeroBlock(state = state)
            SpeakersSection(speakers = state.installed?.speakers ?: state.card?.speakers.orEmpty())
            PreviewSection(
                enabled = state.isInstalled,
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

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.RecordVoiceOver,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.size(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = installed?.title ?: card?.title ?: "—",
                        style = MaterialTheme.typography.headlineSmall,
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
            Spacer(Modifier.height(12.dp))
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
private fun SpeakersSection(speakers: List<Speaker>) {
    if (speakers.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.voice_detail_speakers),
            style = MaterialTheme.typography.titleMedium,
        )
        speakers.forEach { spk ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = when (spk.gender.lowercase()) {
                                "f" -> Icons.Outlined.Female
                                "m" -> Icons.Outlined.Person
                                else -> Icons.Outlined.RecordVoiceOver
                            },
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(spk.name, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text = spk.gender.uppercase(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PreviewSection(
    enabled: Boolean,
    onPlay: (String) -> Unit,
    onStop: () -> Unit,
) {
    val defaultText = stringResource(R.string.voice_detail_preview_text)
    var text by remember { mutableStateOf(defaultText) }
    var playing by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.voice_detail_preview),
            style = MaterialTheme.typography.titleMedium,
        )
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 3,
        )
        if (!enabled) {
            Text(
                text = stringResource(R.string.voice_detail_preview_not_installed),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        FilledTonalButton(
            onClick = {
                if (playing) {
                    playing = false
                    onStop()
                } else {
                    playing = true
                    onPlay(text)
                }
            },
            enabled = enabled && text.isNotBlank(),
        ) {
            Icon(
                imageVector = if (playing) Icons.Outlined.Stop else Icons.Outlined.PlayArrow,
                contentDescription = null,
            )
            Spacer(Modifier.size(8.dp))
            Text(stringResource(if (playing) R.string.action_stop else R.string.action_play))
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
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                installed.languages.forEach { locale ->
                    val isDefault = locale in defaulted
                    AssistChip(
                        onClick = { onSetDefault(locale) },
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
                            { Icon(Icons.Outlined.CheckCircle, contentDescription = null) }
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
                if (!installed.bundled) {
                    OutlinedButton(onClick = onUninstall) {
                        Icon(Icons.Outlined.Delete, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text(stringResource(R.string.action_uninstall))
                    }
                }
            }
            state.card != null -> {
                FilledTonalButton(onClick = onInstall) {
                    Icon(Icons.Outlined.CloudDownload, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text(stringResource(R.string.action_install))
                }
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
