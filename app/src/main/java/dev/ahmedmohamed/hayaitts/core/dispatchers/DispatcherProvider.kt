package dev.ahmedmohamed.hayaitts.core.dispatchers

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * Wraps the global [Dispatchers] singletons behind an injectable interface so
 * tests can substitute a [kotlinx.coroutines.test.TestDispatcher].
 *
 * Production code must depend on this contract, never on `Dispatchers.IO` /
 * `Dispatchers.Default` / `Dispatchers.Main` directly. The Konsist suite in
 * `app/src/test/.../core/konsist/` enforces that rule.
 */
interface DispatcherProvider {
    val main: CoroutineDispatcher
    val io: CoroutineDispatcher
    val default: CoroutineDispatcher
    val unconfined: CoroutineDispatcher
}

class DefaultDispatcherProvider : DispatcherProvider {
    override val main: CoroutineDispatcher = Dispatchers.Main
    override val io: CoroutineDispatcher = Dispatchers.IO
    override val default: CoroutineDispatcher = Dispatchers.Default
    override val unconfined: CoroutineDispatcher = Dispatchers.Unconfined
}
