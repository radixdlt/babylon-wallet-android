package com.babylon.wallet.android.domain.common

import com.babylon.wallet.android.R

sealed interface Result<out T> {

    data class Success<T>(val data: T) : Result<T>

    data class Error(
        val exception: Throwable? = null
    ) : Result<Nothing>
}

suspend fun <T> Result<T>.onValue(action: suspend (T) -> Unit) {
    if (this is Result.Success) {
        action(this.data)
    }
}

suspend fun <T, R> Result<T>.map(action: suspend (T) -> Result<R>): Result<R> {
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

suspend fun <T> Result<T>.onError(action: suspend (Throwable?) -> Unit) {
    if (this is Result.Error) {
        action(this.exception)
    }
}
