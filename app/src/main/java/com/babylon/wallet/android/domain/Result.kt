package com.babylon.wallet.android.domain

sealed interface Result<out T> {

    data class Success<T>(val data: T) : Result<T>

    data class Error<T>(
        val message: String?,
        val data: T? = null,
        val exception: Throwable? = null
    ) : Result<T>
}
