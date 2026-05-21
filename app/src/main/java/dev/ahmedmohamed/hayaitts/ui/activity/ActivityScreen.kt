@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package dev.ahmedmohamed.hayaitts.ui.activity

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.ahmedmohamed.hayaitts.R
import dev.ahmedmohamed.hayaitts.data.telemetry.SynthesisTelemetryRepository
import dev.ahmedmohamed.hayaitts.domain.model.DownloadState
import org.koin.androidx.compose.koinViewModel

private enum class Pane(val labelRes: Int) {
    Downloads(R.string.activity_pane_downloads),
    Extractions(R.string.activity_pane_extractions),
    Generations(R.string.activity_pane_generations),
    RequestLog(R.string.activity_pane_log),
    Cache(R.string.activity_pane_cache),
}

@Composable
fun ActivityScreen(onBack: () -> Unit) {
    val vm: ActivityViewModel = koinViewModel()
    val state by vm.uiState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var selected by remember { mutableStateOf(Pane.Downloads) }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(stringResource(R.string.activity_title)) },
                    scrollBehavior = scrollBehavior,
                )
                PrimaryScrollableTabRow(
                    selectedTabIndex = Pane.entries.indexOf(selected),
                    edgePadding = 16.dp,
                ) {
                    Pane.entries.forEachIndexed { i, pane ->
                        Tab(
                            selected = selected == pane,
                            onClick = { selected = pane },
                            text = { Text(stringResource(pane.labelRes), maxLines = 1) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when (selected) {
                Pane.Downloads -> DownloadsPane(state.downloads.filter {
                    it.state is DownloadState.Queued || it.state is DownloadState.Running
                })
                Pane.Extractions -> DownloadsPane(state.downloads.filter {
                    it.state is DownloadState.Extracting
                })
                Pane.Generations -> GenerationsPane(state.telemetry)
                Pane.RequestLog -> RequestLogPane(state.telemetry)
                Pane.Cache -> CachePane(installedCount = state.installedCount, bytes = state.cacheBytes)
            }
        }
    }
}

@Composable
private fun DownloadsPane(rows: List<ActivityViewModel.DownloadRow>) {
    if (rows.isEmpty()) {
        EmptyPane(text = stringResource(R.string.empty_section))
        return
    }
    LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
        items(rows, key = { it.voiceId }) { row -> DownloadRowItem(row) }
    }
}

@Composable
private fun DownloadRowItem(row: ActivityViewModel.DownloadRow) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(row.title, style = MaterialTheme.typography.titleMedium)
        val (label, fraction) = when (val s = row.state) {
            is DownloadState.Idle -> "Idle" to null
            is DownloadState.Queued -> "Queued" to null
            is DownloadState.Running -> "${(s.pct * 100).toInt()}%" to s.pct
            is DownloadState.Extracting -> "Extracting ${(s.pct * 100).toInt()}%" to s.pct
            is DownloadState.Done -> "Installed" to 1f
            is DownloadState.Failed -> "Failed: ${s.reason}" to null
            is DownloadState.Cancelled -> "Cancelled" to null
        }
        if (fraction != null) {
            LinearWavyProgressIndicator(
                progress = { fraction },
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            LinearWavyProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    androidx.compose.material3.HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
    )
}

@Composable
private fun GenerationsPane(events: List<SynthesisTelemetryRepository.Event>) {
    if (events.isEmpty()) {
        EmptyPane(text = stringResource(R.string.activity_no_live_generations))
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(events, key = { it.timestamp }) { event ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = "${event.voiceId}  ·  ${stringResource(R.string.activity_rtf, event.rtf)}",
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = "synth ${event.synthMs} ms · audio ${event.audioMs} ms · ${event.textLength} chars",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = FontFamily.Monospace,
                )
            }
            androidx.compose.material3.HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            )
        }
    }
}

@Composable
private fun RequestLogPane(events: List<SynthesisTelemetryRepository.Event>) {
    if (events.isEmpty()) {
        EmptyPane(text = stringResource(R.string.activity_no_request_log))
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items(events, key = { it.timestamp }) { event ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = event.callerPackage ?: "—",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.weight(1f),
                    fontFamily = FontFamily.Monospace,
                )
                Text(
                    text = event.locale ?: "—",
                    style = MaterialTheme.typography.labelMedium,
                )
                Text(
                    text = "${event.textLength} ch",
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}

@Composable
private fun CachePane(installedCount: Int, bytes: Long) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("$installedCount voices installed", style = MaterialTheme.typography.titleMedium)
        val mb = bytes / 1_000_000L
        Text(
            "$mb MB on disk",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun EmptyPane(text: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(8.dp))
    }
}
