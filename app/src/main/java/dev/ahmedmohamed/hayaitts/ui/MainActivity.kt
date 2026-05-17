package dev.ahmedmohamed.hayaitts.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.compose.rememberNavController
import dev.ahmedmohamed.hayaitts.ui.nav.HayaiTtsNavHost
import dev.ahmedmohamed.hayaitts.ui.nav.Routes
import dev.ahmedmohamed.hayaitts.ui.quickswitch.VoiceQuickSwitcher
import dev.ahmedmohamed.hayaitts.ui.theme.HayaiTtsTheme

/**
 * Single-activity host. The voice quick-switcher sheet is hoisted here so it
 * is reachable from every destination via the shared icon in each screen's
 * top app bar — see [HayaiTtsNavHost] which forwards an `onOpenQuickSwitch`
 * callback to each leaf screen.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HayaiTtsTheme {
                Surface {
                    val navController = rememberNavController()
                    var quickSwitchOpen by remember { mutableStateOf(false) }

                    HayaiTtsNavHost(
                        navController = navController,
                        onOpenQuickSwitch = { quickSwitchOpen = true },
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
                }
            }
        }
    }
}
