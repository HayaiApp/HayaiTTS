@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package dev.ahmedmohamed.hayaitts.ui.browse

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.DockedSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.MultiChoiceSegmentedButtonRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.ahmedmohamed.hayaitts.R
import dev.ahmedmohamed.hayaitts.domain.model.DownloadState
import dev.ahmedmohamed.hayaitts.domain.model.ModelFamily
import dev.ahmedmohamed.hayaitts.domain.model.Tier
import dev.ahmedmohamed.hayaitts.ui.components.CatalogVoiceCard
import dev.ahmedmohamed.hayaitts.ui.components.EmptyState
import dev.ahmedmohamed.hayaitts.ui.components.HayaiRichTooltipBox
import dev.ahmedmohamed.hayaitts.ui.components.displayName
import dev.ahmedmohamed.hayaitts.ui.components.displayRes
import org.koin.androidx.compose.koinViewModel

/**
 * Catalog browser. UX:
 *   - Floating [DockedSearchBar] at the top — leading back, trailing
 *     [FilterList] icon with a count badge.
 *   - One bottom-sheet filter UI with three grouped sections (Tier /
 *     Language / Family).
 *   - Active filter chips render below the search bar (only when there
 *     are active filters), each with an inline X to remove it.
 *   - The catalog list is the only scrolling surface.
 *
 * All colors come from `MaterialTheme.colorScheme`; no per-family palettes
 * leak in.
 */
@Composable
fun BrowseScreen(
    onBack: () -> Unit,
    onVoiceClick: (voiceId: String) -> Unit,
    onOpenQuickSwitch: () -> Unit,
    viewModel: BrowseViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val haptics = LocalHapticFeedback.current
    var filterSheetOpen by remember { mutableStateOf(false) }
    var searchExpanded by remember { mutableStateOf(false) }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            SearchHeader(
                query = state.filters.query,
                onQueryChange = viewModel::setQuery,
                activeFilterCount = state.filters.activeCount.let { c ->
                    // The query already shows in the field; count only the
                    // sheet-managed filters in the badge.
                    val q = if (state.filters.query.isNotBlank()) 1 else 0
                    c - q
                },
                expanded = searchExpanded,
                onExpandedChange = { searchExpanded = it },
                onBack = onBack,
                onOpenFilters = { filterSheetOpen = true },
                onOpenQuickSwitch = onOpenQuickSwitch,
                resultCount = state.cards.size,
                content = {
                    // Expanded suggestions: the same list. Render in a
                    // simpler scroll so the SearchBar's transient state
                    // doesn't fight with the LazyColumn's anchor.
                    if (state.cards.isEmpty()) {
                        BrowseEmptyState(
                            hasActiveFilters = state.filters.activeCount > 0,
                            onClearFilters = viewModel::clearAllFilters,
                        )
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            state.cards.forEach { card ->
                                CatalogVoiceCard(
                                    card = card,
                                    downloadState = state.downloads[card.id] ?: DownloadState.Idle,
                                    isInstalled = card.id in state.installedIds,
                                    onOpen = {
                                        searchExpanded = false
                                        onVoiceClick(card.id)
                                    },
                                    onInstall = { viewModel.enqueue(card) },
                                    onCancel = { viewModel.cancel(card.id) },
                                    recommendedTier = state.recommendedTier,
                                )
                            }
                        }
                    }
                },
            )

            ActiveFilterRow(
                filters = state.filters,
                onClearTier = { viewModel.setTier(null) },
                onRemoveLanguage = { viewModel.toggleLanguage(it) },
                onRemoveFamily = { viewModel.toggleFamily(it) },
                onClearAll = viewModel::clearAllFilters,
            )

            AnimatedContent(
                targetState = state.cards.isEmpty(),
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "browse-empty",
            ) { empty ->
                if (empty) {
                    BrowseEmptyState(
                        hasActiveFilters = state.filters.activeCount > 0,
                        onClearFilters = viewModel::clearAllFilters,
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
    }

    if (filterSheetOpen) {
        FilterSheet(
            filters = state.filters,
            availableLanguages = state.availableLanguages,
            availableFamilies = state.availableFamilies,
            resultCount = state.cards.size,
            onToggleLanguage = {
                viewModel.toggleLanguage(it)
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            },
            onSetTier = {
                viewModel.setTier(it)
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            },
            onToggleFamily = {
                viewModel.toggleFamily(it)
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            },
            onReset = viewModel::clearAllFilters,
            onDismiss = { filterSheetOpen = false },
        )
    }
}

@Composable
private fun SearchHeader(
    query: String,
    onQueryChange: (String) -> Unit,
    activeFilterCount: Int,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onBack: () -> Unit,
    onOpenFilters: () -> Unit,
    onOpenQuickSwitch: () -> Unit,
    resultCount: Int,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    DockedSearchBar(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        inputField = {
            SearchBarDefaults.InputField(
                query = query,
                onQueryChange = onQueryChange,
                onSearch = { onExpandedChange(false) },
                expanded = expanded,
                onExpandedChange = onExpandedChange,
                placeholder = {
                    Text(stringResource(R.string.browse_search_placeholder_count, resultCount))
                },
                leadingIcon = {
                    if (expanded) {
                        IconButton(onClick = { onExpandedChange(false) }) {
                            Icon(
                                Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = stringResource(R.string.action_back),
                            )
                        }
                    } else {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = stringResource(R.string.action_back),
                            )
                        }
                    }
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(
                                Icons.Outlined.Close,
                                contentDescription = stringResource(R.string.action_clear),
                            )
                        }
                    } else {
                        HayaiRichTooltipBox(
                            title = stringResource(R.string.tooltip_filter_title),
                            description = stringResource(R.string.tooltip_filter_body),
                        ) {
                            BadgedBox(
                                badge = {
                                    if (activeFilterCount > 0) {
                                        Badge { Text("$activeFilterCount") }
                                    }
                                },
                            ) {
                                IconButton(onClick = onOpenFilters) {
                                    Icon(
                                        Icons.Outlined.FilterList,
                                        contentDescription = stringResource(R.string.browse_open_filters),
                                    )
                                }
                            }
                        }
                    }
                },
            )
        },
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        content = content,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ActiveFilterRow(
    filters: BrowseViewModel.Filters,
    onClearTier: () -> Unit,
    onRemoveLanguage: (String) -> Unit,
    onRemoveFamily: (ModelFamily) -> Unit,
    onClearAll: () -> Unit,
) {
    val hasAny = filters.tier != null ||
        filters.languages.isNotEmpty() ||
        filters.families.isNotEmpty()
    if (!hasAny) return
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        val tier = filters.tier
        if (tier != null) {
            ActiveChip(label = tierLabel(tier), onRemove = onClearTier)
        }
        filters.languages.forEach { lang ->
            ActiveChip(label = displayName(lang), onRemove = { onRemoveLanguage(lang) })
        }
        filters.families.forEach { fam ->
            ActiveChip(label = stringResource(fam.displayRes()), onRemove = { onRemoveFamily(fam) })
        }
        TextButton(onClick = onClearAll) {
            Text(stringResource(R.string.browse_filter_clear_all))
        }
    }
}

