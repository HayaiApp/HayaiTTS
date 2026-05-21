@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package dev.ahmedmohamed.hayaitts.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.Help
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.SdStorage
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.ahmedmohamed.hayaitts.BuildConfig
import dev.ahmedmohamed.hayaitts.R
import dev.ahmedmohamed.hayaitts.data.update.UpdateChannel
import dev.ahmedmohamed.hayaitts.data.update.UpdateStatus
import dev.ahmedmohamed.hayaitts.domain.model.InstalledVoice
import dev.ahmedmohamed.hayaitts.domain.model.StorageLocation
import dev.ahmedmohamed.hayaitts.ui.help.HelpActivity
import dev.ahmedmohamed.hayaitts.ui.theme.HayaiTtsTheme
import dev.ahmedmohamed.hayaitts.ui.update.UpdateViewModel
import org.koin.androidx.compose.koinViewModel

/**
 * Engine-side TTS settings screen. Wired into the manifest as the
 * `android.speech.tts.engine.SETTINGS` activity — the user reaches it by tapping
 * the ⚙ next to "HayaiTTS" in system TTS settings.
 *
 * Sections (M3 Expressive list groups): Downloads, Default voices, Storage,
 * About. Storage location changes do NOT migrate existing voices — see the
 * inline "Restart required" hint.
 */
