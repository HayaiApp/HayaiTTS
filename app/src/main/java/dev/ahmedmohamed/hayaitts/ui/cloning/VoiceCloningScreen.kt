@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package dev.ahmedmohamed.hayaitts.ui.cloning

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Audiotrack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.ahmedmohamed.hayaitts.R
import dev.ahmedmohamed.hayaitts.ui.components.HayaiScreenChrome
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Voice-cloning playground. Available only for voices whose family
 * supports reference-audio cloning (ZipVoice / Pocket). Layout (top → down):
 *
 *  1. Reference source row: a `Record` button (mic capture, runtime
 *     RECORD_AUDIO request) and an `Upload` button (SAF audio picker).
 *  2. Captured-clip summary card with duration / sample rate / clear.
 *  3. "Reference transcript" multi-line text field — what the captured
 *     clip says.
 *  4. "Target text" multi-line text field — what the cloned voice
 *     should say.
 *  5. `Generate` primary button + a play/stop control once the clone
 *     finishes.
 */
@Composable
fun VoiceCloningScreen(
    voiceId: String,
    onBack: () -> Unit,
) {
    val viewModel: VoiceCloningViewModel = koinViewModel { parametersOf(voiceId) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Runtime RECORD_AUDIO request — only fires when the user taps the
    // mic button. Permission already granted → starts immediately.
    val recordPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) viewModel.startRecording()
    }

    // SAF audio file picker — accepts any audio/* MIME, MediaCodec
    // does the decode.
    val pickAudioLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) viewModel.loadReferenceFile(uri)
    }

    LaunchedEffect(state.error) {
        val err = state.error ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(err)
        viewModel.dismissError()
    }

    HayaiScreenChrome(
        title = stringResource(R.string.cloning_title),
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = stringResource(R.string.action_back),
                )
            }
        },
        snackbarHostState = snackbarHostState,
    ) { topInset ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(Modifier.height(topInset))

            Text(
                text = stringResource(R.string.cloning_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            ReferenceSourceRow(
                recording = state.recording,
                amplitude = state.recordingAmplitude,
                onRecordTap = {
                    if (state.recording) {
                        viewModel.stopRecording()
                        return@ReferenceSourceRow
                    }
                    val granted = ContextCompat.checkSelfPermission(
                        context, Manifest.permission.RECORD_AUDIO,
                    ) == PackageManager.PERMISSION_GRANTED
                    if (granted) {
                        viewModel.startRecording()
                    } else {
                        recordPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                onUploadTap = {
                    pickAudioLauncher.launch(arrayOf("audio/*"))
                },
            )

            val clip = state.referenceClip
            if (clip != null) {
                ReferenceClipCard(
                    durationMs = clip.durationMs,
                    sampleRate = clip.sampleRate,
                    onClear = viewModel::clearReference,
                )
            }

            OutlinedTextField(
                value = state.referenceText,
                onValueChange = viewModel::setReferenceText,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.cloning_reference_text)) },
                placeholder = {
                    Text(stringResource(R.string.cloning_reference_text_hint))
                },
                minLines = 2,
                maxLines = 4,
                shape = RoundedCornerShape(16.dp),
            )

            OutlinedTextField(
                value = state.targetText,
                onValueChange = viewModel::setTargetText,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.cloning_target_text)) },
                placeholder = {
                    Text(stringResource(R.string.cloning_target_text_hint))
                },
                minLines = 3,
                maxLines = 8,
                shape = RoundedCornerShape(16.dp),
            )

            GenerateRow(
                enabled = state.canGenerate,
                generating = state.generating,
                playing = state.playing,
                onGenerate = viewModel::generate,
            )
        }
    }
}

@Composable
private fun ReferenceSourceRow(
    recording: Boolean,
    amplitude: Float,
    onRecordTap: () -> Unit,
    onUploadTap: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        FilledTonalButton(
            onClick = onRecordTap,
            modifier = Modifier
                .weight(1f)
                .height(64.dp),
            colors = if (recording) {
                ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                )
            } else {
                ButtonDefaults.filledTonalButtonColors()
            },
        ) {
            Icon(
                imageVector = if (recording) Icons.Outlined.Stop else Icons.Outlined.Mic,
                contentDescription = null,
            )
            Spacer(Modifier.size(8.dp))
            Text(
                text = stringResource(
                    if (recording) R.string.cloning_stop_recording
                    else R.string.cloning_record,
                ),
            )
        }
        FilledTonalButton(
            onClick = onUploadTap,
            enabled = !recording,
            modifier = Modifier
                .weight(1f)
                .height(64.dp),
        ) {
            Icon(Icons.Outlined.FileUpload, contentDescription = null)
            Spacer(Modifier.size(8.dp))
            Text(stringResource(R.string.cloning_upload))
        }
    }
    if (recording) {
        // Visualise live mic input as a wavy bar so the user knows the
        // capture is actually picking up sound.
        LinearWavyProgressIndicator(
            progress = { amplitude.coerceIn(0f, 1f) * 4f },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ReferenceClipCard(
    durationMs: Long,
    sampleRate: Int,
    onClear: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.Audiotrack,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
            Spacer(Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.cloning_clip_summary),
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = "${(durationMs / 1000.0).format(1)} s · ${sampleRate / 1000} kHz",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onClear) {
                Icon(
                    Icons.Outlined.Close,
                    contentDescription = stringResource(R.string.action_clear),
                )
            }
        }
    }
}

@Composable
private fun GenerateRow(
    enabled: Boolean,
    generating: Boolean,
    playing: Boolean,
    onGenerate: () -> Unit,
) {
    Button(
        onClick = onGenerate,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
    ) {
        Icon(
            imageVector = when {
                playing -> Icons.Outlined.Stop
                else -> Icons.Outlined.PlayArrow
            },
            contentDescription = null,
        )
        Spacer(Modifier.size(8.dp))
        Text(
            text = stringResource(
                when {
                    generating -> R.string.cloning_generating
                    playing -> R.string.cloning_playing
                    else -> R.string.cloning_generate
                },
            ),
        )
    }
    if (generating) {
        LinearWavyProgressIndicator(modifier = Modifier.fillMaxWidth())
    }
}

private fun Double.format(digits: Int): String = "%.${digits}f".format(this)
