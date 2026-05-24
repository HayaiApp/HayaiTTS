@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package dev.ahmedmohamed.hayaitts.ui.studio

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.ahmedmohamed.hayaitts.R
import dev.ahmedmohamed.hayaitts.ui.components.HayaiEmpty
import dev.ahmedmohamed.hayaitts.ui.components.HayaiEmptyMode
import dev.ahmedmohamed.hayaitts.ui.components.HayaiTopBar
import dev.ahmedmohamed.hayaitts.ui.library.LibraryViewModel
import dev.ahmedmohamed.hayaitts.ui.playground.PlaygroundScreen
import org.koin.androidx.compose.koinViewModel

/**
 * v2 Studio tab. Picks the user's first installed voice as the default
 * subject for the Playground knobs. When no voices are installed it surfaces
 * an empty state pointing at Browse.
 */
@Composable
fun StudioScreen(
    onBack: () -> Unit,
    onOpenQuickSwitch: () -> Unit,
) {
    val libraryVm: LibraryViewModel = koinViewModel()
    val state by libraryVm.uiState.collectAsStateWithLifecycle()
    val firstVoiceId = state.orderedInstalled.firstOrNull()?.voiceId
        ?: state.installed.firstOrNull()?.voiceId

    if (firstVoiceId == null) {
        StudioEmpty(onBrowse = onBack)
        return
    }
    PlaygroundScreen(
        voiceId = firstVoiceId,
        onBack = onBack,
    )
}

@Composable
private fun StudioEmpty(onBrowse: () -> Unit) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            HayaiTopBar(
                title = stringResource(R.string.studio_title),
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        HayaiEmpty(
            mode = HayaiEmptyMode.Empty(
                icon = Icons.Outlined.Tune,
                title = stringResource(R.string.studio_title),
                subtitle = stringResource(R.string.studio_no_voices),
                cta = stringResource(R.string.nav_browse) to onBrowse,
            ),
            modifier = Modifier.fillMaxSize().padding(padding),
        )
    }
}
