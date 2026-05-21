@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package dev.ahmedmohamed.hayaitts.ui.browse

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Female
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardOptions
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
 *   - Inline search field at the top with leading Back, trailing Filter icon
 *     (badged with active-filter count). The field never enters an "expanded"
 *     state — typing filters the list directly under it.
 *   - One bottom-sheet filter UI with four grouped sections (Tier / Language /
 *     Family / Gender).
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

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            SearchHeader(
                query = state.filters.query,
                onQueryChange = viewModel::setQuery,
                activeSheetFilterCount = run {
                    var n = 0
                    if (state.filters.languages.isNotEmpty()) n++
                    if (state.filters.tier != null) n++
                    if (state.filters.families.isNotEmpty()) n++
                    if (state.filters.genders.isNotEmpty()) n++
                    n
                },
                onBack = onBack,
                onOpenFilters = { filterSheetOpen = true },
                resultCount = state.cards.size,
            )

            ActiveFilterRow(
                filters = state.filters,
                onClearTier = { viewModel.setTier(null) },
                onRemoveLanguage = { viewModel.toggleLanguage(it) },
                onRemoveFamily = { viewModel.toggleFamily(it) },
                onRemoveGender = { viewModel.toggleGender(it) },
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
            availableGenders = state.availableGenders,
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
            onToggleGender = {
                viewModel.toggleGender(it)
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            },
            onReset = viewModel::clearAllFilters,
            onDismiss = { filterSheetOpen = false },
        )
    }
}

/**
 * Plain inline search field that lives in the header row alongside Back and
 * Filter icons. No "expand to fullscreen suggestions" mode — the user
 * complaint was specifically that the previous SearchBar rendered duplicate
 * cards inside its expanded slot. Typing here just updates `query` on the
 * VM, which re-filters the LazyColumn below.
 */
@Composable
private fun SearchHeader(
    query: String,
    onQueryChange: (String) -> Unit,
    activeSheetFilterCount: Int,
    onBack: () -> Unit,
    onOpenFilters: () -> Unit,
    resultCount: Int,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = stringResource(R.string.action_back),
            )
        }
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 4.dp),
            singleLine = true,
            shape = CircleShape,
            placeholder = {
                Text(stringResource(R.string.browse_search_placeholder_count, resultCount))
            },
            leadingIcon = {
                Icon(Icons.Outlined.Search, contentDescription = null)
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(
                            Icons.Outlined.Close,
                            contentDescription = stringResource(R.string.action_clear),
                        )
                    }
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.outline,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            ),
        )
        HayaiRichTooltipBox(
            title = stringResource(R.string.tooltip_filter_title),
            description = stringResource(R.string.tooltip_filter_body),
        ) {
            BadgedBox(
                badge = {
                    if (activeSheetFilterCount > 0) {
                        Badge { Text("$activeSheetFilterCount") }
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
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ActiveFilterRow(
    filters: BrowseViewModel.Filters,
    onClearTier: () -> Unit,
    onRemoveLanguage: (String) -> Unit,
    onRemoveFamily: (ModelFamily) -> Unit,
    onRemoveGender: (String) -> Unit,
    onClearAll: () -> Unit,
) {
    val hasAny = filters.tier != null ||
        filters.languages.isNotEmpty() ||
        filters.families.isNotEmpty() ||
        filters.genders.isNotEmpty()
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
        filters.genders.forEach { gender ->
            ActiveChip(label = genderLabel(gender), onRemove = { onRemoveGender(gender) })
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
 * (mutually exclusive). Language, Family, and Gender are multi-select
 * FilterChip flows. "Reset" clears every group; "Show N results" dismisses
 * the sheet.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterSheet(
    filters: BrowseViewModel.Filters,
    availableLanguages: List<String>,
    availableFamilies: List<ModelFamily>,
    availableGenders: List<String>,
    resultCount: Int,
    onToggleLanguage: (String) -> Unit,
    onSetTier: (Tier?) -> Unit,
    onToggleFamily: (ModelFamily) -> Unit,
    onToggleGender: (String) -> Unit,
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

            FilterGroup(stringResource(R.string.browse_filter_gender)) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    availableGenders.forEach { gender ->
                        val selected = gender in filters.genders
                        FilterChip(
                            selected = selected,
                            onClick = { onToggleGender(gender) },
                            label = { Text(genderLabel(gender)) },
                            leadingIcon = {
                                Icon(
                                    imageVector = genderIcon(gender),
                                    contentDescription = null,
                                    modifier = Modifier.size(FilterChipDefaults.IconSize),
                                )
                            },
                        )
                    }
                }
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

            Box(modifier = Modifier.padding(top = 8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(
                        onClick = onReset,
                        modifier = Modifier.weight(1f),
                        colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error,
                        ),
                    ) {
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
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
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

@Composable
private fun genderLabel(gender: String): String = when (gender.lowercase()) {
    "f", "female" -> stringResource(R.string.browse_filter_gender_female)
    "m", "male" -> stringResource(R.string.browse_filter_gender_male)
    else -> stringResource(R.string.browse_filter_gender_unknown)
}

private fun genderIcon(gender: String): ImageVector = when (gender.lowercase()) {
    "f", "female" -> Icons.Outlined.Female
    "m", "male" -> Icons.Outlined.Person
    else -> Icons.Outlined.RecordVoiceOver
}
