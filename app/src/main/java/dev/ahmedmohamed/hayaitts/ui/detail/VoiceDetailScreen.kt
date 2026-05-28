@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package dev.ahmedmohamed.hayaitts.ui.detail

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Female
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import dev.ahmedmohamed.hayaitts.ui.components.HayaiTopBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.ahmedmohamed.hayaitts.R
import kotlinx.coroutines.launch
import dev.ahmedmohamed.hayaitts.domain.model.DownloadState
import dev.ahmedmohamed.hayaitts.domain.model.Speaker
import dev.ahmedmohamed.hayaitts.ui.components.DownloadProgress
import dev.ahmedmohamed.hayaitts.ui.components.FamilyChip
import dev.ahmedmohamed.hayaitts.ui.components.HayaiRichTooltipBox
import dev.ahmedmohamed.hayaitts.ui.components.LanguageChip
import dev.ahmedmohamed.hayaitts.ui.components.TierChip
import dev.ahmedmohamed.hayaitts.ui.components.identityOrDefault
import dev.ahmedmohamed.hayaitts.ui.speaker.SpeakerPickerActivity
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Per-voice detail screen reached from Browse or Library.
 *
 * Layout (top-down):
 *   1. [LargeFlexibleTopAppBar] — back + title + subtitle. Actions slot holds
 *      Quick-Switch + a 3-dot overflow with Playground / Uninstall.
 *   2. Hero block — family glyph, title, chip strip, license caption.
 *   3. Speakers row.
 *   4. Preview block — waveform + text + play/stop.
 *   5. Optional "Use as default for X" grouped section (only when installed
 *      and the voice ships multiple locales).
 *   6. Sticky-feeling bottom [PrimaryAction] — one button that switches
 *      between Install / Cancel-download / Set-default / Coming-soon based
 *      on state.
 *
 * Every secondary affordance (Playground, Uninstall, Quick Switch) is in the
 * top-bar's actions slot so the body has one and only one primary CTA.
 */
