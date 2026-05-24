package dev.ahmedmohamed.hayaitts.core.konsist

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.verify.assertFalse
import org.junit.Test

/**
 * Theme + design-system invariants. These rules enforce the monochrome
 * design contract and the "one shared primitive per concept" rule.
 *
 * If a rule needs to change, update both this file and `docs/ARCHITECTURE.md`.
 */
class ThemeRulesTest {

    private val sources by lazy { Konsist.scopeFromProduction() }

    @Test
    fun `no production code references colorScheme tertiary slots`() {
        // The monochrome theme in Theme.kt binds `tertiary*` to the same
        // neutral steps as `secondary*`, but callers that explicitly reach
        // for tertiary still betray the intent — they implied a separate
        // accent color where none exists. Use `secondary*` or
        // `surfaceContainerHigh` instead.
        sources
            .files
            .filterNot { it.path.endsWith("Theme.kt") }
            .assertFalse { file ->
                Regex("colorScheme\\.tertiary").containsMatchIn(file.text)
            }
    }

    @Test
    fun `no production code calls raw TopAppBar outside HayaiTopBar`() {
        // Plain `TopAppBar` is the small/non-flexible variant; it ignores
        // `scrollBehavior` collapse animations and reserves a fixed 64dp
        // height. Every screen should use HayaiTopBar so the bar collapses
        // on scroll and looks consistent across the app.
        sources
            .files
            .filterNot { it.path.endsWith("HayaiTopBar.kt") }
            .assertFalse { file ->
                Regex("(?<![A-Za-z0-9_])TopAppBar\\(").containsMatchIn(file.text)
            }
    }

    @Test
    fun `no production code calls Material TopAppBar variants outside HayaiTopBar`() {
        // The flexible / large / medium / center-aligned variants also
        // belong inside HayaiTopBar so every screen renders the same visual.
        sources
            .files
            .filterNot { it.path.endsWith("HayaiTopBar.kt") }
            .assertFalse { file ->
                val text = file.text
                Regex("(?<![A-Za-z0-9_])MediumFlexibleTopAppBar\\(").containsMatchIn(text) ||
                    Regex("(?<![A-Za-z0-9_])LargeFlexibleTopAppBar\\(").containsMatchIn(text) ||
                    Regex("(?<![A-Za-z0-9_])MediumTopAppBar\\(").containsMatchIn(text) ||
                    Regex("(?<![A-Za-z0-9_])LargeTopAppBar\\(").containsMatchIn(text) ||
                    Regex("(?<![A-Za-z0-9_])CenterAlignedTopAppBar\\(").containsMatchIn(text)
            }
    }

    // Note: a ContainedLoadingIndicator-outside-HayaiEmpty rule was tried
    // here briefly. It conflicts with legitimate uses in DownloadProgress
    // (active download spinner) and CustomImportScreen (busy state); the
    // original "spinner on empty screen" anti-pattern that motivated the
    // rule was already removed by deleting EmptyState.kt.
}
