package com.babylon.wallet.android.domain

sealed interface Result<out T> {

    data class Success<T>(val data: T) : Result<T>

    data class Error<T>(
        val message: String?,
        val data: T? = null,
        val exception: Throwable? = null
    ) : Result<T>
}

suspend fun <T> Result<T>.onValue(action: suspend (T) -> Unit) {
    if (this is Result.Success) {
        action(this.data)
    }
}

fun <T> Result<T>.onError(action: (Result.Error<T>) -> Unit) {
    if (this is Result.Error) {
        action(this)
    }
}
