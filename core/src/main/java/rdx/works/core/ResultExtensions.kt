package rdx.works.core

inline fun <T, S> Result<T>.then(other: (T) -> Result<S>): Result<S> = if (isSuccess) {
    other(getOrNull()!!)
} else {
    Result.failure(exceptionOrNull()!!)
}
