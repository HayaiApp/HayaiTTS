package dev.ahmedmohamed.hayaitts.ui.nav

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import dev.ahmedmohamed.hayaitts.ui.browse.BrowseScreen
import dev.ahmedmohamed.hayaitts.ui.detail.VoiceDetailScreen
import dev.ahmedmohamed.hayaitts.ui.library.LibraryScreen

/**
 * Top-level navigation graph. Three destinations:
 *
 *  - [Routes.LIBRARY]        : start destination; the installed-voices home.
 *  - [Routes.BROWSE]         : catalog browser with filters + search.
 *  - [Routes.VOICE_DETAIL]   : per-voice detail with preview + install/uninstall.
 *
 * Routes are hand-coded strings (no type-safe nav-3 wrappers) because the
 * graph is tiny and a build-time generator would add KSP cost for marginal
 * gain. The single navigation arg, voiceId, is passed via [NavType.StringType].
 */
@Composable
fun HayaiTtsNavHost(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Routes.LIBRARY) {
        composable(Routes.LIBRARY) {
            LibraryScreen(
                onBrowse = { navController.navigate(Routes.BROWSE) },
                onVoiceClick = { id -> navController.navigate(Routes.voiceDetail(id)) },
            )
        }
        composable(Routes.BROWSE) {
            BrowseScreen(
                onBack = { navController.popBackStack() },
                onVoiceClick = { id -> navController.navigate(Routes.voiceDetail(id)) },
            )
        }
        composable(
            route = Routes.VOICE_DETAIL,
            arguments = listOf(navArgument(Routes.ARG_VOICE_ID) { type = NavType.StringType }),
        ) { entry ->
            val id = entry.arguments?.getString(Routes.ARG_VOICE_ID).orEmpty()
            VoiceDetailScreen(
                voiceId = id,
                onBack = { navController.popBackStack() },
            )
        }
    }
}

object Routes {
    const val LIBRARY = "library"
    const val BROWSE = "browse"
    const val ARG_VOICE_ID = "voiceId"
    const val VOICE_DETAIL = "voiceDetail/{$ARG_VOICE_ID}"

    fun voiceDetail(voiceId: String): String = "voiceDetail/$voiceId"
}
