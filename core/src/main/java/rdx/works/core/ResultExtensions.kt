package rdx.works.core

inline fun <T, S> Result<T>.then(other: (T) -> Result<S>): Result<S> = fold(
    onSuccess = {
        other(it)
    },
    onFailure = {
        Result.failure(it)
    }
)
