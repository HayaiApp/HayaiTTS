package dev.ahmedmohamed.hayaitts.core.result

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class OutcomeTest {

    @Test
    fun `map transforms success values`() {
        val r = Outcome.Success(2).map { it * 3 }
        assertTrue(r is Outcome.Success)
        assertEquals(6, (r as Outcome.Success).value)
    }

    @Test
    fun `map leaves failure untouched`() {
        val failure: Outcome<Int> = Outcome.Failure(AppError.Network)
        val r = failure.map { it * 3 }
        assertSame(failure, r)
    }

    @Test
    fun `flatMap chains successes`() {
        val r = Outcome.Success(5).flatMap { Outcome.Success(it + 1) }
        assertEquals(6, (r as Outcome.Success).value)
    }

    @Test
    fun `flatMap short-circuits on failure`() {
        val r: Outcome<Int> = Outcome.Failure(AppError.Storage)
        val out = r.flatMap { Outcome.Success(it + 1) }
        assertTrue(out is Outcome.Failure)
        assertEquals(AppError.Storage, (out as Outcome.Failure).error)
    }

    @Test
    fun `runCatchingOutcome classifies thrown exceptions`() {
        val r = runCatchingOutcome(classify = { AppError.Network }) {
            error("boom")
        }
        assertTrue(r is Outcome.Failure)
        assertEquals(AppError.Network, (r as Outcome.Failure).error)
    }
}
