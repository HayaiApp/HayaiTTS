@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package dev.ahmedmohamed.hayaitts.ui.speaker

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import dev.ahmedmohamed.hayaitts.R
import dev.ahmedmohamed.hayaitts.domain.model.InstalledVoice
import dev.ahmedmohamed.hayaitts.domain.model.Speaker
import dev.ahmedmohamed.hayaitts.domain.repo.SettingsRepository
import dev.ahmedmohamed.hayaitts.domain.repo.VoiceRepository
import dev.ahmedmohamed.hayaitts.ui.theme.HayaiTtsTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

/**
 * In-app default-speaker picker for multi-speaker voices. Selecting a speaker
 * persists the choice to [SettingsRepository] so [HayaiTtsService] and the
 * Playground both honor it. Picking "Reset" removes the override and falls
 * back to the voice's first speaker.
 *
 * Caller passes [EXTRA_VOICE_ID]. On success, returns RESULT_OK with
 * [EXTRA_SPEAKER_ID] set to the picked id (absent if reset).
 */
class SpeakerPickerActivity : ComponentActivity() {
    private val voices: VoiceRepository by inject()
    private val settings: SettingsRepository by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val voiceId = intent.getStringExtra(EXTRA_VOICE_ID).orEmpty()
        if (voiceId.isEmpty()) {
            setResult(Activity.RESULT_CANCELED)
            finish()
            return
        }
        setContent {
            HayaiTtsTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SpeakerPickerScreen(
                        voiceId = voiceId,
                        voices = voices,
                        settings = settings,
                        onPicked = { speakerId ->
                            setResult(
                                Activity.RESULT_OK,
                                Intent().putExtra(EXTRA_SPEAKER_ID, speakerId),
                            )
                            finish()
                        },
                        onReset = {
                            setResult(Activity.RESULT_OK)
                            finish()
                        },
                        onBack = { finish() },
                    )
                }
            }
        }
    }

    companion object {
        const val EXTRA_VOICE_ID = "voice_id"
        const val EXTRA_SPEAKER_ID = "speaker_id"

        fun intent(context: Context, voiceId: String): Intent =
            Intent(context, SpeakerPickerActivity::class.java)
                .putExtra(EXTRA_VOICE_ID, voiceId)
    }
}

@Composable
private fun SpeakerPickerScreen(
    voiceId: String,
    voices: VoiceRepository,
    settings: SettingsRepository,
    onPicked: (Int) -> Unit,
    onReset: () -> Unit,
    onBack: () -> Unit,
) {
    val voice by produceState<InstalledVoice?>(initialValue = null, voiceId) {
        value = voices.installed.first().firstOrNull { it.voiceId == voiceId }
    }
    val defaults by settings.defaultSpeakerByVoice.collectAsState(initial = emptyMap())
    val currentSpeakerId = defaults[voiceId]

    val scope = rememberCoroutineScope()
    val topBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(topBarState)

    Scaffold(
        modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(stringResource(R.string.speaker_picker_title)) },
                subtitle = { Text(voice?.title.orEmpty()) },
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
        SpeakerList(
            speakers = voice?.speakers.orEmpty(),
            currentSpeakerId = currentSpeakerId,
            contentPadding = padding,
            onPick = { speakerId ->
                scope.launch {
                    settings.setDefaultSpeaker(voiceId, speakerId)
                    onPicked(speakerId)
                }
            },
            onReset = {
                scope.launch {
                    settings.clearDefaultSpeaker(voiceId)
                    onReset()
                }
            },
        )
    }
}

@Composable
private fun SpeakerList(
    speakers: List<Speaker>,
    currentSpeakerId: Int?,
    contentPadding: PaddingValues,
    onPick: (Int) -> Unit,
    onReset: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize().padding(contentPadding)) {
        LazyColumn(verticalArrangement = Arrangement.Top) {
            item("reset") {
                ListItem(
                    leadingContent = {
                        Icon(Icons.Outlined.RestartAlt, contentDescription = null)
                    },
                    headlineContent = { Text(stringResource(R.string.speaker_picker_reset)) },
                    supportingContent = {
                        Text(stringResource(R.string.speaker_picker_reset_subtitle))
                    },
                    trailingContent = {
                        RadioButton(
                            selected = currentSpeakerId == null,
                            onClick = onReset,
                        )
                    },
                    modifier = Modifier.clickable { onReset() },
                )
                HorizontalDivider()
            }
            items(speakers, key = { it.id }) { speaker ->
                ListItem(
                    headlineContent = { Text(speaker.name) },
                    supportingContent = {
                        Text(
                            stringResource(
                                R.string.speaker_picker_subtitle,
                                speaker.id,
                                speaker.gender.ifEmpty { "—" },
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    trailingContent = {
                        RadioButton(
                            selected = speaker.id == currentSpeakerId,
                            onClick = { onPick(speaker.id) },
                        )
                    },
                    modifier = Modifier.clickable { onPick(speaker.id) },
                )
            }
        }
    }
}
