package rdx.works.peerdroid.data

import kotlinx.coroutines.CancellationException

/**
 * Like [runCatching], but lets coroutine cancellation propagate instead of turning it into a failure, and does not
 * catch [Error]s.
 */
internal inline fun <T> runCatchingCancellable(block: () -> T): Result<T> = try {
    Result.success(block())
} catch (exception: Exception) {
    if (exception is CancellationException) {
        throw exception
    }
    Result.failure(exception)
}
