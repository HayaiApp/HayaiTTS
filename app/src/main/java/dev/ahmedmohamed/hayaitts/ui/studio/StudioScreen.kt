@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package dev.ahmedmohamed.hayaitts.ui.studio

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MediumFlexibleTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.ahmedmohamed.hayaitts.R
import dev.ahmedmohamed.hayaitts.ui.components.EmptyState
import dev.ahmedmohamed.hayaitts.ui.library.LibraryViewModel
import dev.ahmedmohamed.hayaitts.ui.playground.PlaygroundScreen
import org.koin.androidx.compose.koinViewModel

/**
 * v2 Studio tab. Picks the user's first installed voice as the default
 * subject for the Playground knobs. When no voices are installed it surfaces
 * an empty state pointing at Browse.
 *
 * v2.0.0-b1 reuses the existing [PlaygroundScreen] body so the slider
 * surface and history flow keep working while later commits inside the
 * branch swap in profiles + A/B compare + SSML highlighting + WAV export
 * per the architecture plan.
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
            MediumFlexibleTopAppBar(
                title = { Text(stringResource(R.string.studio_title)) },
                subtitle = { Text(stringResource(R.string.studio_subtitle)) },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            EmptyState(
                icon = Icons.Outlined.Tune,
                title = stringResource(R.string.studio_title),
                subtitle = stringResource(R.string.studio_no_voices),
                ctaLabel = stringResource(R.string.nav_browse),
                onCta = onBrowse,
            )
        }
    }
}
