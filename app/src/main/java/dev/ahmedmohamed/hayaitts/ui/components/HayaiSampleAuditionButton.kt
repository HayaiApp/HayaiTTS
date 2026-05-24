@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package dev.ahmedmohamed.hayaitts.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.ahmedmohamed.hayaitts.R
import dev.ahmedmohamed.hayaitts.data.preview.SampleAudioPlayer
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

enum class AuditionStyle { Filled, Text, IconOnly }

/**
 * Single composable that handles the entire audition lifecycle: pick up the
 * Koin-injected [SampleAudioPlayer], observe its state, render a play/stop
 * affordance matching [style], and stop on disposal.
 *
 * `url == null` renders the button disabled. The play/stop state is keyed by
 * the URL — switching the URL (e.g. via a speaker picker) flips this button
 * back to "Play" even if a different URL is currently playing.
 */
@Composable
fun HayaiSampleAuditionButton(
    url: String?,
    modifier: Modifier = Modifier,
    style: AuditionStyle = AuditionStyle.Filled,
    playLabel: String = stringResource(R.string.voice_detail_sample_play),
    stopLabel: String = stringResource(R.string.voice_detail_sample_stop),
    loadingLabel: String = stringResource(R.string.voice_detail_sample_loading),
) {
    val player: SampleAudioPlayer = koinInject()
    val playerState by player.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    DisposableEffect(Unit) {
        onDispose { player.stop() }
    }

    val isThisPlaying = url != null &&
        playerState is SampleAudioPlayer.State.Playing &&
        (playerState as SampleAudioPlayer.State.Playing).url == url
    val isThisLoading = url != null &&
        playerState is SampleAudioPlayer.State.Loading &&
        (playerState as SampleAudioPlayer.State.Loading).url == url

    val active = isThisPlaying || isThisLoading
    val onClick: () -> Unit = {
        when {
            url == null -> Unit
            active -> player.stop()
            else -> scope.launch { player.play(url) }
        }
    }

    val label = when {
        isThisLoading -> loadingLabel
        active -> stopLabel
        else -> playLabel
    }

    when (style) {
        AuditionStyle.Filled -> FilledTonalButton(
            onClick = onClick,
            enabled = url != null,
            modifier = modifier,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (active) Icons.Outlined.Stop else Icons.Outlined.PlayArrow,
                    contentDescription = null,
                )
                Spacer(Modifier.size(8.dp))
                Text(label)
            }
        }
        AuditionStyle.Text -> TextButton(
            onClick = onClick,
            enabled = url != null,
            modifier = modifier,
        ) {
            Icon(
                imageVector = if (active) Icons.Outlined.Stop else Icons.Outlined.PlayArrow,
                contentDescription = null,
            )
            Spacer(Modifier.size(8.dp))
            Text(label)
        }
        AuditionStyle.IconOnly -> FilledTonalIconButton(
            onClick = onClick,
            enabled = url != null,
            modifier = modifier,
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ),
        ) {
            Icon(
                imageVector = if (active) Icons.Outlined.Stop else Icons.Outlined.PlayArrow,
                contentDescription = if (active) stopLabel else playLabel,
            )
        }
    }
}