class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HayaiTtsTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SettingsScreen(onBack = { finish() })
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel(),
    updateViewModel: UpdateViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val cacheClearedBytes by viewModel.cacheClearedBytes.collectAsStateWithLifecycle()
    val updateStatus by updateViewModel.status.collectAsStateWithLifecycle()
    val updateChannel by updateViewModel.updateChannel.collectAsStateWithLifecycle()
    val lastChecked by updateViewModel.lastChecked.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        state = rememberTopAppBarState(),
    )

    var defaultPickerLocale by remember { mutableStateOf<String?>(null) }
    var licenseDialogOpen by remember { mutableStateOf(false) }
    var channelPickerOpen by remember { mutableStateOf(false) }
    var threadsDialogOpen by remember { mutableStateOf(false) }
    var maxSentencesDialogOpen by remember { mutableStateOf(false) }

    // Surface a snackbar for every terminal update-status transition kicked off
    // from this screen. The launch-time auto-check is handled in MainActivity.
    LaunchedEffect(updateStatus) {
        when (val s = updateStatus) {
            is UpdateStatus.UpToDate -> {
                snackbarHostState.showSnackbar(context.getString(R.string.settings_update_uptodate))
                updateViewModel.consumeStatus()
            }
            is UpdateStatus.Available -> {
                snackbarHostState.showSnackbar(
                    context.getString(R.string.settings_update_available, s.tag),
                )
                // Do NOT consume — MainActivity's dialog observes the same state.
            }
            is UpdateStatus.Failed -> {
                snackbarHostState.showSnackbar(
                    context.getString(R.string.settings_update_failed, s.reason),
                )
                updateViewModel.consumeStatus()
            }
            else -> Unit
        }
    }

    // Surface the freed-bytes snackbar exactly once per clear action.
    LaunchedEffect(cacheClearedBytes) {
        val freed = cacheClearedBytes ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(
            context.getString(R.string.settings_clear_cache_done, formatBytes(freed)),
        )
        viewModel.consumeCacheClearedEvent()
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                subtitle = { Text(stringResource(R.string.settings_subtitle)) },
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(vertical = 8.dp),
        ) {
            item("downloads_header") { SectionHeader(stringResource(R.string.settings_section_downloads)) }
            item("wifi_only") {
                ListItem(
                    leadingContent = { Icon(Icons.Outlined.Wifi, contentDescription = null) },
                    headlineContent = { Text(stringResource(R.string.settings_wifi_only)) },
                    supportingContent = { Text(stringResource(R.string.settings_wifi_only_subtitle)) },
                    trailingContent = {
                        Switch(
                            checked = state.wifiOnly,
                            onCheckedChange = viewModel::setWifiOnly,
                        )
                    },
                )
            }
            item("storage_location_header") {
                ListItem(
                    leadingContent = { Icon(Icons.Outlined.SdStorage, contentDescription = null) },
                    headlineContent = { Text(stringResource(R.string.settings_storage_location)) },
                    supportingContent = {
                        // Hide the "restart required" hint now that the
                        // migrator actually relocates voices on toggle. Show
                        // a live progress line instead when a move is in
                        // flight.
                        val progress = state.moveProgress
                        if (progress is dev.ahmedmohamed.hayaitts.domain.model.MoveProgress.Moving) {
                            val total = progress.totalCount.coerceAtLeast(1)
                            Text(
                                stringResource(
                                    R.string.settings_storage_moving,
                                    progress.doneCount,
                                    total,
                                    progress.currentVoiceId.orEmpty(),
                                ),
                            )
                        } else if (progress is dev.ahmedmohamed.hayaitts.domain.model.MoveProgress.Failed) {
                            Text(
                                stringResource(R.string.settings_storage_move_failed, progress.reason),
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    },
                )
                if (state.isMoving) {
                    val progress = state.moveProgress as dev.ahmedmohamed.hayaitts.domain.model.MoveProgress.Moving
                    androidx.compose.material3.LinearWavyProgressIndicator(
                        progress = { progress.fraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }
            }
            item("storage_internal") {
                StorageRadioRow(
                    label = stringResource(R.string.settings_storage_internal),
                    selected = state.storageLocation == StorageLocation.INTERNAL,
                    enabled = !state.isMoving,
                    onSelect = { viewModel.setStorageLocation(StorageLocation.INTERNAL) },
                )
            }
            item("storage_external") {
                StorageRadioRow(
                    label = if (state.hasExternalStorage) {
                        stringResource(R.string.settings_storage_external)
                    } else {
                        stringResource(R.string.settings_storage_external_unavailable)
                    },
                    selected = state.storageLocation == StorageLocation.EXTERNAL,
                    enabled = state.hasExternalStorage && !state.isMoving,
                    onSelect = { viewModel.setStorageLocation(StorageLocation.EXTERNAL) },
                )
            }

            item("divider_1") { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }
            item("defaults_header") { SectionHeader(stringResource(R.string.settings_section_defaults)) }
            if (state.installedLocales.isEmpty()) {
                item("defaults_empty") {
                    ListItem(headlineContent = { Text(stringResource(R.string.settings_defaults_empty)) })
                }
            } else {
                items(state.installedLocales) { locale ->
                    val currentVoice = state.defaultsByLocale[locale]
                    val voiceTitle = state.installed.firstOrNull { it.voiceId == currentVoice }?.title
                    ListItem(
                        leadingContent = { Icon(Icons.Outlined.Folder, contentDescription = null) },
                        headlineContent = { Text(locale) },
                        supportingContent = {
                            Text(
                                voiceTitle ?: stringResource(R.string.settings_pick_voice_clear),
                            )
                        },
                        modifier = Modifier.clickableRow { defaultPickerLocale = locale },
                    )
                }
            }

            item("divider_2") { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }
            item("storage_header") { SectionHeader(stringResource(R.string.settings_section_storage)) }
            item("total_size") {
                ListItem(
                    leadingContent = { Icon(Icons.Outlined.Folder, contentDescription = null) },
                    headlineContent = {
                        Text(
                            stringResource(
                                R.string.settings_total_models_size,
                                formatBytes(state.totalInstalledBytes),
                            ),
                        )
                    },
                    modifier = Modifier.clickableRow { viewModel.refreshInstalledSize() },
                )
            }
            item("clear_cache") {
                ListItem(
                    leadingContent = { Icon(Icons.Outlined.CleaningServices, contentDescription = null) },
                    headlineContent = { Text(stringResource(R.string.settings_clear_cache)) },
                    supportingContent = { Text(stringResource(R.string.settings_clear_cache_subtitle)) },
                    modifier = Modifier.clickableRow { viewModel.clearDownloadCache() },
                )
            }

            item("divider_performance") { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }
            item("performance_header") { SectionHeader(stringResource(R.string.settings_section_performance)) }
            item("nnapi") {
                ListItem(
                    leadingContent = { Icon(Icons.Outlined.Bolt, contentDescription = null) },
                    headlineContent = { Text(stringResource(R.string.settings_nnapi)) },
                    supportingContent = { Text(stringResource(R.string.settings_nnapi_subtitle)) },
                    trailingContent = {
                        Switch(
                            checked = state.useNnapi,
                            onCheckedChange = viewModel::setUseNnapi,
                        )
                    },
                )
            }
            item("threads") {
                ListItem(
                    leadingContent = { Icon(Icons.Outlined.Tune, contentDescription = null) },
                    headlineContent = { Text(stringResource(R.string.settings_threads)) },
                    supportingContent = { Text(stringResource(R.string.settings_threads_subtitle)) },
                    trailingContent = {
                        Text(
                            stringResource(R.string.settings_threads_value, state.synthesisThreads),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    },
                    modifier = Modifier.clickableRow { threadsDialogOpen = true },
                )
            }
            item("max_sentences") {
                ListItem(
                    leadingContent = { Icon(Icons.Outlined.Info, contentDescription = null) },
                    headlineContent = { Text(stringResource(R.string.settings_max_sentences)) },
                    supportingContent = { Text(stringResource(R.string.settings_max_sentences_subtitle)) },
                    trailingContent = {
                        Text(
                            stringResource(R.string.settings_max_sentences_value, state.maxNumSentences),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    },
                    modifier = Modifier.clickableRow { maxSentencesDialogOpen = true },
                )
            }

            item("divider_updates") { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }
            item("updates_header") { SectionHeader(stringResource(R.string.settings_section_updates)) }
            item("update_channel") {
                ListItem(
                    leadingContent = { Icon(Icons.Outlined.Update, contentDescription = null) },
                    headlineContent = { Text(stringResource(R.string.settings_update_channel)) },
                    supportingContent = { Text(updateChannel.displayName()) },
                    modifier = Modifier.clickableRow { channelPickerOpen = true },
                )
            }
            item("update_current_version") {
                ListItem(
                    leadingContent = { Icon(Icons.Outlined.Info, contentDescription = null) },
                    headlineContent = { Text(stringResource(R.string.settings_current_version)) },
                    supportingContent = {
                        Text(
                            stringResource(
                                R.string.settings_current_version_value,
                                BuildConfig.VERSION_NAME,
                                BuildConfig.VERSION_CODE,
                            ),
                        )
                    },
                )
            }
            item("update_check_now") {
                val isChecking = updateStatus is UpdateStatus.Checking
                ListItem(
                    leadingContent = { Icon(Icons.Outlined.Refresh, contentDescription = null) },
                    headlineContent = {
                        Text(
                            if (isChecking) stringResource(R.string.settings_checking_for_updates)
                            else stringResource(R.string.settings_check_for_updates),
                        )
                    },
                    supportingContent = {
                        Text(stringResource(R.string.settings_check_for_updates_subtitle))
                    },
                    modifier = if (isChecking) Modifier else Modifier.clickableRow {
                        updateViewModel.checkNow(force = true)
                    },
                )
            }
            item("update_last_checked") {
                ListItem(
                    leadingContent = { Icon(Icons.Outlined.Schedule, contentDescription = null) },
                    headlineContent = { Text(stringResource(R.string.settings_last_checked)) },
                    supportingContent = { Text(formatRelativeTime(lastChecked)) },
                )
            }

            item("divider_3") { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }
            item("about_header") { SectionHeader(stringResource(R.string.settings_section_about)) }
            item("version") {
                ListItem(
                    leadingContent = { Icon(Icons.Outlined.Info, contentDescription = null) },
                    headlineContent = { Text(stringResource(R.string.app_name)) },
                    supportingContent = {
                        Text(
                            stringResource(
                                R.string.settings_version,
                                BuildConfig.VERSION_NAME,
                                BuildConfig.VERSION_CODE,
                            ),
                        )
                    },
                )
            }
            item("help") {
                ListItem(
                    leadingContent = { Icon(Icons.Outlined.Help, contentDescription = null) },
                    headlineContent = { Text(stringResource(R.string.settings_help_label)) },
                    supportingContent = { Text(stringResource(R.string.settings_help_subtitle)) },
                    modifier = Modifier.clickableRow {
                        context.startActivity(Intent(context, HelpActivity::class.java))
                    },
                )
            }
            item("license") {
                ListItem(
                    leadingContent = { Icon(Icons.Outlined.Gavel, contentDescription = null) },
                    headlineContent = { Text(stringResource(R.string.settings_license_label)) },
                    supportingContent = { Text(stringResource(R.string.settings_license_subtitle)) },
                    modifier = Modifier.clickableRow { licenseDialogOpen = true },
                )
            }
            item("powered_by") {
                ListItem(
                    leadingContent = { Icon(Icons.Outlined.OpenInNew, contentDescription = null) },
                    headlineContent = { Text(stringResource(R.string.settings_powered_by)) },
                    supportingContent = { Text(stringResource(R.string.settings_powered_by_subtitle)) },
                    modifier = Modifier.clickableRow {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse(SHERPA_REPO_URL)),
                        )
                    },
                )
            }
        }
    }

    defaultPickerLocale?.let { locale ->
        DefaultVoicePickerSheet(
            locale = locale,
            installed = state.installed,
            currentVoiceId = state.defaultsByLocale[locale],
            onPick = { voiceId ->
                viewModel.setDefault(locale, voiceId)
                defaultPickerLocale = null
            },
            onDismiss = { defaultPickerLocale = null },
        )
    }

    if (licenseDialogOpen) {
        LicenseDialog(onDismiss = { licenseDialogOpen = false })
    }

    if (channelPickerOpen) {
        UpdateChannelDialog(
            current = updateChannel,
            onPick = { picked ->
                updateViewModel.setChannel(picked)
                channelPickerOpen = false
            },
            onDismiss = { channelPickerOpen = false },
        )
    }

    if (threadsDialogOpen) {
        ThreadsDialog(
            current = state.synthesisThreads,
            onPick = { picked ->
                viewModel.setSynthesisThreads(picked)
                threadsDialogOpen = false
            },
            onDismiss = { threadsDialogOpen = false },
        )
    }

    if (maxSentencesDialogOpen) {
        MaxSentencesDialog(
            current = state.maxNumSentences,
            onPick = { picked ->
                viewModel.setMaxNumSentences(picked)
                maxSentencesDialogOpen = false
            },
            onDismiss = { maxSentencesDialogOpen = false },
        )
    }
}

@Composable
private fun UpdateChannelDialog(
    current: UpdateChannel,
    onPick: (UpdateChannel) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_update_channel_dialog_title)) },
        text = {
            Column {
                UpdateChannel.values().forEach { channel ->
                    val (label, subtitle) = when (channel) {
                        UpdateChannel.STABLE -> stringResource(R.string.settings_update_channel_stable) to
                            stringResource(R.string.settings_update_channel_stable_subtitle)
                        UpdateChannel.BETA -> stringResource(R.string.settings_update_channel_beta) to
                            stringResource(R.string.settings_update_channel_beta_subtitle)
                        UpdateChannel.NIGHTLY -> stringResource(R.string.settings_update_channel_nightly) to
                            stringResource(R.string.settings_update_channel_nightly_subtitle)
                    }
                    ListItem(
                        headlineContent = { Text(label) },
                        supportingContent = { Text(subtitle) },
                        trailingContent = {
                            RadioButton(
                                selected = current == channel,
                                onClick = { onPick(channel) },
                            )
                        },
                        modifier = Modifier.clickableRow { onPick(channel) },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.settings_license_dialog_close))
            }
        },
    )
}