@Composable
fun VoiceDetailScreen(
    voiceId: String,
    onBack: () -> Unit,
    onOpenQuickSwitch: () -> Unit,
    onOpenPlayground: () -> Unit = {},
    onOpenCloning: () -> Unit = {},
) {
    val viewModel: VoiceDetailViewModel = koinViewModel { parametersOf(voiceId) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val amplitudes by viewModel.previewAmplitudes.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    DisposableEffect(Unit) { onDispose { viewModel.stop() } }

    val accent = MaterialTheme.colorScheme.primary
    val context = LocalContext.current
    val installed = state.installed
    val card = state.card
    var overflowOpen by remember { mutableStateOf(false) }

    val subtitle = buildString {
        val firstLang = (installed?.languages ?: card?.languages.orEmpty()).firstOrNull()
        val fam = card?.modelFamily?.name?.lowercase()?.replaceFirstChar { it.uppercase() }
            ?: installed?.family?.name?.lowercase()?.replaceFirstChar { it.uppercase() }
        if (firstLang != null) append(firstLang)
        if (fam != null) {
            if (isNotEmpty()) append("  ·  ")
            append(fam)
        }
    }.ifBlank { null }
    dev.ahmedmohamed.hayaitts.ui.components.HayaiScreenChrome(
        title = installed?.title ?: card?.title ?: voiceId,
        subtitle = subtitle,
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = stringResource(R.string.action_back),
                )
            }
        },
        actions = {
            IconButton(onClick = onOpenQuickSwitch) {
                Icon(
                    Icons.Outlined.RecordVoiceOver,
                    contentDescription = stringResource(R.string.quick_switch_title),
                )
            }
            Box {
                IconButton(onClick = { overflowOpen = true }) {
                    Icon(
                        Icons.Outlined.MoreVert,
                        contentDescription = stringResource(R.string.voice_detail_actions_overflow),
                    )
                }
                DropdownMenu(
                    expanded = overflowOpen,
                    onDismissRequest = { overflowOpen = false },
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.voice_detail_open_playground)) },
                        leadingIcon = {
                            Icon(Icons.Outlined.Tune, contentDescription = null)
                        },
                        enabled = state.isInstalled,
                        onClick = {
                            overflowOpen = false
                            onOpenPlayground()
                        },
                    )
                    // Cloning entry — only visible for voices whose family
                    // supports reference-audio cloning AND that are already
                    // installed (the screen needs the JNI engine loaded).
                    if (state.isInstalled && (
                            card?.modelFamily?.supportsCloning == true ||
                                installed?.family?.supportsCloning == true
                            )
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.cloning_open_action)) },
                            leadingIcon = {
                                Icon(
                                    Icons.Outlined.RecordVoiceOver,
                                    contentDescription = null,
                                )
                            },
                            onClick = {
                                overflowOpen = false
                                onOpenCloning()
                            },
                        )
                    }
                    if (state.isInstalled && installed != null && !installed.bundled) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.voice_detail_uninstall_action)) },
                            leadingIcon = {
                                Icon(Icons.Outlined.Delete, contentDescription = null)
                            },
                            onClick = {
                                overflowOpen = false
                                viewModel.uninstall()
                            },
                        )
                    }
                }
            }
        },
    ) { topInset ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = dev.ahmedmohamed.hayaitts.ui.theme.Spacing.screenHorizontal)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(dev.ahmedmohamed.hayaitts.ui.theme.Spacing.itemSpacing),
        ) {
            Spacer(Modifier.height(topInset))
            HeroBlock(state = state)
            // Audition block: streams the upstream-rendered MP3 sample so the
            // user can hear the voice *before* downloading the model. When the
            // voice has multiple speakers or languages, the pickers let the
            // user choose which variant they hear.
            if (!state.isInstalled) {
                AuditionBlock(state = state, context = context)
            }
            if (state.isInstalled) {
                Text(
                    text = stringResource(R.string.voice_detail_speakers),
                    style = MaterialTheme.typography.titleMedium,
                )
                dev.ahmedmohamed.hayaitts.ui.components.HayaiSpeakerPickerAvatars(
                    speakers = state.installed?.speakers ?: state.card?.speakers.orEmpty(),
                    selectedSid = state.selectedSid,
                    onPick = viewModel::setSpeaker,
                )
            }
            PreviewSection(
                enabled = state.isInstalled,
                playing = state.previewing,
                generating = state.generating,
                amplitudes = amplitudes,
                accent = accent,
                onPlay = viewModel::play,
                onStop = viewModel::stop,
            )
            if (state.isInstalled && installed != null) {
                DefaultLocaleSection(
                    locales = installed.languages,
                    defaulted = state.defaultedLocales(),
                    onToggleDefault = viewModel::setDefault,
                )
            }
            PrimaryAction(
                state = state,
                onInstall = viewModel::install,
                onCancel = viewModel::cancel,
            )
            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun AuditionBlock(
    state: VoiceDetailViewModel.UiState,
    @Suppress("UNUSED_PARAMETER") context: android.content.Context,
) {
    val card = state.card
    val speakers = card?.speakers.orEmpty()
    val languages = card?.languages.orEmpty()
    var selectedSid by remember(card?.id) {
        mutableStateOf(speakers.firstOrNull()?.id ?: 0)
    }
    var selectedLang by remember(card?.id) {
        mutableStateOf(languages.firstOrNull() ?: "")
    }
    val sampleUrl = card?.sampleFor(selectedSid, selectedLang.ifBlank { null }) ?: return

    // Compact audition block: the Play button is the primary affordance and
    // sits inline with the speaker / language pickers on a single row when
    // they're needed. The web-demo "Try in browser" link has been removed —
    // it was a duplicate of the audition (and most upstream demo pages are
    // dead links anyway).
    Column(
        verticalArrangement = Arrangement.spacedBy(dev.ahmedmohamed.hayaitts.ui.theme.Spacing.chipSpacing),
    ) {
        Text(
            text = stringResource(R.string.voice_detail_preview),
            style = MaterialTheme.typography.titleMedium,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            dev.ahmedmohamed.hayaitts.ui.components.HayaiSampleAuditionButton(
                url = sampleUrl,
                style = dev.ahmedmohamed.hayaitts.ui.components.AuditionStyle.Filled,
            )
            if (speakers.size > 1) {
                dev.ahmedmohamed.hayaitts.ui.components.HayaiSpeakerPickerInline(
                    speakers = speakers,
                    selectedSid = selectedSid,
                    onPick = { selectedSid = it },
                )
            }
            if (languages.size > 1) {
                dev.ahmedmohamed.hayaitts.ui.components.HayaiLanguagePickerInline(
                    languages = languages,
                    selected = selectedLang,
                    onPick = { selectedLang = it },
                )
            }
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
    val speakers = installed?.speakers?.size ?: card?.speakers?.size ?: 1
    val familyForBadge = family ?: dev.ahmedmohamed.hayaitts.domain.model.ModelFamily.PIPER

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Hero shape — replaces the duplicate title that used to live here.
        // Title is now in the HayaiTopBar; the body opens with a strong
        // visual badge instead of a competing display-size text.
        HeroShape(family = familyForBadge)
        val subtitleLine = buildString {
            append(stringResource(R.string.voice_detail_speakers_count, speakers))
            if (sampleRate != null) {
                append(" · ")
                append(stringResource(R.string.voice_detail_sample_rate, sampleRate))
            }
        }
        Text(
            text = subtitleLine,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ChipRow {
            languages.forEach { LanguageChip(it) }
            family?.let { FamilyChip(it) }
            tier?.let { TierChip(tier = it, sizeMb = size) }
        }
        if (license != null) {
            Text(
                text = stringResource(R.string.voice_detail_license, license),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun HeroShape(
    family: dev.ahmedmohamed.hayaitts.domain.model.ModelFamily,
) {
    val identity = family.identityOrDefault()
    val transition = androidx.compose.animation.core.rememberInfiniteTransition(label = "hero-morph")
    val corner by transition.animateFloat(
        initialValue = 32f,
        targetValue = 50f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(durationMillis = 7_000),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse,
        ),
        label = "hero-morph-corner",
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(corner))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = identity.icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(56.dp),
        )
    }
}


@Composable
private fun PreviewSection(
    enabled: Boolean,
    playing: Boolean,
    generating: Boolean,
    amplitudes: FloatArray,
    accent: Color,
    onPlay: (String) -> Unit,
    onStop: () -> Unit,
) {
    val defaultText = stringResource(R.string.voice_detail_preview_text)
    var text by remember { mutableStateOf(defaultText) }
    val active = playing || generating

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.voice_detail_preview),
            style = MaterialTheme.typography.titleMedium,
        )
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            maxLines = 3,
        )
        if (!enabled) {
            Text(
                text = stringResource(R.string.voice_detail_preview_not_installed),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        WaveformBars(
            amplitudes = amplitudes,
            accent = accent,
            playing = playing,
        )
        HayaiRichTooltipBox(
            title = stringResource(R.string.tooltip_preview_play_title),
            description = stringResource(R.string.tooltip_preview_play_body),
        ) {
            FilledTonalButton(
                onClick = {
                    if (active) onStop() else onPlay(text)
                },
                enabled = enabled && text.isNotBlank() && !generating,
            ) {
                // Three labels in priority order: Generating… (synth in
                // flight), Stop (playback in flight), Play (idle). Same
                // affordance the Studio playground uses.
                val labelRes = when {
                    generating -> R.string.voice_detail_preview_generating
                    playing -> R.string.action_stop
                    else -> R.string.action_play
                }
                AnimatedContent(
                    targetState = labelRes,
                    transitionSpec = dev.ahmedmohamed.hayaitts.ui.theme.HayaiMotion.swap(),
                    label = "play-icon",
                ) { label ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = when (label) {
                                R.string.action_stop -> Icons.Outlined.Stop
                                R.string.voice_detail_preview_generating -> Icons.Outlined.HourglassEmpty
                                else -> Icons.Outlined.PlayArrow
                            },
                            contentDescription = null,
                        )
                        Spacer(Modifier.size(8.dp))
                        Text(stringResource(label))
                    }
                }
            }
        }
    }
}

