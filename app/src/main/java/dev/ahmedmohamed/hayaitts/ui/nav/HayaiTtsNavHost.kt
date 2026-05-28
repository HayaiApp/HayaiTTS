package dev.ahmedmohamed.hayaitts.ui.nav

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import dev.ahmedmohamed.hayaitts.R
import dev.ahmedmohamed.hayaitts.ui.activity.ActivityScreen
import dev.ahmedmohamed.hayaitts.ui.browse.BrowseScreen
import dev.ahmedmohamed.hayaitts.ui.custom.CustomImportScreen
import dev.ahmedmohamed.hayaitts.ui.detail.VoiceDetailScreen
import dev.ahmedmohamed.hayaitts.ui.library.LibraryScreen
import dev.ahmedmohamed.hayaitts.ui.onboarding.OnboardingScreen
import dev.ahmedmohamed.hayaitts.ui.settings.SettingsScreen
import dev.ahmedmohamed.hayaitts.ui.studio.StudioScreen
import java.net.URLEncoder

/**
 * Top-level navigation: a [NavigationBar] hosts five top-level destinations
 * (Library / Browse / Studio / Activity / Settings). Each tab pushes detail
 * routes (voice detail, custom import) on top of itself without disturbing
 * the bottom bar.
 *
 * The Onboarding route is special — it renders without the bottom bar so
 * the first-launch flow is uncluttered. The graph picks it as the start
 * destination on the very first launch only; subsequent launches start on
 * Library.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HayaiTtsNavHost(
    navController: NavHostController,
    onOpenQuickSwitch: () -> Unit,
    startDestination: String = Routes.LIBRARY,
    onCompleteOnboarding: () -> Unit = {},
) {
    val currentEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentEntry?.destination?.route
    val showBottomBar = currentRoute in BOTTOM_BAR_ROUTES

    Scaffold(
        // Each screen's `HayaiScreenChrome` (the floating pill bar) handles
        // its own status-bar inset. If we leave the default systemBars on
        // this outer Scaffold, the inner bar gets pushed down by the inset
        // *twice* and the area between the system status bar and the pill
        // reads as a filled "top app bar background". Restrict the outer
        // inset to the navigation bars so the bottom NavigationBar still
        // floats above the system nav.
        contentWindowInsets = WindowInsets.navigationBars,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    Tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(stringResource(tab.labelRes)) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            composable(Routes.ONBOARDING) {
                OnboardingScreen(
                    onComplete = {
                        onCompleteOnboarding()
                        navController.navigate(Routes.LIBRARY) {
                            popUpTo(Routes.ONBOARDING) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable(Routes.LIBRARY) {
                LibraryScreen(
                    onBrowse = { navController.navigate(Routes.BROWSE) },
                    onVoiceClick = { id -> navController.navigate(Routes.voiceDetail(id)) },
                    onImport = { uri -> navController.navigate(Routes.customImport(uri)) },
                    onOpenQuickSwitch = onOpenQuickSwitch,
                    onOpenDownloads = { navController.navigate(Routes.ACTIVITY) },
                )
            }
            composable(Routes.BROWSE) {
                BrowseScreen(
                    onBack = { navController.popBackStack() },
                    onVoiceClick = { id -> navController.navigate(Routes.voiceDetail(id)) },
                    onOpenQuickSwitch = onOpenQuickSwitch,
                )
            }
            composable(Routes.STUDIO) {
                StudioScreen(
                    onBack = { navController.popBackStack() },
                    onOpenQuickSwitch = onOpenQuickSwitch,
                )
            }
            composable(
                route = Routes.ACTIVITY,
                deepLinks = listOf(navDeepLink { uriPattern = "hayaitts://downloads" }),
            ) {
                ActivityScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(onBack = { navController.popBackStack() })
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
                    onOpenPlayground = { navController.navigate(Routes.STUDIO) },
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
}

private data class Tab(
    val route: String,
    val icon: ImageVector,
    val labelRes: Int,
)

private val Tabs = listOf(
    Tab(Routes.LIBRARY, Icons.Outlined.LibraryMusic, R.string.nav_library),
    Tab(Routes.BROWSE, Icons.Outlined.Search, R.string.nav_browse),
    Tab(Routes.STUDIO, Icons.Outlined.Tune, R.string.nav_studio),
    Tab(Routes.ACTIVITY, Icons.Outlined.Bolt, R.string.nav_activity),
    Tab(Routes.SETTINGS, Icons.Outlined.Settings, R.string.nav_settings),
)

private val BOTTOM_BAR_ROUTES = Tabs.map { it.route }.toSet()

object Routes {
    const val ONBOARDING = "onboarding"
    const val LIBRARY = "library"
    const val BROWSE = "browse"
    const val STUDIO = "studio"
    const val ACTIVITY = "activity"
    const val SETTINGS = "settings"
    const val ARG_VOICE_ID = "voiceId"
    const val VOICE_DETAIL = "voiceDetail/{$ARG_VOICE_ID}"

    const val ARG_ENCODED_URI = "encodedUri"
    const val CUSTOM_IMPORT = "customImport/{$ARG_ENCODED_URI}"

    fun voiceDetail(voiceId: String): String = "voiceDetail/$voiceId"

    /**
     * Picked SAF URIs are URL-encoded so the `content://...` literal doesn't
     * collide with route path parsing. The destination VM decodes once.
     */
    fun customImport(rawUri: String): String =
        "customImport/${URLEncoder.encode(rawUri, "UTF-8")}"
}
