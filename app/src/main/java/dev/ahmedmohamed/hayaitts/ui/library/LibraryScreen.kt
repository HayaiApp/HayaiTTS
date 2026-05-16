@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package dev.ahmedmohamed.hayaitts.ui.library

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.ahmedmohamed.hayaitts.R
import dev.ahmedmohamed.hayaitts.domain.model.DownloadState
import dev.ahmedmohamed.hayaitts.domain.model.InstalledVoice
import org.koin.androidx.compose.koinViewModel

@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel = koinViewModel(),
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.library_title)) },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        // Trip the entrance animation once on first composition so the screen
        // springs in instead of popping.
        var visible by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { visible = true }

        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(
                initialOffsetY = { it / 4 },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow,
                ),
            ) + fadeIn(animationSpec = spring(stiffness = Spring.StiffnessLow)),
            modifier = Modifier.padding(innerPadding),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                state.installed.forEach { voice ->
                    InstalledVoiceCard(voice = voice)
                }

                SmokeTestDownloadSection(
                    smokeState = state.downloadStates[LibraryViewModel.SMOKE_TEST_VOICE_ID]
                        ?: DownloadState.Idle,
                    alreadyInstalled = state.installed.any {
                        it.voiceId == LibraryViewModel.SMOKE_TEST_VOICE_ID
                    },
                    onEnqueue = { viewModel.enqueueSmokeTest() },
                    onCancel = { viewModel.cancelSmokeTest() },
                )
            }
        }
    }
}

@Composable
private fun InstalledVoiceCard(voice: InstalledVoice) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.RecordVoiceOver,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(text = voice.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = buildSubtitle(voice),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (voice.bundled) {
                    AssistChip(
                        onClick = {},
                        enabled = false,
                        label = { Text(stringResource(R.string.voice_chip_bundled)) },
                        colors = AssistChipDefaults.assistChipColors(
                            disabledLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            disabledContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        ),
                    )
                }
            }
            FilledTonalIconButton(onClick = { /* preview wired in 4b */ }) {
                Icon(
                    imageVector = Icons.Outlined.PlayArrow,
                    contentDescription = stringResource(R.string.voice_preview_action),
                )
            }
        }
    }
}

private fun buildSubtitle(voice: InstalledVoice): String {
    val lang = voice.languages.firstOrNull() ?: "—"
    val family = voice.family.name.lowercase().replaceFirstChar { it.uppercase() }
    val tier = voice.tier.name.lowercase().replaceFirstChar { it.uppercase() }
    return "$lang • $family • $tier"
}

@Composable
private fun SmokeTestDownloadSection(
    smokeState: DownloadState,
    alreadyInstalled: Boolean,
    onEnqueue: () -> Unit,
    onCancel: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val labelRes = if (alreadyInstalled) {
                R.string.library_test_download_again
            } else R.string.library_test_download

            when (smokeState) {
                is DownloadState.Running -> {
                    Text(
                        stringResource(R.string.library_downloading),
                        style = MaterialTheme.typography.labelLarge,
                    )
                    LinearWavyProgressIndicator(
                        progress = { smokeState.pct },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(4.dp))
                    OutlinedButton(onClick = onCancel) {
                        Icon(Icons.Outlined.Cancel, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text("Cancel")
                    }
                }

                DownloadState.Queued, DownloadState.Extracting -> {
                    Text(
                        stringResource(
                            if (smokeState is DownloadState.Extracting) R.string.library_extracting
                            else R.string.library_downloading,
                        ),
                        style = MaterialTheme.typography.labelLarge,
                    )
                    LinearWavyProgressIndicator(modifier = Modifier.fillMaxWidth())
                }

                is DownloadState.Failed -> {
                    Text(
                        stringResource(R.string.library_download_failed, smokeState.reason),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                    SmokeTestEnqueueButton(labelRes = labelRes, onClick = onEnqueue)
                }

                DownloadState.Cancelled -> {
                    Text(
                        stringResource(R.string.library_download_cancelled),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    SmokeTestEnqueueButton(labelRes = labelRes, onClick = onEnqueue)
                }

                DownloadState.Done, DownloadState.Idle -> {
                    SmokeTestEnqueueButton(labelRes = labelRes, onClick = onEnqueue)
                }
            }
        }
    }
}

@Composable
private fun SmokeTestEnqueueButton(labelRes: Int, onClick: () -> Unit) {
    FilledTonalButton(onClick = onClick) {
        Icon(Icons.Outlined.CloudDownload, contentDescription = null)
        Spacer(Modifier.size(8.dp))
        Text(stringResource(labelRes))
    }
}
