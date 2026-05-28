@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package dev.ahmedmohamed.hayaitts.ui.settings

import android.content.Intent
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Search
import androidx.core.net.toUri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.ui.Alignment
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
import androidx.compose.material.icons.automirrored.outlined.Help
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
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
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.ahmedmohamed.hayaitts.BuildConfig
import dev.ahmedmohamed.hayaitts.R
import dev.ahmedmohamed.hayaitts.data.update.UpdateStatus
import dev.ahmedmohamed.hayaitts.domain.model.UpdateChannel
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
                    val updateVm: UpdateViewModel = koinViewModel()
                    SettingsScreen(onBack = { finish() }, updateViewModel = updateVm)

                    // Hayai's UpdateDialog is hosted in MainActivity for the
                    // in-app Settings tab, but when SettingsActivity is opened
                    // standalone (system TTS engine settings cog) MainActivity
                    // is not running. Host the dialog here too so the install
                    // flow remains reachable from either entry point.
                    val status by updateVm.status.collectAsStateWithLifecycle()
                    val download by updateVm.download.collectAsStateWithLifecycle()
                    val dismissed by updateVm.dialogDismissed.collectAsStateWithLifecycle()
                    val available = status as? UpdateStatus.Available
                    if (available != null && !dismissed) {
                        dev.ahmedmohamed.hayaitts.ui.update.UpdateDialog(
                            available = available,
                            download = download,
                            onInstall = updateVm::startInstall,
                            onCancelInstall = updateVm::cancelInstall,
                            onDismiss = updateVm::dismissDialog,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(
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
    val resources = LocalResources.current
    val upToDateMessage = stringResource(R.string.settings_update_uptodate)
    val installActionLabel = stringResource(R.string.update_action_install)
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        state = rememberTopAppBarState(),
    )

    var defaultPickerLocale by remember { mutableStateOf<String?>(null) }
    var licenseDialogOpen by remember { mutableStateOf(false) }
    var channelPickerOpen by remember { mutableStateOf(false) }
    var threadsDialogOpen by remember { mutableStateOf(false) }
    var maxSentencesDialogOpen by remember { mutableStateOf(false) }
    var languagePickerOpen by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val allowedLanguages by viewModel.allowedLanguages.collectAsStateWithLifecycle()
    val catalogLanguages by viewModel.catalogLanguages.collectAsStateWithLifecycle()

    // Section + item labels resolved up front so search can match against them
    // outside of @Composable-scope inside the LazyColumn item lambdas. Each
    // label set is the headline + supporting text of one ListItem; the
    // section-header check just folds across its items' labels.
    val labelDownloadsSection = stringResource(R.string.settings_section_downloads)
    val labelWifi = stringResource(R.string.settings_wifi_only) +
        " " + stringResource(R.string.settings_wifi_only_subtitle)
    val labelStorageLoc = stringResource(R.string.settings_storage_location)
    val labelStorageInternal = stringResource(R.string.settings_storage_internal)
    val labelStorageExternal =
        if (state.hasExternalStorage) stringResource(R.string.settings_storage_external)
        else stringResource(R.string.settings_storage_external_unavailable)

    val labelDefaultsSection = stringResource(R.string.settings_section_defaults)
    val labelDefaultsEmpty = stringResource(R.string.settings_defaults_empty)
    val labelAllowedLanguages = stringResource(R.string.settings_allowed_languages) +
        " " + stringResource(R.string.settings_allowed_languages_subtitle)

    val labelStorageSection = stringResource(R.string.settings_section_storage)
    val labelTotalSize = stringResource(
        R.string.settings_total_models_size,
        formatBytes(state.totalInstalledBytes),
    )
    val labelClearCache = stringResource(R.string.settings_clear_cache) +
        " " + stringResource(R.string.settings_clear_cache_subtitle)

    val labelPerfSection = stringResource(R.string.settings_section_performance)
    val labelNnapi = stringResource(R.string.settings_nnapi) +
        " " + stringResource(R.string.settings_nnapi_subtitle)
    val labelThreads = stringResource(R.string.settings_threads) +
        " " + stringResource(R.string.settings_threads_subtitle)
    val labelMaxSentences = stringResource(R.string.settings_max_sentences) +
        " " + stringResource(R.string.settings_max_sentences_subtitle)

    val labelUpdatesSection = stringResource(R.string.settings_section_updates)
    val labelUpdateChannel = stringResource(R.string.settings_update_channel)
    val labelCurrentVersion = stringResource(R.string.settings_current_version) +
        " " + BuildConfig.VERSION_NAME
    val labelCheckUpdates = stringResource(R.string.settings_check_for_updates) +
        " " + stringResource(R.string.settings_check_for_updates_subtitle)
    val labelLastChecked = stringResource(R.string.settings_last_checked)

    val labelAboutSection = stringResource(R.string.settings_section_about)
    val labelAppName = stringResource(R.string.app_name)
    val labelHelp = stringResource(R.string.settings_help_label) +
        " " + stringResource(R.string.settings_help_subtitle)
    val labelLicense = stringResource(R.string.settings_license_label) +
        " " + stringResource(R.string.settings_license_subtitle)
    val labelPoweredBy = stringResource(R.string.settings_powered_by) +
        " " + stringResource(R.string.settings_powered_by_subtitle)

    val q = searchQuery.trim()
    fun matches(vararg parts: String): Boolean =
        q.isEmpty() || parts.any { it.contains(q, ignoreCase = true) }

    val showDownloadsSection = matches(labelWifi, labelStorageLoc, labelStorageInternal, labelStorageExternal)
    val showDefaultsSection = matches(
        labelDefaultsEmpty,
        labelAllowedLanguages,
        *state.installedLocales.toTypedArray(),
    )
    val showStorageSection = matches(labelTotalSize, labelClearCache)
    val showPerfSection = matches(labelNnapi, labelThreads, labelMaxSentences)
    val showUpdatesSection = matches(labelUpdateChannel, labelCurrentVersion, labelCheckUpdates, labelLastChecked)
    val showAboutSection = matches(labelAppName, labelHelp, labelLicense, labelPoweredBy)

    // Surface a snackbar for every terminal update-status transition kicked off
    // from this screen. The launch-time auto-check is handled in MainActivity.
    LaunchedEffect(updateStatus) {
        when (val s = updateStatus) {
            is UpdateStatus.UpToDate -> {
                snackbarHostState.showSnackbar(upToDateMessage)
                updateViewModel.consumeStatus()
            }
            is UpdateStatus.Available -> {
                val result = snackbarHostState.showSnackbar(
                    message = resources.getString(R.string.settings_update_available, s.tag),
                    actionLabel = installActionLabel,
                    withDismissAction = true,
                )
                if (result == androidx.compose.material3.SnackbarResult.ActionPerformed) {
                    updateViewModel.startInstall()
                }
                // Do NOT consume — the hosted UpdateDialog observes the same state
                // and will surface install progress.
            }
            is UpdateStatus.Failed -> {
                snackbarHostState.showSnackbar(
                    resources.getString(R.string.settings_update_failed, s.reason),
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
            resources.getString(R.string.settings_clear_cache_done, formatBytes(freed)),
        )
        viewModel.consumeCacheClearedEvent()
    }

    dev.ahmedmohamed.hayaitts.ui.components.HayaiScreenChrome(
        title = stringResource(R.string.settings_title),
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = stringResource(R.string.action_back),
                )
            }
        },
        searchable = dev.ahmedmohamed.hayaitts.ui.components.HayaiSearchable(
            query = searchQuery,
            onQueryChange = { searchQuery = it },
            placeholder = stringResource(R.string.topbar_search_settings),
        ),
        snackbarHostState = snackbarHostState,
    ) { topInset ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = topInset, bottom = 16.dp),
        ) {
            if (showDownloadsSection) item("downloads_header") { SectionHeader(labelDownloadsSection) }
            if (showDownloadsSection && matches(labelWifi)) item("wifi_only") {
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
            if (showDownloadsSection && matches(labelStorageLoc)) item("storage_location_header") {
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
            if (showDownloadsSection && matches(labelStorageInternal, labelStorageLoc)) item("storage_internal") {
                StorageRadioRow(
                    label = stringResource(R.string.settings_storage_internal),
                    selected = state.storageLocation == StorageLocation.INTERNAL,
                    enabled = !state.isMoving,
                    onSelect = { viewModel.setStorageLocation(StorageLocation.INTERNAL) },
                )
            }
            if (showDownloadsSection && matches(labelStorageExternal, labelStorageLoc)) item("storage_external") {
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

            if (showDefaultsSection) item("defaults_header") { SectionHeader(labelDefaultsSection) }
            if (showDefaultsSection && matches(labelAllowedLanguages)) item("allowed_languages") {
                ListItem(
                    leadingContent = {
                        Icon(
                            androidx.compose.material.icons.Icons.Outlined.Language,
                            contentDescription = null,
                        )
                    },
                    headlineContent = { Text(stringResource(R.string.settings_allowed_languages)) },
                    supportingContent = {
                        val n = allowedLanguages.size
                        Text(
                            if (n == 0) stringResource(R.string.settings_allowed_languages_all)
                            else stringResource(R.string.settings_allowed_languages_count, n),
                        )
                    },
                    modifier = Modifier.clickableRow { languagePickerOpen = true },
                )
            }
            if (showDefaultsSection && state.installedLocales.isEmpty()) {
                item("defaults_empty") {
                    ListItem(headlineContent = { Text(stringResource(R.string.settings_defaults_empty)) })
                }
            } else if (showDefaultsSection) {
                items(state.installedLocales.filter { matches(it) }) { locale ->
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

            if (showStorageSection) item("storage_header") { SectionHeader(labelStorageSection) }
            if (showStorageSection && matches(labelTotalSize)) item("total_size") {
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
            if (showStorageSection && matches(labelClearCache)) item("clear_cache") {
                ListItem(
                    leadingContent = { Icon(Icons.Outlined.CleaningServices, contentDescription = null) },
                    headlineContent = { Text(stringResource(R.string.settings_clear_cache)) },
                    supportingContent = { Text(stringResource(R.string.settings_clear_cache_subtitle)) },
                    modifier = Modifier.clickableRow { viewModel.clearDownloadCache() },
                )
            }

            if (showPerfSection) item("performance_header") { SectionHeader(labelPerfSection) }
            if (showPerfSection && matches(labelNnapi)) item("nnapi") {
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
            if (showPerfSection && matches(labelThreads)) item("threads") {
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
            if (showPerfSection && matches(labelMaxSentences)) item("max_sentences") {
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

            if (showUpdatesSection) item("updates_header") { SectionHeader(labelUpdatesSection) }
            if (showUpdatesSection && matches(labelUpdateChannel)) item("update_channel") {
                ListItem(
                    leadingContent = { Icon(Icons.Outlined.Update, contentDescription = null) },
                    headlineContent = { Text(stringResource(R.string.settings_update_channel)) },
                    supportingContent = { Text(updateChannel.displayName()) },
                    modifier = Modifier.clickableRow { channelPickerOpen = true },
                )
            }
            if (showUpdatesSection && matches(labelCurrentVersion)) item("update_current_version") {
                ListItem(
                    leadingContent = { Icon(Icons.Outlined.Info, contentDescription = null) },
                    headlineContent = { Text(stringResource(R.string.settings_current_version)) },
                    supportingContent = {
                        Text(
                            stringResource(
                                R.string.settings_current_version_value,
                                buildChannelLabel(BuildConfig.VERSION_NAME),
                                BuildConfig.VERSION_NAME,
                                BuildConfig.VERSION_CODE,
                            ),
                        )
                    },
                )
            }
            if (showUpdatesSection && matches(labelCheckUpdates)) item("update_check_now") {
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
            if (showUpdatesSection && matches(labelLastChecked)) item("update_last_checked") {
                ListItem(
                    leadingContent = { Icon(Icons.Outlined.Schedule, contentDescription = null) },
                    headlineContent = { Text(stringResource(R.string.settings_last_checked)) },
                    supportingContent = { Text(formatRelativeTime(lastChecked)) },
                )
            }

            if (showAboutSection) item("about_header") { SectionHeader(labelAboutSection) }
            if (showAboutSection && matches(labelAppName)) item("version") {
                ListItem(
                    leadingContent = { Icon(Icons.Outlined.Info, contentDescription = null) },
                    headlineContent = { Text(stringResource(R.string.app_name)) },
                    supportingContent = {
                        Text(
                            stringResource(
                                R.string.settings_version,
                                buildChannelLabel(BuildConfig.VERSION_NAME),
                                BuildConfig.VERSION_NAME,
                                BuildConfig.VERSION_CODE,
                            ),
                        )
                    },
                )
            }
            if (showAboutSection && matches(labelHelp)) item("help") {
                ListItem(
                    leadingContent = { Icon(Icons.AutoMirrored.Outlined.Help, contentDescription = null) },
                    headlineContent = { Text(stringResource(R.string.settings_help_label)) },
                    supportingContent = { Text(stringResource(R.string.settings_help_subtitle)) },
                    modifier = Modifier.clickableRow {
                        context.startActivity(Intent(context, HelpActivity::class.java))
                    },
                )
            }
            if (showAboutSection && matches(labelLicense)) item("license") {
                ListItem(
                    leadingContent = { Icon(Icons.Outlined.Gavel, contentDescription = null) },
                    headlineContent = { Text(stringResource(R.string.settings_license_label)) },
                    supportingContent = { Text(stringResource(R.string.settings_license_subtitle)) },
                    modifier = Modifier.clickableRow { licenseDialogOpen = true },
                )
            }
            if (showAboutSection && matches(labelPoweredBy)) item("powered_by") {
                ListItem(
                    leadingContent = { Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = null) },
                    headlineContent = { Text(stringResource(R.string.settings_powered_by)) },
                    supportingContent = { Text(stringResource(R.string.settings_powered_by_subtitle)) },
                    modifier = Modifier.clickableRow {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, SHERPA_REPO_URL.toUri()),
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

    if (languagePickerOpen) {
        AllowedLanguagesSheet(
            available = catalogLanguages,
            selected = allowedLanguages,
            onCommit = { next ->
                viewModel.setAllowedLanguages(next)
                languagePickerOpen = false
            },
            onDismiss = { languagePickerOpen = false },
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
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.2.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
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

/**
 * Map [BuildConfig.VERSION_NAME] to one of "Stable" / "Beta" / "Nightly".
 *
 * The release workflow exports the git tag as `HAYAITTS_VERSION_NAME`, which
 * the gradle build strips of its leading `v` before baking into the APK. So
 * the patterns we look at here are:
 *   - `2.0.0` → Stable
 *   - `2.0.0-b3` → Beta
 *   - `r142` → Nightly (the nightly tag scheme used by build_push.yml)
 * Anything we can't classify falls back to "Stable" so the row never shows
 * a blank pill.
 */
@Composable
private fun buildChannelLabel(versionName: String): String = when {
    versionName.startsWith("r") && versionName.removePrefix("r").all { it.isDigit() } ->
        stringResource(R.string.settings_update_channel_nightly)
    versionName.contains("-b") ->
        stringResource(R.string.settings_update_channel_beta)
    else ->
        stringResource(R.string.settings_update_channel_stable)
}

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

/**
 * Modal bottom sheet for picking the global allowed-language set. The list is
 * a search-filtered FlowRow of FilterChips with a "Select all" / "Clear"
 * toggle in the action row. Empty selection persists as "no restriction".
 */
@Composable
private fun AllowedLanguagesSheet(
    available: List<String>,
    selected: Set<String>,
    onCommit: (Set<String>) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var draft by remember(selected) { mutableStateOf(selected) }
    var query by remember { mutableStateOf("") }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.settings_allowed_languages_sheet_title),
                style = MaterialTheme.typography.titleLarge,
            )
            androidx.compose.material3.OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Outlined.Close, contentDescription = null)
                        }
                    }
                },
                placeholder = {
                    Text(stringResource(R.string.browse_filter_language_search))
                },
                shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
            )
            val q = query.trim()
            val matching = remember(available, q, draft) {
                if (q.isEmpty()) available
                else available.filter { lang ->
                    lang.contains(q, ignoreCase = true) ||
                        dev.ahmedmohamed.hayaitts.ui.components.displayName(lang)
                            .contains(q, ignoreCase = true)
                }
            }
            androidx.compose.foundation.layout.FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                matching.forEach { lang ->
                    val isOn = lang in draft
                    androidx.compose.material3.FilterChip(
                        selected = isOn,
                        onClick = {
                            draft = if (isOn) draft - lang else draft + lang
                        },
                        label = {
                            Text(
                                dev.ahmedmohamed.hayaitts.ui.components.displayName(lang),
                            )
                        },
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    onClick = {
                        draft = if (draft.size == available.size) emptySet()
                            else available.toSet()
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.settings_allowed_languages_select_all))
                }
                Button(
                    onClick = { onCommit(draft) },
                    modifier = Modifier.weight(2f),
                ) {
                    Text(stringResource(R.string.browse_filter_show_results, draft.size))
                }
            }
        }
    }
}