@Composable
private fun WaveformBars(
    amplitudes: FloatArray,
    accent: Color,
    playing: Boolean,
) {
    // Matches the 32-bin RMS envelope published by VoicePreviewPlayer.amplitudes.
    // Keeping the bar count equal to the source resolution skips the downsample
    // step in WaveformBars and lets the UI render each bin 1:1.
    val bars = 32
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(bars) { i ->
            val sourceIdx = (i.toFloat() / bars * amplitudes.size).toInt().coerceIn(0, amplitudes.lastIndex.coerceAtLeast(0))
            val target = if (amplitudes.isEmpty()) 0.1f else amplitudes[sourceIdx]
            val baseline = if (playing) 0.18f else 0.08f
            val height by animateFloatAsState(
                targetValue = (target.coerceAtLeast(baseline)).coerceIn(0.05f, 1f),
                animationSpec = spring(stiffness = 600f),
                label = "wave-bar-$i",
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(height)
                    .clip(RoundedCornerShape(50))
                    .background(accent),
            )
        }
    }
}

/**
 * Groups the per-locale "Set default" toggles into one labeled section. Each
 * locale renders as a [FilterChip]-style assist chip — selected when this
 * voice is the current default for that locale.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DefaultLocaleSection(
    locales: List<String>,
    defaulted: Set<String>,
    onToggleDefault: (locale: String) -> Unit,
) {
    if (locales.isEmpty()) return
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.voice_detail_set_default_header),
            style = MaterialTheme.typography.titleMedium,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            locales.forEach { locale ->
                val isDefault = locale in defaulted
                AssistChip(
                    onClick = { onToggleDefault(locale) },
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
                        {
                            Icon(
                                Icons.Outlined.CheckCircle,
                                contentDescription = null,
                            )
                        }
                    } else null,
                    colors = AssistChipDefaults.assistChipColors(),
                )
            }
        }
    }
}

/**
 * One primary action button anchored at the bottom of the scroll body. The
 * label and behaviour switches between Install / Cancel-download /
 * Re-install-available / Coming-soon based on the current state. Download
 * progress (when running) is rendered above the button so we don't stack a
 * progress strip beside an action.
 */
@Composable
private fun PrimaryAction(
    state: VoiceDetailViewModel.UiState,
    onInstall: () -> Unit,
    onCancel: () -> Unit,
) {
    val ds = state.downloadState
    val running = ds is DownloadState.Running || ds is DownloadState.Extracting || ds is DownloadState.Queued
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        when {
            running -> {
                DownloadProgress(state = ds)
                Button(
                    onClick = onCancel,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Outlined.Cancel, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text(stringResource(R.string.voice_detail_cancel_primary))
                }
            }
            state.isInstalled -> {
                // Primary action is consumed by the per-locale chips above;
                // installed voices have no body-level CTA. The toolbar
                // overflow handles Uninstall and Playground.
            }
            state.card != null && state.card.available -> {
                Button(
                    onClick = onInstall,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Outlined.CloudDownload, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text(stringResource(R.string.voice_detail_install_primary))
                }
            }
            state.card != null && !state.card.available -> {
                AssistChip(
                    onClick = {},
                    enabled = false,
                    label = { Text(stringResource(R.string.voice_chip_coming_soon)) },
                )
                Text(
                    text = stringResource(R.string.voice_detail_coming_soon_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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

