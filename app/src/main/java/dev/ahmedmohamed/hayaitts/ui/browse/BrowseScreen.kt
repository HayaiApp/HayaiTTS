@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package dev.ahmedmohamed.hayaitts.ui.browse

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.FilterAlt
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.ahmedmohamed.hayaitts.R
import dev.ahmedmohamed.hayaitts.domain.model.DownloadState
import dev.ahmedmohamed.hayaitts.domain.model.ModelFamily
import dev.ahmedmohamed.hayaitts.domain.model.Tier
import dev.ahmedmohamed.hayaitts.ui.components.CatalogVoiceCard
import dev.ahmedmohamed.hayaitts.ui.components.EmptyState
import dev.ahmedmohamed.hayaitts.ui.components.displayName
import dev.ahmedmohamed.hayaitts.ui.components.displayRes
import org.koin.androidx.compose.koinViewModel

/**
 * Catalog browser. Filter chips for language / tier / family sit directly
 * under the app bar, an outlined search field below them, and the filtered
 * catalog list fills the rest.
 *
 * The "Family" chip is multi-select today even though the bundled catalog only
 * contains [ModelFamily.PIPER] entries — Phase 5 will populate the other
 * families and we want the UI to handle them without a rewrite.
 */
@Composable
fun BrowseScreen(
    onBack: () -> Unit,
    onVoiceClick: (voiceId: String) -> Unit,
    viewModel: BrowseViewModel = koinViewModel(),
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var languageSheetOpen by remember { mutableStateOf(false) }
    var familySheetOpen by remember { mutableStateOf(false) }
    var tierMenuOpen by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.browse_title)) },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            FilterRow(
                filters = state.filters,
                onOpenLanguages = { languageSheetOpen = true },
                onOpenFamilies = { familySheetOpen = true },
                onOpenTier = { tierMenuOpen = true },
                tierMenuOpen = tierMenuOpen,
                onCloseTierMenu = { tierMenuOpen = false },
                onPickTier = {
                    viewModel.setTier(it)
                    tierMenuOpen = false
                },
            )
            SearchField(
                query = state.filters.query,
                onChange = viewModel::setQuery,
            )
            if (state.cards.isEmpty()) {
                EmptyState(
                    icon = Icons.Outlined.LibraryMusic,
                    title = stringResource(R.string.browse_empty_title),
                    subtitle = stringResource(R.string.browse_empty_subtitle),
                )
            } else {
                CatalogList(
                    cards = state.cards,
                    downloads = state.downloads,
                    installedIds = state.installedIds,
                    recommendedTier = state.recommendedTier,
                    onClickCard = onVoiceClick,
                    onInstall = viewModel::enqueue,
                    onCancel = viewModel::cancel,
                )
            }
        }
    }

    if (languageSheetOpen) {
        MultiSelectSheet(
            title = stringResource(R.string.browse_filter_language),
            options = state.availableLanguages,
            selected = state.filters.languages,
            renderLabel = { displayName(it) },
            onToggle = viewModel::toggleLanguage,
            onClear = viewModel::clearLanguages,
            onDismiss = { languageSheetOpen = false },
        )
    }
    if (familySheetOpen) {
        MultiSelectSheet(
            title = stringResource(R.string.browse_filter_family),
            options = state.availableFamilies,
            selected = state.filters.families,
            renderLabel = { stringResource(it.displayRes()) },
            onToggle = viewModel::toggleFamily,
            onClear = viewModel::clearFamilies,
            onDismiss = { familySheetOpen = false },
        )
    }
}

@Composable
private fun FilterRow(
    filters: BrowseViewModel.Filters,
    onOpenLanguages: () -> Unit,
    onOpenFamilies: () -> Unit,
    onOpenTier: () -> Unit,
    tierMenuOpen: Boolean,
    onCloseTierMenu: () -> Unit,
    onPickTier: (Tier?) -> Unit,
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item("lang") {
            val count = filters.languages.size
            val label = if (count == 0) {
                stringResource(R.string.browse_filter_language)
            } else "${stringResource(R.string.browse_filter_language)} · $count"
            FilterChip(
                selected = count > 0,
                onClick = onOpenLanguages,
                label = { Text(label) },
                leadingIcon = filterIcon(),
            )
        }
        item("tier") {
            val label = when (filters.tier) {
                Tier.LOW -> stringResource(R.string.browse_tier_low)
                Tier.MID -> stringResource(R.string.browse_tier_mid)
                Tier.HIGH -> stringResource(R.string.browse_tier_high)
                null -> stringResource(R.string.browse_filter_tier)
            }
            Column {
                FilterChip(
                    selected = filters.tier != null,
                    onClick = onOpenTier,
                    label = { Text(label) },
                    leadingIcon = filterIcon(),
                )
                DropdownMenu(
                    expanded = tierMenuOpen,
                    onDismissRequest = onCloseTierMenu,
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.browse_tier_all)) },
                        onClick = { onPickTier(null) },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.browse_tier_low)) },
                        onClick = { onPickTier(Tier.LOW) },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.browse_tier_mid)) },
                        onClick = { onPickTier(Tier.MID) },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.browse_tier_high)) },
                        onClick = { onPickTier(Tier.HIGH) },
                    )
                }
            }
        }
        item("family") {
            val count = filters.families.size
            val label = if (count == 0) {
                stringResource(R.string.browse_filter_family)
            } else "${stringResource(R.string.browse_filter_family)} · $count"
            FilterChip(
                selected = count > 0,
                onClick = onOpenFamilies,
                label = { Text(label) },
                leadingIcon = filterIcon(),
            )
        }
    }
}

@Composable
private fun filterIcon(): @Composable () -> Unit = {
    Icon(Icons.Outlined.FilterAlt, contentDescription = null, modifier = Modifier)
}

@Composable
private fun SearchField(
    query: String,
    onChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        singleLine = true,
        placeholder = { Text(stringResource(R.string.browse_search_placeholder)) },
        leadingIcon = {
            Icon(Icons.Outlined.Search, contentDescription = null)
        },
    )
}

@Composable
private fun CatalogList(
    cards: List<dev.ahmedmohamed.hayaitts.domain.model.VoiceCard>,
    downloads: Map<String, DownloadState>,
    installedIds: Set<String>,
    recommendedTier: Tier,
    onClickCard: (String) -> Unit,
    onInstall: (dev.ahmedmohamed.hayaitts.domain.model.VoiceCard) -> Unit,
    onCancel: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(items = cards, key = { it.id }) { card ->
            val state = downloads[card.id] ?: DownloadState.Idle
            CatalogVoiceCard(
                card = card,
                downloadState = state,
                isInstalled = card.id in installedIds,
                onOpen = { onClickCard(card.id) },
                onInstall = { onInstall(card) },
                onCancel = { onCancel(card.id) },
                recommendedTier = recommendedTier,
            )
        }
    }
}

@Composable
private fun <T> MultiSelectSheet(
    title: String,
    options: List<T>,
    selected: Set<T>,
    renderLabel: @Composable (T) -> String,
    onToggle: (T) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 8.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )
            options.forEach { option ->
                val isSelected = option in selected
                ListItem(
                    headlineContent = { Text(renderLabel(option)) },
                    trailingContent = {
                        FilterChip(
                            selected = isSelected,
                            onClick = { onToggle(option) },
                            label = { Text(if (isSelected) "✓" else "+") },
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggle(option) },
                )
            }
            TextButton(
                onClick = onClear,
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(8.dp),
            ) {
                Text(stringResource(R.string.browse_filter_clear))
            }
        }
    }
}