@Composable
private fun ActiveChip(label: String, onRemove: () -> Unit) {
    FilterChip(
        selected = true,
        onClick = onRemove,
        label = { Text(label) },
        trailingIcon = {
            Icon(
                Icons.Outlined.Close,
                contentDescription = stringResource(R.string.action_clear),
                modifier = Modifier.size(16.dp),
            )
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
        contentPadding = PaddingValues(top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(items = cards, key = { it.id }) { card ->
            val s = downloads[card.id] ?: DownloadState.Idle
            CatalogVoiceCard(
                card = card,
                downloadState = s,
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
private fun BrowseEmptyState(
    hasActiveFilters: Boolean,
    onClearFilters: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        EmptyState(
            icon = Icons.Outlined.LibraryMusic,
            title = stringResource(R.string.browse_empty_title),
            subtitle = stringResource(R.string.browse_empty_subtitle),
            ctaLabel = if (hasActiveFilters) {
                stringResource(R.string.browse_filter_clear_all)
            } else null,
            onCta = if (hasActiveFilters) onClearFilters else null,
            showLoadingPulse = false,
        )
    }
}

/**
 * Single grouped bottom sheet for all filters. Tier is a segmented row
 * (mutually exclusive). Language + Family are multi-select FilterChip
 * flows. "Reset" clears every group; "Show N results" dismisses the sheet.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterSheet(
    filters: BrowseViewModel.Filters,
    availableLanguages: List<String>,
    availableFamilies: List<ModelFamily>,
    resultCount: Int,
    onToggleLanguage: (String) -> Unit,
    onSetTier: (Tier?) -> Unit,
    onToggleFamily: (ModelFamily) -> Unit,
    onReset: () -> Unit,
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
                .heightIn(max = 720.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text(
                text = stringResource(R.string.browse_filter_sheet_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 4.dp),
            )

            FilterGroup(stringResource(R.string.browse_filter_tier)) {
                TierSegmented(tier = filters.tier, onPick = onSetTier)
            }

            HorizontalDivider()

            FilterGroup(stringResource(R.string.browse_filter_language)) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    availableLanguages.forEach { lang ->
                        FilterChip(
                            selected = lang in filters.languages,
                            onClick = { onToggleLanguage(lang) },
                            label = { Text(displayName(lang)) },
                            leadingIcon = if (lang in filters.languages) {
                                {
                                    Icon(
                                        Icons.Outlined.Close,
                                        contentDescription = null,
                                        modifier = Modifier.size(FilterChipDefaults.IconSize),
                                    )
                                }
                            } else null,
                        )
                    }
                }
            }

            HorizontalDivider()

            FilterGroup(stringResource(R.string.browse_filter_family)) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    availableFamilies.forEach { fam ->
                        FilterChip(
                            selected = fam in filters.families,
                            onClick = { onToggleFamily(fam) },
                            label = { Text(stringResource(fam.displayRes())) },
                        )
                    }
                }
            }

            // Bottom actions — pinned to the sheet's bottom by scroll.
            Box(modifier = Modifier.padding(top = 8.dp)) {
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onReset, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.browse_filter_reset))
                    }
                    Button(onClick = onDismiss, modifier = Modifier.weight(2f)) {
                        Text(stringResource(R.string.browse_filter_show_results, resultCount))
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterGroup(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        content()
    }
}

@Composable
private fun TierSegmented(tier: Tier?, onPick: (Tier?) -> Unit) {
    val options = listOf(null, Tier.LOW, Tier.MID, Tier.HIGH)
    MultiChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, option ->
            SegmentedButton(
                checked = tier == option,
                onCheckedChange = { onPick(option) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                label = { Text(tierLabel(option)) },
            )
        }
    }
}

@Composable
private fun tierLabel(tier: Tier?): String = when (tier) {
    null -> stringResource(R.string.browse_tier_all)
    Tier.LOW -> stringResource(R.string.browse_tier_low)
    Tier.MID -> stringResource(R.string.browse_tier_mid)
    Tier.HIGH -> stringResource(R.string.browse_tier_high)
}
