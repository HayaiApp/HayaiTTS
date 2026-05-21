package dev.ahmedmohamed.hayaitts.core.result

/**
 * Typed, exhaustive result for repository and use-case returns. Replaces
 * thrown exceptions at the data-layer boundary so callers must handle the
 * failure branch in a `when (...)`.
 *
 * Repositories at module edges (Room, OkHttp, filesystem) wrap their raw
 * exceptions into a typed [AppError] before returning. UseCases compose with
 * [map] / [flatMap] / [fold] without touching `try/catch`.
 */
sealed interface Outcome<out T> {
    data class Success<T>(val value: T) : Outcome<T>
    data class Failure(val error: AppError, val cause: Throwable? = null) : Outcome<Nothing>
}

/** Domain-level error taxonomy. New variants are added here, not by string. */
sealed interface AppError {
    data object Network : AppError
    data object Storage : AppError
    data object Cancelled : AppError
    data class Catalog(val reason: String) : AppError
    data class Runtime(val family: String) : AppError
    data class Validation(val field: String, val reason: String) : AppError
    data class Unknown(val message: String) : AppError
}

inline fun <T, R> Outcome<T>.map(transform: (T) -> R): Outcome<R> = when (this) {
    is Outcome.Success -> Outcome.Success(transform(value))
    is Outcome.Failure -> this
}

inline fun <T, R> Outcome<T>.flatMap(transform: (T) -> Outcome<R>): Outcome<R> = when (this) {
    is Outcome.Success -> transform(value)
    is Outcome.Failure -> this
}

inline fun <T, R> Outcome<T>.fold(onSuccess: (T) -> R, onFailure: (AppError, Throwable?) -> R): R =
    when (this) {
        is Outcome.Success -> onSuccess(value)
        is Outcome.Failure -> onFailure(error, cause)
    }

/** Convenience: wrap a throwing block into an [Outcome]. */
inline fun <T> runCatchingOutcome(
    classify: (Throwable) -> AppError = { AppError.Unknown(it.message ?: it.javaClass.simpleName) },
    block: () -> T,
): Outcome<T> = try {
    Outcome.Success(block())
} catch (t: kotlinx.coroutines.CancellationException) {
    throw t
} catch (t: Throwable) {
    Outcome.Failure(classify(t), t)
}

fun <T> T.asSuccess(): Outcome<T> = Outcome.Success(this)
fun AppError.asFailure(cause: Throwable? = null): Outcome<Nothing> = Outcome.Failure(this, cause)
