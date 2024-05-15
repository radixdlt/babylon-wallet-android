package rdx.works.core

inline fun <FirstResult, SecondResult> Result<FirstResult>.then(
    other: (FirstResult) -> Result<SecondResult>
): Result<SecondResult> = fold(
    onSuccess = { receivedValue ->
        try {
            other(receivedValue)
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    },
    onFailure = {
        Result.failure(it)
    }
)
inline fun <T> Result<T>.mapError(
    map: (Throwable) -> Throwable
): Result<T> = fold(
    onSuccess = { Result.success(it) },
    onFailure = { Result.failure(map(it)) }
)

fun <T> Result<T>.toUnitResult() = map {}
