package com.babylon.wallet.android.domain.common

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

fun <T> Result<T>.onError(action: (Throwable?) -> Unit) {
    if (this is Result.Error) {
        action(this.exception)
    }
}
