package dev.ahmedmohamed.hayaitts.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import dev.ahmedmohamed.hayaitts.data.onboarding.OnboardingPreferences
import dev.ahmedmohamed.hayaitts.data.update.UpdateStatus
import dev.ahmedmohamed.hayaitts.ui.nav.HayaiTtsNavHost
import dev.ahmedmohamed.hayaitts.ui.nav.Routes
import dev.ahmedmohamed.hayaitts.ui.quickswitch.VoiceQuickSwitcher
import dev.ahmedmohamed.hayaitts.ui.theme.HayaiTtsTheme
import dev.ahmedmohamed.hayaitts.ui.update.UpdateDialog
import dev.ahmedmohamed.hayaitts.ui.update.UpdateViewModel
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.compose.koinViewModel

/**
 * Single-activity host. The voice quick-switcher sheet is hoisted here so it
 * is reachable from every destination via the shared icon in each screen's
 * top app bar — see [HayaiTtsNavHost] which forwards an `onOpenQuickSwitch`
 * callback to each leaf screen.
 *
 * On first launch the activity routes to the onboarding flow instead of
 * Library; subsequent launches land directly on Library. The flag is owned
 * by [OnboardingPreferences].
 */
class MainActivity : ComponentActivity() {

    private val onboardingPrefs: OnboardingPreferences by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HayaiTtsTheme {
                Surface {
                    val navController = rememberNavController()
                    var quickSwitchOpen by remember { mutableStateOf(false) }

                    // Snapshot the flag synchronously the first time the
                    // composition runs so the NavHost picks a deterministic
                    // start destination — we cannot wait on a Flow to emit
                    // before composing the graph or `rememberSaveable` state
                    // would reset on every cold start.
                    val onboardingComplete by onboardingPrefs.isComplete
                        .collectAsState(initial = null)

                    // Until we know, render nothing under the Surface. The
                    // first DataStore read is sub-frame on real devices so
                    // this never produces a visible flash.
                    val resolved = onboardingComplete ?: return@Surface

                    val startDestination = if (resolved) Routes.LIBRARY else Routes.ONBOARDING

                    HayaiTtsNavHost(
                        navController = navController,
                        onOpenQuickSwitch = { quickSwitchOpen = true },
                        startDestination = startDestination,
                        onCompleteOnboarding = {
                            lifecycleScope.launch { onboardingPrefs.setComplete(true) }
                        },
                    )

                    VoiceQuickSwitcher(
                        visible = quickSwitchOpen,
                        onDismiss = { quickSwitchOpen = false },
                        onManage = {
                            quickSwitchOpen = false
                            navController.navigate(Routes.LIBRARY) {
                                popUpTo(Routes.LIBRARY) { inclusive = false }
                                launchSingleTop = true
                            }
                        },
                        onAddVoice = {
                            quickSwitchOpen = false
                            navController.navigate(Routes.BROWSE) {
                                launchSingleTop = true
                            }
                        },
                    )

                    // Auto-updater host: kick off a quiet check on first
                    // composition (the 6h debounce inside UpdateChecker means
                    // this is a no-op on most launches), then if the resulting
                    // status is `Available` and the user has not dismissed it
                    // this process, surface the dialog as a top-level overlay.
                    val updateVm: UpdateViewModel = koinViewModel()
                    val updateStatus by updateVm.status.collectAsStateWithLifecycle()
                    val updateDownload by updateVm.download.collectAsStateWithLifecycle()
                    val updateDismissed by updateVm.dialogDismissed.collectAsStateWithLifecycle()
                    LaunchedEffect(Unit) { updateVm.autoCheck() }

                    val available = updateStatus as? UpdateStatus.Available
                    if (available != null && !updateDismissed) {
                        UpdateDialog(
                            available = available,
                            download = updateDownload,
                            onInstall = updateVm::startInstall,
                            onCancelInstall = updateVm::cancelInstall,
                            onDismiss = updateVm::dismissDialog,
                        )
                    }
                }
            }
        }
    }
}
