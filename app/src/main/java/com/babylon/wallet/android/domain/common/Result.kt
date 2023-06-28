package com.babylon.wallet.android.domain.common

// Temporary solution until we replace our own Result to Kotlin's Result
typealias KotlinResult<T> = kotlin.Result<T>

sealed interface Result<out T> {

    data class Success<T>(val data: T) : Result<T>

    data class Error(
        val exception: Throwable? = null
    ) : Result<Nothing>
}

suspend fun <T> Result<T>.onValue(action: suspend (T) -> Unit) = apply {
    if (this is Result.Success) {
        action(this.data)
    }
}

suspend fun <T, R> Result<T>.map(action: suspend (T) -> R): Result<R> {
    return when (this) {
        is Result.Error -> Result.Error(this.exception)
        is Result.Success -> Result.Success(action(data))
    }
}

inline fun <T, R> Result<T>.switchMap(action: (T) -> Result<R>): Result<R> {
    return when (this) {
        is Result.Error -> Result.Error(this.exception)
        is Result.Success -> action(this.data)
    }
}

fun <T> Result<T>.value(): T? {
    if (this is Result.Success) {
        return data
    }
    return null
}

suspend fun <T> Result<T>.onError(action: suspend (Throwable?) -> Unit) = apply {
    if (this is Result.Error) {
        action(this.exception)
    }
}

fun <T> Result<T>.asKotlinResult(): KotlinResult<T> = when (this) {
    is Result.Success -> KotlinResult.success(value = data)
    is Result.Error -> KotlinResult.failure(exception = exception ?: error(""))
}
