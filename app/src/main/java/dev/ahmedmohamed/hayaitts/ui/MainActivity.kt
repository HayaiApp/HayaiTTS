package dev.ahmedmohamed.hayaitts.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import androidx.navigation.compose.rememberNavController
import dev.ahmedmohamed.hayaitts.ui.nav.HayaiTtsNavHost
import dev.ahmedmohamed.hayaitts.ui.theme.HayaiTtsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HayaiTtsTheme {
                Surface {
                    HayaiTtsNavHost(rememberNavController())
                }
            }
        }
    }
}
