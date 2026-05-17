@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package dev.ahmedmohamed.hayaitts.ui.update

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import dev.ahmedmohamed.hayaitts.R
import dev.ahmedmohamed.hayaitts.data.update.DownloadProgress
import dev.ahmedmohamed.hayaitts.data.update.UpdateChannel
import dev.ahmedmohamed.hayaitts.data.update.UpdateStatus

/**
 * Shown when [UpdateStatus.Available] flips on — either from the launch-time
 * auto-check or the Settings "Check now" button.
 *
 * Visual rules (per the Hayai design memo): flat M3 surfaces only, no
 * gradients, no per-channel seed colors. The channel chip is the only colored
 * element and picks deterministically from `primaryContainer` /
 * `secondaryContainer` / `tertiaryContainer`.
 *
 * Body rendering: GitHub Release `body` is markdown but we deliberately do NOT
 * add a markdown renderer. Plain text in a monospace [Text] preserves all the
 * relevant structure (lists, code blocks, headings) well enough for a
 * changelog while keeping the dependency surface flat.
 */
@Composable
fun UpdateDialog(
    available: UpdateStatus.Available,
    download: DownloadProgress?,
    onInstall: () -> Unit,
    onCancelInstall: () -> Unit,
    onDismiss: () -> Unit,
) {
    val isDownloading = download is DownloadProgress.Running
    val isDone = download is DownloadProgress.Done

    AlertDialog(
        onDismissRequest = { if (!isDownloading) onDismiss() },
        title = { Text(stringResource(R.string.update_dialog_title)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = available.tag,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    ChannelChip(channel = available.channel)
                }

                if (isDownloading || isDone) {
                    val fraction = (download as? DownloadProgress.Running)?.fraction ?: 1f
                    val bytesRead = (download as? DownloadProgress.Running)?.bytesRead ?: 0L
                    val totalBytes = (download as? DownloadProgress.Running)?.totalBytes ?: 0L
                    LinearWavyProgressIndicator(
                        progress = { fraction.coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text = stringResource(
                            R.string.update_progress_caption,
                            formatMb(bytesRead),
                            formatMb(totalBytes.coerceAtLeast(0L)),
                            (fraction * 100).toInt(),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    val failed = download as? DownloadProgress.Failed
                    if (failed != null) {
                        Text(
                            text = stringResource(R.string.update_download_failed, failed.reason),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    val cleanedBody = available.body
                        .replace("\r\n", "\n")
                        .trim()
                        .ifEmpty { stringResource(R.string.update_no_changelog) }
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        Text(
                            text = cleanedBody,
                            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        },
        confirmButton = {
            when {
                isDownloading -> TextButton(onClick = onCancelInstall) {
                    Text(stringResource(R.string.action_cancel))
                }
                else -> TextButton(onClick = onInstall) {
                    Text(stringResource(R.string.update_action_install))
                }
            }
        },
        dismissButton = {
            if (!isDownloading) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.update_action_later))
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    )
}

@Composable
private fun ChannelChip(channel: UpdateChannel) {
    val (label, bg, fg) = when (channel) {
        UpdateChannel.STABLE -> Triple(
            stringResource(R.string.update_channel_stable),
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
        )
        UpdateChannel.BETA -> Triple(
            stringResource(R.string.update_channel_beta),
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer,
        )
        UpdateChannel.NIGHTLY -> Triple(
            stringResource(R.string.update_channel_nightly),
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer,
        )
    }
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = fg,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .padding(PaddingValues(horizontal = 10.dp, vertical = 4.dp)),
    )
}

private fun formatMb(bytes: Long): String {
    if (bytes <= 0L) return "0 MB"
    val mb = bytes.toDouble() / (1024.0 * 1024.0)
    return "%.1f MB".format(mb)
}