@Composable
private fun UpdateChannel.displayName(): String = when (this) {
    UpdateChannel.STABLE -> stringResource(R.string.settings_update_channel_stable)
    UpdateChannel.BETA -> stringResource(R.string.settings_update_channel_beta)
    UpdateChannel.NIGHTLY -> stringResource(R.string.settings_update_channel_nightly)
}

/**
 * Compact "X min ago" / "X hr ago" / "X d ago" formatter for the
 * `lastUpdateCheckMillis` DataStore field. Returns the localized "Never" string
 * when no check has ever completed.
 */
@Composable
private fun formatRelativeTime(epochMs: Long): String {
    if (epochMs <= 0L) return stringResource(R.string.settings_last_checked_never)
    val delta = (System.currentTimeMillis() - epochMs).coerceAtLeast(0L)
    val minutes = delta / 60_000L
    val hours = delta / (60_000L * 60L)
    val days = delta / (60_000L * 60L * 24L)
    return when {
        minutes < 1L -> stringResource(R.string.settings_last_checked_just_now)
        minutes < 60L -> stringResource(R.string.settings_last_checked_minutes, minutes.toInt())
        hours < 24L -> stringResource(R.string.settings_last_checked_hours, hours.toInt())
        else -> stringResource(R.string.settings_last_checked_days, days.toInt())
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
    )
}

