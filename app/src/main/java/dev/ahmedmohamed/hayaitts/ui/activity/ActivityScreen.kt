@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package dev.ahmedmohamed.hayaitts.ui.activity

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import dev.ahmedmohamed.hayaitts.ui.components.HayaiEmpty
import dev.ahmedmohamed.hayaitts.ui.components.HayaiEmptyMode
import dev.ahmedmohamed.hayaitts.ui.components.HayaiTopBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.ahmedmohamed.hayaitts.R
import dev.ahmedmohamed.hayaitts.data.telemetry.SynthesisTelemetryRepository
import dev.ahmedmohamed.hayaitts.domain.model.DownloadState
import org.koin.androidx.compose.koinViewModel

private enum class Pane(val labelRes: Int, val icon: ImageVector) {
    Downloads(R.string.activity_pane_downloads, Icons.Outlined.CloudDownload),
    Extractions(R.string.activity_pane_extractions, Icons.Outlined.Inventory2),
    Generations(R.string.activity_pane_generations, Icons.Outlined.GraphicEq),
    RequestLog(R.string.activity_pane_log, Icons.Outlined.History),
    Cache(R.string.activity_pane_cache, Icons.Outlined.Folder),
}

@Composable
fun ActivityScreen(onBack: () -> Unit) {
    val vm: ActivityViewModel = koinViewModel()
    val state by vm.uiState.collectAsStateWithLifecycle()
    var selected by remember { mutableStateOf(Pane.Downloads) }
    var searchQuery by remember { mutableStateOf("") }

    dev.ahmedmohamed.hayaitts.ui.components.HayaiScreenChrome(
        title = stringResource(R.string.activity_title),
        searchable = dev.ahmedmohamed.hayaitts.ui.components.HayaiSearchable(
            query = searchQuery,
            onQueryChange = { searchQuery = it },
            placeholder = stringResource(R.string.topbar_search_activity),
        ),
        barOverlay = {
            // Primary scrollable tabs sit just below the floating top bar.
            // Tabs use the content slot so the icon + label render side by
            // side rather than stacked vertically.
            androidx.compose.material3.Surface(
                color = MaterialTheme.colorScheme.background,
            ) {
                PrimaryScrollableTabRow(
                    selectedTabIndex = Pane.entries.indexOf(selected),
                    edgePadding = 16.dp,
                ) {
                    Pane.entries.forEach { pane ->
                        Tab(
                            selected = selected == pane,
                            onClick = { selected = pane },
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                            ) {
                                Icon(pane.icon, contentDescription = null)
                                Text(stringResource(pane.labelRes), maxLines = 1)
                            }
                        }
                    }
                }
            }
        },
    ) { topInset ->
        // The tab strip is part of the floating chrome, so add its measured
        // height (~48dp tab + 1dp divider) to the inset so content scrolls
        // behind both bar and tabs.
        val chromeInset = topInset + 49.dp
        val q = searchQuery.trim()
        val downloadsFiltered = remember(state.downloads, q) {
            state.downloads.filter { row ->
                q.isEmpty() || row.title.contains(q, ignoreCase = true) ||
                    row.voiceId.contains(q, ignoreCase = true)
            }
        }
        val telemetryFiltered = remember(state.telemetry, q) {
            state.telemetry.filter { ev ->
                q.isEmpty() ||
                    ev.voiceId.contains(q, ignoreCase = true) ||
                    ev.locale?.contains(q, ignoreCase = true) == true ||
                    ev.callerPackage?.contains(q, ignoreCase = true) == true
            }
        }
        when (selected) {
            Pane.Downloads -> DownloadsPane(downloadsFiltered.filter {
                it.state is DownloadState.Queued || it.state is DownloadState.Running
            }, topInset = chromeInset)
            Pane.Extractions -> ExtractionsPane(downloadsFiltered.filter {
                it.state is DownloadState.Extracting
            }, topInset = chromeInset)
            Pane.Generations -> GenerationsPane(telemetryFiltered, topInset = chromeInset)
            Pane.RequestLog -> RequestLogPane(telemetryFiltered, topInset = chromeInset)
            Pane.Cache -> CachePane(
                installedCount = state.installedCount,
                bytes = state.cacheBytes,
                topInset = chromeInset,
            )
        }
    }
}

