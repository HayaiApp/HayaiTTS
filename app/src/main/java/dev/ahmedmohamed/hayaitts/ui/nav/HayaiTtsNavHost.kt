package dev.ahmedmohamed.hayaitts.ui.nav

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import dev.ahmedmohamed.hayaitts.ui.browse.BrowseScreen
import dev.ahmedmohamed.hayaitts.ui.custom.CustomImportScreen
import dev.ahmedmohamed.hayaitts.ui.detail.VoiceDetailScreen
import dev.ahmedmohamed.hayaitts.ui.library.LibraryScreen
import java.net.URLEncoder

/**
 * Top-level navigation graph. Destinations:
 *
 *  - [Routes.LIBRARY]        : start destination; the installed-voices home.
 *  - [Routes.BROWSE]         : catalog browser with filters + search.
 *  - [Routes.VOICE_DETAIL]   : per-voice detail with preview + install/uninstall.
 *  - [Routes.CUSTOM_IMPORT]  : Phase 6 custom voice import wizard.
 *
 * The voice quick-switcher bottom sheet is owned by [dev.ahmedmohamed.hayaitts.ui.MainActivity];
 * every leaf screen receives [onOpenQuickSwitch] so its top bar can pop the
 * sheet from a global icon. Keeping the state outside the nav graph means the
 * sheet does not flicker when the user navigates between routes.
 */
@Composable
fun HayaiTtsNavHost(
    navController: NavHostController,
    onOpenQuickSwitch: () -> Unit,
) {
    NavHost(navController = navController, startDestination = Routes.LIBRARY) {
        composable(Routes.LIBRARY) {
            LibraryScreen(
                onBrowse = { navController.navigate(Routes.BROWSE) },
                onVoiceClick = { id -> navController.navigate(Routes.voiceDetail(id)) },
                onImport = { uri ->
                    navController.navigate(Routes.customImport(uri))
                },
                onOpenQuickSwitch = onOpenQuickSwitch,
            )
        }
        composable(Routes.BROWSE) {
            BrowseScreen(
                onBack = { navController.popBackStack() },
                onVoiceClick = { id -> navController.navigate(Routes.voiceDetail(id)) },
                onOpenQuickSwitch = onOpenQuickSwitch,
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
                onOpenQuickSwitch = onOpenQuickSwitch,
            )
        }
        composable(
            route = Routes.CUSTOM_IMPORT,
            arguments = listOf(navArgument(Routes.ARG_ENCODED_URI) { type = NavType.StringType }),
        ) { entry ->
            val encoded = entry.arguments?.getString(Routes.ARG_ENCODED_URI).orEmpty()
            CustomImportScreen(
                encodedUri = encoded,
                onClose = { navController.popBackStack() },
            )
        }
    }
}

object Routes {
    const val LIBRARY = "library"
    const val BROWSE = "browse"
    const val ARG_VOICE_ID = "voiceId"
    const val VOICE_DETAIL = "voiceDetail/{$ARG_VOICE_ID}"

    const val ARG_ENCODED_URI = "encodedUri"
    const val CUSTOM_IMPORT = "customImport/{$ARG_ENCODED_URI}"

    fun voiceDetail(voiceId: String): String = "voiceDetail/$voiceId"

    /**
     * The picked content URI is double-encoded — once by [URLEncoder] so it
     * survives the navigation arg path segment (`content://...` would otherwise
     * collide with route parsing), and then decoded by the destination VM.
     */
    fun customImport(rawUri: String): String =
        "customImport/${URLEncoder.encode(rawUri, "UTF-8")}"
}
