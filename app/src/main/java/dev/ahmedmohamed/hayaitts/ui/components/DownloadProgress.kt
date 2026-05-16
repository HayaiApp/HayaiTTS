@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package dev.ahmedmohamed.hayaitts.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.ahmedmohamed.hayaitts.R
import dev.ahmedmohamed.hayaitts.domain.model.DownloadState
import kotlin.math.roundToInt

/**
 * Shared progress widget for the catalog/install lifecycle. Renders different
 * indicators per [DownloadState]:
 *
 *  * [DownloadState.Queued]      -> indeterminate linear wavy + "Queued"
 *  * [DownloadState.Running]     -> determinate linear wavy + "12 MB / 78 MB · 15%"
 *  * [DownloadState.Extracting]  -> circular wavy (size unknown until done)
 *
 * Calling for any other state is a no-op; callers should branch on the state
 * upstream and render the install/install-again button instead.
 */
@Composable
fun DownloadProgress(
    state: DownloadState,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        when (state) {
            is DownloadState.Running -> {
                LinearWavyProgressIndicator(
                    progress = { state.pct.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = stringResource(
                        R.string.download_progress_running,
                        bytesToMb(state.downloadedBytes),
                        bytesToMb(state.totalBytes),
                        (state.pct * 100f).roundToInt().coerceIn(0, 100),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            DownloadState.Queued -> {
                LinearWavyProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text(
                    stringResource(R.string.library_downloading),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            DownloadState.Extracting -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    ContainedLoadingIndicator(
                        modifier = Modifier.padding(end = 4.dp),
                    )
                    Text(
                        stringResource(R.string.download_extracting),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            else -> {
                // No-op: callers should render the install button for these states.
            }
        }
    }
}

private fun bytesToMb(bytes: Long): Int =
    (bytes.toDouble() / 1024.0 / 1024.0).roundToInt()
