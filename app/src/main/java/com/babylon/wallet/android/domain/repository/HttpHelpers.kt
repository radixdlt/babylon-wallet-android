package com.babylon.wallet.android.domain.repository

import com.babylon.wallet.android.domain.Result
import retrofit2.Response

suspend fun <T, A> performHttpRequest(call: suspend () -> Response<T>, map: suspend (T) -> A, error: (() -> Exception)? = null): Result<A> {
    return try {
        val response = call()
        if (response.isSuccessful && response.body() != null) {
            Result.Success(data = map(response.body()!!))
        } else {
            val definedError = error?.invoke()
            Result.Error(message = definedError?.message ?: "Let's parse error from response here", exception = definedError?.cause)
        }
    } catch (e: Exception) {
        Result.Error(e.message, exception = e)
    }
}