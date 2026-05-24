@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package dev.ahmedmohamed.hayaitts.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumFlexibleTopAppBar
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * The one and only top app bar used throughout HayaiTTS.
 *
 * Wraps [MediumFlexibleTopAppBar] (the M3 Expressive flexible variant) with:
 *  - A fixed collapsed height that matches the legacy small `TopAppBar`
 *    (56dp), so collapsed state looks like the bar the user already knows.
 *  - A compact expanded height (96dp) — large enough for the title/subtitle
 *    stack to breathe, small enough that scrolling collapses it within a
 *    single gesture.
 *  - Optional `subtitle` slot that's commonly used to surface counts /
 *    context (e.g., "188 voices" on Browse).
 *
 * Plain `TopAppBar` is forbidden across the app (enforced by Konsist). This
 * keeps every screen's bar visually consistent and ensures scroll-collapse
 * actually works — `TopAppBar` + `exitUntilCollapsedScrollBehavior` is a
 * no-op because the small bar has no expanded state.
 */
@Composable
fun HayaiTopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    MediumFlexibleTopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
            )
        },
        subtitle = {
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        },
        modifier = modifier,
        navigationIcon = navigationIcon,
        actions = actions,
        collapsedHeight = 56.dp,
        expandedHeight = 96.dp,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            titleContentColor = MaterialTheme.colorScheme.onBackground,
            navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
            actionIconContentColor = MaterialTheme.colorScheme.onBackground,
        ),
        scrollBehavior = scrollBehavior,
    )
}