@Composable
private fun StorageRadioRow(
    label: String,
    selected: Boolean,
    enabled: Boolean = true,
    onSelect: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(label) },
        trailingContent = {
            RadioButton(
                selected = selected,
                onClick = onSelect,
                enabled = enabled,
            )
        },
        modifier = if (enabled) Modifier.clickableRow(onSelect) else Modifier,
    )
}

@Composable
private fun DefaultVoicePickerSheet(
    locale: String,
    installed: List<InstalledVoice>,
    currentVoiceId: String?,
    onPick: (voiceId: String?) -> Unit,
    onDismiss: () -> Unit,
) {
    val candidates = remember(installed, locale) {
        installed.filter { locale in it.languages }
    }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(R.string.settings_pick_voice_title, locale),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_pick_voice_clear)) },
                trailingContent = {
                    RadioButton(
                        selected = currentVoiceId == null,
                        onClick = { onPick(null) },
                    )
                },
                modifier = Modifier.clickableRow { onPick(null) },
            )
            candidates.forEach { voice ->
                ListItem(
                    headlineContent = { Text(voice.title) },
                    supportingContent = { Text(voice.languages.joinToString()) },
                    trailingContent = {
                        RadioButton(
                            selected = currentVoiceId == voice.voiceId,
                            onClick = { onPick(voice.voiceId) },
                        )
                    },
                    modifier = Modifier.clickableRow { onPick(voice.voiceId) },
                )
            }
        }
    }
}

