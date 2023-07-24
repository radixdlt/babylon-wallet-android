package rdx.works.core

inline fun <FirstResult, SecondResult> Result<FirstResult>.then(
    other: (FirstResult) -> Result<SecondResult>
): Result<Pair<FirstResult, SecondResult>> = fold(
    onSuccess = { receivedValue ->
        other(receivedValue).map { receivedValue to it }
    },
    onFailure = {
        Result.failure(it)
    }
)
