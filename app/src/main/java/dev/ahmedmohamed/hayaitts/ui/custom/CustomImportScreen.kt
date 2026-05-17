@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package dev.ahmedmohamed.hayaitts.ui.custom

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Done
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.ahmedmohamed.hayaitts.R
import dev.ahmedmohamed.hayaitts.data.custom.CustomBundleAnalyzer
import dev.ahmedmohamed.hayaitts.data.custom.CustomBundleInstaller
import dev.ahmedmohamed.hayaitts.domain.model.ModelFamily
import dev.ahmedmohamed.hayaitts.ui.components.displayName
import dev.ahmedmohamed.hayaitts.ui.components.displayRes
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun CustomImportScreen(
    encodedUri: String,
    onClose: () -> Unit,
    viewModel: CustomImportViewModel = koinViewModel { parametersOf(encodedUri) },
) {
    val phase by viewModel.state.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    // Auto-pop when the import finishes successfully so the Library list shows
    // the new voice without an extra tap.
    LaunchedEffect(phase) {
        if (phase is CustomImportViewModel.Phase.Done) onClose()
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.import_title)) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when (val p = phase) {
                is CustomImportViewModel.Phase.Analyzing -> AnalyzingBody()
                is CustomImportViewModel.Phase.Confirm -> ConfirmBody(
                    confirm = p,
                    onNameChange = viewModel::updateName,
                    onFamilyChange = viewModel::updateFamily,
                    onAddLanguage = viewModel::addLanguage,
                    onRemoveLanguage = viewModel::removeLanguage,
                    onImport = viewModel::startImport,
                    onCancel = onClose,
                )
                is CustomImportViewModel.Phase.Importing -> ImportingBody(progress = p.progress)
                is CustomImportViewModel.Phase.Done -> AnalyzingBody() // brief flash before pop
                is CustomImportViewModel.Phase.Failed -> FailedBody(reason = p.reason, onClose = onClose)
            }
        }
    }
}

@Composable
private fun AnalyzingBody() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ContainedLoadingIndicator()
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.import_analyzing),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ConfirmBody(
    confirm: CustomImportViewModel.Phase.Confirm,
    onNameChange: (String) -> Unit,
    onFamilyChange: (CustomImportViewModel.FamilyChoice) -> Unit,
    onAddLanguage: (String) -> Unit,
    onRemoveLanguage: (String) -> Unit,
    onImport: () -> Unit,
    onCancel: () -> Unit,
) {
    var addLangOpen by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = confirm.analysis.familyReason,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        // Voice name field — pre-filled with the archive base name.
        OutlinedTextField(
            value = confirm.voiceName,
            onValueChange = onNameChange,
            label = { Text(stringResource(R.string.import_voice_name)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        // Family override dropdown.
        FamilyDropdown(
            current = confirm.chosenFamily,
            onChange = onFamilyChange,
        )

        // Languages chip row + add-language affordance.
        Text(
            text = stringResource(R.string.import_languages),
            style = MaterialTheme.typography.labelLarge,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            confirm.languages.forEach { tag ->
                InputChip(
                    selected = true,
                    onClick = {},
                    label = { Text(displayName(tag)) },
                    trailingIcon = {
                        IconButton(onClick = { onRemoveLanguage(tag) }) {
                            Icon(Icons.Outlined.Close, contentDescription = null)
                        }
                    },
                    colors = InputChipDefaults.inputChipColors(),
                )
            }
            AssistChip(
                onClick = { addLangOpen = true },
                label = { Text(stringResource(R.string.import_lang_add)) },
            )
        }

        // Speakers section.
        HorizontalDivider()
        Text(
            text = stringResource(R.string.import_speakers_header, confirm.analysis.speakers.size),
            style = MaterialTheme.typography.titleSmall,
        )
        if (confirm.analysis.speakers.size <= 1) {
            Text(
                text = stringResource(R.string.import_no_speakers),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                confirm.analysis.speakers.forEach { sp ->
                    AssistChip(onClick = {}, label = { Text("${sp.id} · ${sp.name}") })
                }
            }
        }

        // File listing.
        HorizontalDivider()
        Text(
            text = stringResource(R.string.import_files_header, confirm.analysis.entries.size),
            style = MaterialTheme.typography.titleSmall,
        )
        confirm.analysis.entries.take(MAX_PREVIEW_FILES).forEach { entry ->
            FileRow(entry)
        }
        if (confirm.analysis.entries.size > MAX_PREVIEW_FILES) {
            Text(
                text = "… +${confirm.analysis.entries.size - MAX_PREVIEW_FILES} more",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Inline error from a previous Import attempt (validation failure).
        confirm.validationError?.let { msg ->
            Text(
                text = msg,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }

        // Action row.
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TextButton(onClick = onCancel) {
                Icon(Icons.Outlined.Cancel, contentDescription = null)
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.action_cancel))
            }
            FilledTonalButton(
                onClick = onImport,
                enabled = confirm.chosenFamily != null,
            ) {
                Icon(Icons.Outlined.Done, contentDescription = null)
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.import_action_import))
            }
        }
    }

    if (addLangOpen) {
        AddLanguageDialog(
            onConfirm = { tag ->
                addLangOpen = false
                onAddLanguage(tag)
            },
            onDismiss = { addLangOpen = false },
        )
    }
}

@Composable
private fun FileRow(entry: CustomBundleAnalyzer.Entry) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = entry.path,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f),
            overflow = TextOverflow.MiddleEllipsis,
            maxLines = 1,
        )
        Text(
            text = "${entry.sizeBytes / 1024} KB",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun FamilyDropdown(
    current: ModelFamily?,
    onChange: (CustomImportViewModel.FamilyChoice) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val label = current?.let { stringResource(it.displayRes()) }
        ?: stringResource(R.string.family_unknown)

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
    ) {
        OutlinedTextField(
            value = label,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.import_family_override)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(androidx.compose.material3.ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            CustomImportViewModel.FamilyChoice.entries.forEach { choice ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(stringResource(choice.family.displayRes()))
                            if (!choice.supported) {
                                Text(
                                    text = stringResource(
                                        R.string.import_unsupported_family,
                                        stringResource(choice.family.displayRes()),
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    },
                    enabled = choice.supported,
                    onClick = {
                        expanded = false
                        onChange(choice)
                    },
                )
            }
        }
    }
}

@Composable
private fun AddLanguageDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.import_lang_add)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text(stringResource(R.string.import_lang_add_placeholder)) },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text) }) {
                Text(stringResource(R.string.import_lang_add_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

@Composable
private fun ImportingBody(progress: CustomBundleInstaller.Progress) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val label = when (progress.step) {
            CustomBundleInstaller.Step.Copying -> stringResource(R.string.import_progress_copying)
            CustomBundleInstaller.Step.Extracting -> stringResource(R.string.import_progress_extracting)
            CustomBundleInstaller.Step.Validating -> stringResource(R.string.import_progress_validating)
        }
        Text(label, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(16.dp))
        // Phase 7 motion consistency: every active-work indicator uses the
        // wavy variant from M3 Expressive — matches DownloadProgress in Browse.
        LinearWavyProgressIndicator(
            progress = { progress.pct.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun FailedBody(reason: String, onClose: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.import_analyzing_failed, reason),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error,
        )
        Spacer(Modifier.height(16.dp))
        FilledTonalButton(onClick = onClose) {
            Text(stringResource(R.string.action_back))
        }
    }
}

private const val MAX_PREVIEW_FILES = 12