@Composable
private fun LicenseDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val licenseText = remember {
        runCatching {
            context.assets.open("LICENSE.txt").bufferedReader().use { it.readText() }
        }.getOrElse { "License file unavailable." }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_license_dialog_title)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(licenseText, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.settings_license_dialog_close))
            }
        },
    )
}

/**
 * ListItem doesn't take onClick directly, so we route taps through a clickable
 * modifier on the whole row. Kept as a thin extension to avoid sprinkling the
 * import everywhere.
 */
private fun Modifier.clickableRow(onClick: () -> Unit): Modifier =
    this.clickable(onClick = onClick)

private const val SHERPA_REPO_URL = "https://github.com/k2-fsa/sherpa-onnx"

private fun formatBytes(bytes: Long): String {
    val mb = bytes.toDouble() / (1024.0 * 1024.0)
    return if (mb >= 1024.0) {
        "%.2f GB".format(mb / 1024.0)
    } else {
        "%.1f MB".format(mb)
    }
}

@Composable
private fun ThreadsDialog(
    current: Int,
    onPick: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val options = listOf(1, 2, 4, 8)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_threads_dialog_title)) },
        text = {
            Column {
                options.forEach { option ->
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.settings_threads_value, option)) },
                        trailingContent = {
                            RadioButton(
                                selected = current == option,
                                onClick = { onPick(option) },
                            )
                        },
                        modifier = Modifier.clickableRow { onPick(option) },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.settings_license_dialog_close))
            }
        },
    )
}

@Composable
private fun MaxSentencesDialog(
    current: Int,
    onPick: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val options = listOf(1, 2, 4, 8)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_max_sentences_dialog_title)) },
        text = {
            Column {
                options.forEach { option ->
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.settings_max_sentences_value, option)) },
                        trailingContent = {
                            RadioButton(
                                selected = current == option,
                                onClick = { onPick(option) },
                            )
                        },
                        modifier = Modifier.clickableRow { onPick(option) },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.settings_license_dialog_close))
            }
        },
    )
}
