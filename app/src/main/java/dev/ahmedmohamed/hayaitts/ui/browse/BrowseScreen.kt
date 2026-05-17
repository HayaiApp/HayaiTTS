@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package dev.ahmedmohamed.hayaitts.ui.browse

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.FilterAlt
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumFlexibleTopAppBar
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.MultiChoiceSegmentedButtonRow
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.ahmedmohamed.hayaitts.R
import dev.ahmedmohamed.hayaitts.domain.model.DownloadState
import dev.ahmedmohamed.hayaitts.domain.model.Tier
import dev.ahmedmohamed.hayaitts.ui.components.CatalogVoiceCard
import dev.ahmedmohamed.hayaitts.ui.components.EmptyState
import dev.ahmedmohamed.hayaitts.ui.components.displayName
import dev.ahmedmohamed.hayaitts.ui.components.displayRes
import org.koin.androidx.compose.koinViewModel

/**
 * Catalog browser. Top bar uses [MediumFlexibleTopAppBar] (expressive 2-row
 * variant with subtitle slot). Tier is a [MultiChoiceSegmentedButtonRow] —
 * tier semantics are mutually exclusive but the segmented row reads as the
 * single most expressive picker for a short fixed set. Languages + families
 * remain multi-select bottom sheets.
 *
 * The bottom [HorizontalFloatingToolbar] replaces the screen's previous FAB
 * with Refresh + Quick Switch + Back actions.
 */
@Composable
fun BrowseScreen(
    onBack: () -> Unit,
    onVoiceClick: (voiceId: String) -> Unit,
    onOpenQuickSwitch: () -> Unit,
    viewModel: BrowseViewModel = koinViewModel(),
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val haptics = LocalHapticFeedback.current

    var languageSheetOpen by remember { mutableStateOf(false) }
    var familySheetOpen by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MediumFlexibleTopAppBar(
                title = { Text(stringResource(R.string.browse_title)) },
                subtitle = {
                    Text(
                        stringResource(R.string.browse_subtitle, state.cards.size),
                    )
                },
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
                            Icons.Outlined.SwapHoriz,
                            contentDescription = stringResource(R.string.quick_switch_title),
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
            TierSegmentedRow(
                tier = state.filters.tier,
                onPick = { picked ->
                    viewModel.setTier(picked)
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                },
            )
            FilterChipRow(
                filters = state.filters,
                onOpenLanguages = { languageSheetOpen = true },
                onOpenFamilies = { familySheetOpen = true },
            )
            SearchField(
                query = state.filters.query,
                onChange = viewModel::setQuery,
            )

            AnimatedContent(
                targetState = state.cards.isEmpty(),
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "browse-empty",
            ) { empty ->
                if (empty) {
                    BrowseEmptyState(
                        filters = state.filters,
                        onClearFilters = {
                            viewModel.setTier(null)
                            viewModel.clearLanguages()
                            viewModel.clearFamilies()
                            viewModel.setQuery("")
                        },
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

    BrowseFloatingActions(
        onQuickSwitch = onOpenQuickSwitch,
        onRefresh = { /* catalog refresh is automatic on launch; expose the manual ping. */ },
    )

    if (languageSheetOpen) {
        MultiSelectSheet(
            title = stringResource(R.string.browse_filter_language),
            options = state.availableLanguages,
            selected = state.filters.languages,
            renderLabel = { displayName(it) },
            onToggle = {
                viewModel.toggleLanguage(it)
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            },
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
            onToggle = {
                viewModel.toggleFamily(it)
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            },
            onClear = viewModel::clearFamilies,
            onDismiss = { familySheetOpen = false },
        )
    }
}

@Composable
private fun TierSegmentedRow(
    tier: Tier?,
    onPick: (Tier?) -> Unit,
) {
    val options = listOf(null, Tier.LOW, Tier.MID, Tier.HIGH)
    MultiChoiceSegmentedButtonRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        options.forEachIndexed { index, option ->
            val label = when (option) {
                null -> stringResource(R.string.browse_tier_all)
                Tier.LOW -> stringResource(R.string.browse_tier_low)
                Tier.MID -> stringResource(R.string.browse_tier_mid)
                Tier.HIGH -> stringResource(R.string.browse_tier_high)
            }
            SegmentedButton(
                checked = tier == option,
                onCheckedChange = { checked ->
                    onPick(if (checked || option == null) option else option)
                },
                shape = SegmentedButtonDefaults.itemShape(
                    index = index,
                    count = options.size,
                ),
                label = { Text(label) },
            )
        }
    }
}

@Composable
private fun FilterChipRow(
    filters: BrowseViewModel.Filters,
    onOpenLanguages: () -> Unit,
    onOpenFamilies: () -> Unit,
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item("lang") {
            CountingFilterChip(
                label = stringResource(R.string.browse_filter_language),
                count = filters.languages.size,
                onClick = onOpenLanguages,
            )
        }
        item("family") {
            CountingFilterChip(
                label = stringResource(R.string.browse_filter_family),
                count = filters.families.size,
                onClick = onOpenFamilies,
            )
        }
    }
}

@Composable
private fun CountingFilterChip(
    label: String,
    count: Int,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = count > 0,
        onClick = onClick,
        label = {
            AnimatedContent(
                targetState = count,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "filter-count",
            ) { c ->
                Text(if (c == 0) label else "$label · $c")
            }
        },
        leadingIcon = {
            Icon(Icons.Outlined.FilterAlt, contentDescription = null)
        },
    )
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
            .padding(horizontal = 16.dp, vertical = 4.dp),
        singleLine = true,
        shape = RoundedCornerShape(20.dp),
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
        contentPadding = PaddingValues(top = 12.dp, bottom = 120.dp),
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
private fun BrowseEmptyState(
    filters: BrowseViewModel.Filters,
    onClearFilters: () -> Unit,
) {
    val hasActiveFilters = filters.activeCount > 0
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
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
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

@Composable
private fun BrowseFloatingActions(
    onQuickSwitch: () -> Unit,
    onRefresh: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 24.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        AnimatedVisibility(visible = true, enter = fadeIn(), exit = fadeOut()) {
            HorizontalFloatingToolbar(expanded = true) {
                IconButton(onClick = onRefresh) {
                    Icon(
                        Icons.Outlined.Refresh,
                        contentDescription = stringResource(R.string.browse_refresh),
                    )
                }
                IconButton(onClick = onQuickSwitch) {
                    Icon(
                        Icons.Outlined.SwapHoriz,
                        contentDescription = stringResource(R.string.quick_switch_title),
                    )
                }
            }
        }
    }
}
