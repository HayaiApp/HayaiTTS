@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package dev.ahmedmohamed.hayaitts.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Search query + change handler + placeholder, packaged so screens can opt
 * into the inline search input by passing a single argument to [HayaiTopBar].
 */
data class HayaiSearchable(
    val query: String,
    val onQueryChange: (String) -> Unit,
    val placeholder: String,
)

/**
 * The one and only top app bar used throughout HayaiTTS.
 *
 * Design:
 *  - A pinned 56dp pill (rounded `RoundedCornerShape(28.dp)`) sitting on a
 *    background-colored strip below the status bar. The pill never collapses
 *    or fades on scroll, so the title / search input is always reachable and
 *    we never end up with a bare empty strip after a partial scroll-collapse.
 *  - Three slots inside the pill: leading icon | content | trailing actions.
 *  - Content is either a [BasicTextField] (when [searchable] is non-null,
 *    making the pill an in-place search field that propagates through
 *    [HayaiSearchable.onQueryChange]) or a stack of title + optional subtitle.
 *  - When a screen opts into search, the leading slot defaults to a magnifier
 *    icon (passing a custom [navigationIcon] e.g. for a back arrow overrides
 *    this). When [searchable] is null and no nav icon is supplied the leading
 *    slot is empty so the title sits flush.
 *
 * The [scrollBehavior] parameter is accepted purely for source compatibility
 * with the previous implementation; this bar does not respond to scroll.
 */
@Composable
fun HayaiTopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    searchable: HayaiSearchable? = null,
    @Suppress("UNUSED_PARAMETER")
    scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    // Outer container is transparent so the pill literally floats above
    // whatever content is scrolling underneath. Only the pill itself draws
    // a filled surface. No bottom padding here — the pill's bottom edge is
    // the bar's bottom edge, so scrolling content disappears exactly at the
    // pill (instead of through a few-dp transparent gap below it).
    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(start = 8.dp, end = 8.dp, top = 6.dp),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LeadingSlot(navigationIcon = navigationIcon, searchable = searchable)
                ContentSlot(
                    title = title,
                    subtitle = subtitle,
                    searchable = searchable,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp),
                )
                TrailingSlot(searchable = searchable, actions = actions)
            }
        }
    }
}

@Composable
private fun LeadingSlot(
    navigationIcon: (@Composable () -> Unit)?,
    searchable: HayaiSearchable?,
) {
    Box(
        modifier = Modifier.size(48.dp),
        contentAlignment = Alignment.Center,
    ) {
        when {
            navigationIcon != null -> navigationIcon()
            searchable != null -> Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ContentSlot(
    title: String,
    subtitle: String?,
    searchable: HayaiSearchable?,
    modifier: Modifier,
) {
    Box(modifier = modifier) {
        if (searchable != null) {
            BasicTextField(
                value = searchable.query,
                onValueChange = searchable.onQueryChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = LocalContentColor.current,
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
                decorationBox = { inner ->
                    if (searchable.query.isEmpty()) {
                        Text(
                            text = searchable.placeholder,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                    inner()
                },
            )
        } else {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun TrailingSlot(
    searchable: HayaiSearchable?,
    actions: @Composable RowScope.() -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End,
    ) {
        if (searchable != null && searchable.query.isNotEmpty()) {
            IconButton(onClick = { searchable.onQueryChange("") }) {
                Icon(Icons.Outlined.Close, contentDescription = null)
            }
        }
        actions()
    }
}

/**
 * Height of [HayaiTopBar] including status-bar inset. Use this as the `top`
 * value of a scrollable container's contentPadding so the first item lines up
 * exactly with the pill's bottom edge — content then scrolls up *behind* the
 * pill. Status bar inset (variable) + 6dp top + 56dp pill = `statusBars + 62.dp`
 * (the bar has no transparent gap below the pill, so there's no band where
 * content would briefly appear before being occluded).
 */
@Composable
fun hayaiTopBarHeight(): Dp =
    WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 62.dp

/**
 * Scaffold replacement that draws [HayaiTopBar] as a floating overlay above
 * scrollable content. The content fills the entire screen (extending up
 * behind the bar); the bar is painted last on top. Pass
 * `topInset = hayaiTopBarHeight()` (which is what [content] receives) into a
 * `LazyColumn.contentPadding` so visible content starts just below the bar.
 *
 * Also fixes the double-counted bottom inset that surfaced when each screen's
 * inner Scaffold redundantly subtracted system navigation-bar height that the
 * outer NavHost Scaffold had already consumed. `contentWindowInsets =
 * WindowInsets(0)` short-circuits the inner pass; the bar handles its own
 * status-bar inset, the outer NavHost handles the bottom.
 */
@Composable
fun HayaiScreenChrome(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    searchable: HayaiSearchable? = null,
    snackbarHostState: SnackbarHostState? = null,
    barOverlay: @Composable (() -> Unit)? = null,
    content: @Composable (topInset: Dp) -> Unit,
) {
    Scaffold(
        modifier = modifier,
        snackbarHost = {
            if (snackbarHostState != null) SnackbarHost(snackbarHostState)
        },
        contentWindowInsets = WindowInsets(0),
        // Scaffold paints `colorScheme.background` across the whole content
        // area by default — that's the "background under the pill spanning
        // the top app bar area" the user was seeing. Pass Transparent so
        // there is no scaffold-level fill above content; the page background
        // comes from the activity window, and the pill is the only opaque
        // element painted across the top edge.
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
    ) { scaffoldPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(scaffoldPadding),
        ) {
            val topInset = hayaiTopBarHeight()
            content(topInset)
            Column(modifier = Modifier.align(Alignment.TopCenter)) {
                HayaiTopBar(
                    title = title,
                    subtitle = subtitle,
                    navigationIcon = navigationIcon,
                    actions = actions,
                    searchable = searchable,
                )
                if (barOverlay != null) barOverlay()
            }
        }
    }
}
