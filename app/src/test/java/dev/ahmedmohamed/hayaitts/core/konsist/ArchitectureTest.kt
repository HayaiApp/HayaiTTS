package dev.ahmedmohamed.hayaitts.core.konsist

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.ext.list.modifierprovider.withoutPrivateModifier
import com.lemonappdev.konsist.api.verify.assertFalse
import com.lemonappdev.konsist.api.verify.assertTrue
import org.junit.Test

/**
 * Architectural invariants for the v2 codebase. Runs as a normal JUnit test
 * (`./gradlew :app:test`) so violations fail CI.
 *
 * Rules mirror the contract in `docs/ARCHITECTURE.md`. If a rule needs to
 * change, update both this file and the doc.
 */
class ArchitectureTest {

    private val sources by lazy { Konsist.scopeFromProduction() }

    @Test
    fun `domain layer does not import android sdk`() {
        sources
            .files
            .filter { it.inPackage("dev.ahmedmohamed.hayaitts.domain") }
            .assertFalse { file ->
                file.imports.any { import ->
                    val name = import.name
                    (name.startsWith("android.") || name.startsWith("androidx.")) &&
                        !name.startsWith("androidx.annotation.")
                }
            }
    }

    @Test
    fun `domain layer does not import data or ui`() {
        sources
            .files
            .filter { it.inPackage("dev.ahmedmohamed.hayaitts.domain") }
            .assertFalse { file ->
                file.imports.any { import ->
                    import.name.startsWith("dev.ahmedmohamed.hayaitts.data") ||
                        import.name.startsWith("dev.ahmedmohamed.hayaitts.ui")
                }
            }
    }

    @Test
    fun `data layer does not import ui`() {
        sources
            .files
            .filter { it.inPackage("dev.ahmedmohamed.hayaitts.data") }
            .assertFalse { file ->
                file.imports.any { it.name.startsWith("dev.ahmedmohamed.hayaitts.ui") }
            }
    }

    @Test
    fun `repository interfaces live in domain`() {
        sources
            .interfaces()
            .filter { it.name.endsWith("Repository") }
            .assertTrue { iface ->
                iface.packagee?.name?.startsWith("dev.ahmedmohamed.hayaitts.domain") == true
            }
    }

    @Test
    fun `repository impls end in Impl and live in data`() {
        sources
            .classes()
            .filter { it.name.endsWith("RepositoryImpl") }
            .assertTrue { impl ->
                impl.packagee?.name?.startsWith("dev.ahmedmohamed.hayaitts.data") == true
            }
    }

    @Test
    fun `production code does not reference Dispatchers directly outside core`() {
        // Framework-constructed classes (WorkManager Workers, BroadcastReceivers)
        // cannot accept a DispatcherProvider via constructor injection because
        // Android instantiates them. They are permitted to import Dispatchers.
        sources
            .files
            .filterNot { it.inPackage("dev.ahmedmohamed.hayaitts.core") }
            .filterNot { it.path.endsWith("Worker.kt") }
            .filterNot { it.path.endsWith("Receiver.kt") }
            .assertFalse { file ->
                file.imports.any { it.name == "kotlinx.coroutines.Dispatchers" }
            }
    }

    @Test
    fun `viewmodels expose no Entity-typed properties`() {
        sources
            .classes()
            .withoutPrivateModifier()
            .filter { it.name.endsWith("ViewModel") }
            .assertFalse { vm ->
                vm.properties().any { property ->
                    val typeName = property.type?.name.orEmpty()
                    typeName.endsWith("Entity")
                }
            }
    }

    private fun com.lemonappdev.konsist.api.declaration.KoFileDeclaration.inPackage(prefix: String): Boolean {
        val name = packagee?.name.orEmpty()
        return name == prefix || name.startsWith("$prefix.")
    }
}
