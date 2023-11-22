package rdx.works.core

inline fun <FirstResult, SecondResult> Result<FirstResult>.then(
    other: (FirstResult) -> Result<SecondResult>
): Result<SecondResult> = fold(
    onSuccess = { receivedValue ->
        other(receivedValue)
    },
    onFailure = {
        Result.failure(it)
    }
)

fun <T> Result<T>.toUnitResult() = map {}
