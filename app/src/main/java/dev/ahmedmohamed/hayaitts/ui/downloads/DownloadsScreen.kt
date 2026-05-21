@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package dev.ahmedmohamed.hayaitts.ui.downloads

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.ahmedmohamed.hayaitts.R
import dev.ahmedmohamed.hayaitts.domain.model.DownloadState
import dev.ahmedmohamed.hayaitts.domain.model.ModelFamily
import dev.ahmedmohamed.hayaitts.ui.components.EmptyState
import dev.ahmedmohamed.hayaitts.ui.components.FamilyBadge
import org.koin.androidx.compose.koinViewModel
import kotlin.math.roundToInt

/**
 * Dedicated screen listing every download the user has touched, grouped into
 * Active / Failed / Recently completed. Each card's container color animates
 * to reflect state: tertiaryContainer while running/extracting,
 * errorContainer on failure, surfaceContainer when done. Section headers
 * carry a tonal leading dot for Expressive flair.
 */
@Composable
fun DownloadsScreen(
    onBack: () -> Unit,
    viewModel: DownloadsViewModel = koinViewModel(),
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(stringResource(R.string.downloads_title)) },
                subtitle = {
                    val activeCount = state.activeCount
                    Text(
                        text = if (activeCount > 0) stringResource(R.string.downloads_subtitle_active, activeCount)
                        else stringResource(R.string.downloads_subtitle_idle),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    IconButton(
                        onClick = viewModel::clearCompleted,
                        enabled = state.completed.isNotEmpty() || state.failed.isNotEmpty(),
                    ) {
                        Icon(Icons.Outlined.DeleteSweep, contentDescription = stringResource(R.string.downloads_clear_completed))
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        if (state.isEmpty) {
            EmptyState(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                icon = Icons.Outlined.Inbox,
                title = stringResource(R.string.downloads_empty_title),
                subtitle = stringResource(R.string.downloads_empty_subtitle),
            )
        } else {
            DownloadsBody(
                state = state,
                contentPadding = innerPadding,
                onCancel = viewModel::cancel,
                onRetry = { row ->
                    val card = row.voiceCard
                    if (card != null) viewModel.retry(card) else viewModel.retry(row.voiceId)
                },
                onRemoveHistory = viewModel::removeFromHistory,
            )
        }
    }
}

@Composable
private fun DownloadsBody(
    state: DownloadsViewModel.UiState,
    contentPadding: PaddingValues,
    onCancel: (String) -> Unit,
    onRetry: (DownloadsViewModel.Row) -> Unit,
    onRemoveHistory: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding(),
            bottom = contentPadding.calculateBottomPadding() + 24.dp,
            start = 0.dp,
            end = 0.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (state.active.isNotEmpty()) {
            item("active_header") {
                SectionHeader(
                    text = stringResource(R.string.downloads_section_active),
                    dotColor = MaterialTheme.colorScheme.tertiary,
                )
            }
            downloadRows(state.active) { row ->
                DownloadRow(row = row, trailing = {
                    OutlinedButton(onClick = { onCancel(row.voiceId) }) {
                        Icon(Icons.Outlined.Cancel, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text(stringResource(R.string.downloads_action_cancel))
                    }
                }, onRemoveHistory = null)
            }
        }
        if (state.failed.isNotEmpty()) {
            item("failed_header") {
                SectionHeader(
                    text = stringResource(R.string.downloads_section_failed),
                    dotColor = MaterialTheme.colorScheme.error,
                )
            }
            downloadRows(state.failed) { row ->
                DownloadRow(row = row, trailing = {
                    FilledTonalButton(onClick = { onRetry(row) }) {
                        Icon(Icons.Outlined.Refresh, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text(stringResource(R.string.downloads_action_retry))
                    }
                }, onRemoveHistory = { onRemoveHistory(row.voiceId) })
            }
        }
        if (state.completed.isNotEmpty()) {
            item("completed_header") {
                SectionHeader(
                    text = stringResource(R.string.downloads_section_completed),
                    dotColor = MaterialTheme.colorScheme.primary,
                )
            }
            downloadRows(state.completed) { row ->
                DownloadRow(row = row, trailing = {
                    OutlinedButton(onClick = { onRetry(row) }) {
                        Icon(Icons.Outlined.CloudDownload, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text(stringResource(R.string.downloads_action_redownload))
                    }
                }, onRemoveHistory = { onRemoveHistory(row.voiceId) })
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String, dotColor: Color) {
    Row(
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier.size(10.dp).clip(CircleShape).background(dotColor),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun DownloadRow(
    row: DownloadsViewModel.Row,
    trailing: @Composable () -> Unit,
    onRemoveHistory: (() -> Unit)?,
) {
    val family = row.voiceCard?.modelFamily ?: ModelFamily.PIPER
    val targetContainer = when (row.state) {
        is DownloadState.Running, is DownloadState.Extracting -> MaterialTheme.colorScheme.tertiaryContainer
        is DownloadState.Failed, DownloadState.Cancelled -> MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.surfaceContainer
    }
    val targetContent = when (row.state) {
        is DownloadState.Running, is DownloadState.Extracting -> MaterialTheme.colorScheme.onTertiaryContainer
        is DownloadState.Failed, DownloadState.Cancelled -> MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.onSurface
    }
    val containerColor by animateColorAsState(
        targetValue = targetContainer,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "download-bg",
    )
    val contentColor by animateColorAsState(
        targetValue = targetContent,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "download-fg",
    )
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor,
        ),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                FamilyBadge(family = family, downloadState = row.state)
                Spacer(Modifier.size(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = row.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = contentColor,
                        maxLines = 1,
                    )
                    Spacer(Modifier.height(2.dp))
                    StatusLine(row, contentColor)
                }
                if (onRemoveHistory != null) OverflowMenu(onRemoveHistory = onRemoveHistory)
            }
            val pct = when (val s = row.state) {
                is DownloadState.Running -> s.pct
                is DownloadState.Extracting -> s.pct
                else -> null
            }
            if (pct != null) {
                Spacer(Modifier.height(14.dp))
                LinearWavyProgressIndicator(
                    progress = { pct.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = contentColor.copy(alpha = 0.18f))
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                trailing()
            }
        }
    }
}

@Composable
private fun StatusLine(row: DownloadsViewModel.Row, contentColor: Color) {
    val color = contentColor.copy(alpha = 0.78f)
    val icon = when (row.state) {
        is DownloadState.Failed -> Icons.Outlined.ErrorOutline
        DownloadState.Cancelled -> Icons.Outlined.ErrorOutline
        DownloadState.Done -> Icons.Outlined.CheckCircle
        else -> null
    }
    val text = when (val s = row.state) {
        is DownloadState.Queued -> stringResource(R.string.downloads_status_queued)
        is DownloadState.Running -> stringResource(
            R.string.downloads_status_running,
            formatSize(s.downloadedBytes),
            formatSize(s.totalBytes),
            (s.pct * 100).roundToInt().coerceIn(0, 100),
        )
        is DownloadState.Extracting -> {
            val pct = (s.pct * 100).roundToInt().coerceIn(0, 100)
            stringResource(R.string.downloads_status_extracting_pct, pct)
        }
        is DownloadState.Failed -> stringResource(R.string.downloads_status_failed, s.reason)
        DownloadState.Cancelled -> stringResource(R.string.library_download_cancelled)
        DownloadState.Done -> {
            val ts = row.completedAtMillis
            if (ts != null) stringResource(R.string.downloads_status_completed_ago, relativeAgo(ts))
            else stringResource(R.string.downloads_status_completed)
        }
        DownloadState.Idle -> ""
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
            Spacer(Modifier.size(4.dp))
        }
        Text(text = text, style = MaterialTheme.typography.bodySmall, color = color)
    }
}

@Composable
private fun OverflowMenu(onRemoveHistory: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Outlined.MoreVert, contentDescription = stringResource(R.string.action_more))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.downloads_action_remove_history)) },
                leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
                onClick = { expanded = false; onRemoveHistory() },
            )
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.downloadRows(
    list: List<DownloadsViewModel.Row>,
    content: @Composable (DownloadsViewModel.Row) -> Unit,
) {
    items(count = list.size, key = { idx -> "dl_" + list[idx].voiceId + "_" + idx }) { idx ->
        content(list[idx])
    }
}

@Composable
private fun formatSize(bytes: Long): String {
    if (bytes <= 0L) return stringResource(R.string.downloads_size_unknown)
    val mb = bytes.toDouble() / 1024.0 / 1024.0
    return if (mb < 1.0) stringResource(R.string.downloads_size_kb, (bytes / 1024L).toInt())
    else stringResource(R.string.downloads_size_mb, mb.roundToInt())
}

@Composable
private fun relativeAgo(timestampMillis: Long): String {
    val delta = (System.currentTimeMillis() - timestampMillis).coerceAtLeast(0L)
    val minutes = delta / 60_000L
    val hours = minutes / 60L
    val days = hours / 24L
    return when {
        minutes < 1L -> stringResource(R.string.downloads_time_just_now)
        minutes < 60L -> stringResource(R.string.downloads_time_minutes, minutes.toInt())
        hours < 24L -> stringResource(R.string.downloads_time_hours, hours.toInt())
        else -> stringResource(R.string.downloads_time_days, days.toInt())
    }
}