@Composable
private fun DownloadsPane(rows: List<ActivityViewModel.DownloadRow>, topInset: androidx.compose.ui.unit.Dp) {
    if (rows.isEmpty()) {
        EmptyHero(
            icon = Icons.Outlined.CloudDownload,
            title = stringResource(R.string.activity_pane_downloads),
            subtitle = stringResource(R.string.empty_section),
            topInset = topInset,
        )
        return
    }
    LazyColumn(contentPadding = PaddingValues(top = topInset, bottom = 8.dp)) {
        items(rows, key = { it.voiceId }) { row -> DownloadRowItem(row) }
    }
}

@Composable
private fun ExtractionsPane(rows: List<ActivityViewModel.DownloadRow>, topInset: androidx.compose.ui.unit.Dp) {
    if (rows.isEmpty()) {
        EmptyHero(
            icon = Icons.Outlined.Inventory2,
            title = stringResource(R.string.activity_pane_extractions),
            subtitle = stringResource(R.string.empty_section),
            topInset = topInset,
        )
        return
    }
    LazyColumn(contentPadding = PaddingValues(top = topInset, bottom = 8.dp)) {
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
            is DownloadState.Idle -> stringResource(R.string.activity_status_idle) to null
            is DownloadState.Queued -> stringResource(R.string.downloads_status_queued) to null
            is DownloadState.Running ->
                stringResource(R.string.activity_status_running_pct, (s.pct * 100).toInt()) to s.pct
            is DownloadState.Extracting ->
                stringResource(R.string.downloads_status_extracting_pct, (s.pct * 100).toInt()) to s.pct
            is DownloadState.Done -> stringResource(R.string.action_installed) to 1f
            is DownloadState.Failed -> stringResource(R.string.downloads_status_failed, s.reason) to null
            is DownloadState.Cancelled -> stringResource(R.string.activity_status_cancelled) to null
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
private fun GenerationsPane(events: List<SynthesisTelemetryRepository.Event>, topInset: androidx.compose.ui.unit.Dp) {
    if (events.isEmpty()) {
        EmptyHero(
            icon = Icons.Outlined.GraphicEq,
            title = stringResource(R.string.activity_pane_generations),
            subtitle = stringResource(R.string.activity_no_live_generations),
            topInset = topInset,
        )
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(top = topInset, start = 12.dp, end = 12.dp, bottom = 12.dp),
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
                    text = stringResource(
                        R.string.activity_generation_detail,
                        event.synthMs,
                        event.audioMs,
                        event.textLength,
                    ),
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
private fun RequestLogPane(events: List<SynthesisTelemetryRepository.Event>, topInset: androidx.compose.ui.unit.Dp) {
    if (events.isEmpty()) {
        EmptyHero(
            icon = Icons.Outlined.History,
            title = stringResource(R.string.activity_pane_log),
            subtitle = stringResource(R.string.activity_no_request_log),
            topInset = topInset,
        )
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(top = topInset, start = 12.dp, end = 12.dp, bottom = 12.dp),
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
                    text = stringResource(R.string.activity_request_chars, event.textLength),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}

@Composable
private fun CachePane(installedCount: Int, bytes: Long, topInset: androidx.compose.ui.unit.Dp) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = topInset)
            .padding(horizontal = 24.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            stringResource(R.string.activity_cache_voices_installed, installedCount),
            style = MaterialTheme.typography.titleMedium,
        )
        val mb = bytes / 1_000_000L
        Text(
            stringResource(R.string.activity_cache_mb_on_disk, mb),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun EmptyHero(icon: ImageVector, title: String, subtitle: String, topInset: androidx.compose.ui.unit.Dp) {
    HayaiEmpty(
        mode = HayaiEmptyMode.Empty(icon = icon, title = title, subtitle = subtitle),
        modifier = Modifier
            .fillMaxSize()
            .padding(top = topInset),
    )
}
