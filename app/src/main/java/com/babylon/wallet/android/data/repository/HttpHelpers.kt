@file:Suppress("TooGenericExceptionCaught")

package com.babylon.wallet.android.data.repository

import com.babylon.wallet.android.data.gateway.generated.converter.Serializer
import com.babylon.wallet.android.data.gateway.generated.model.ErrorResponse
import com.babylon.wallet.android.domain.Result
import kotlinx.serialization.decodeFromString
import retrofit2.Response

suspend fun <T, A> performHttpRequest(
    call: suspend () -> Response<T>,
    map: suspend (T) -> A,
    error: (() -> Exception)? = null
): Result<A> {
    return try {
        val response = call()
        val responseBody = response.body()
        if (response.isSuccessful && responseBody != null) {
            Result.Success(data = map(responseBody))
        } else {
            val definedError = error?.invoke()
            val errorResponse = Serializer.kotlinxSerializationJson.decodeFromString<ErrorResponse>(
                response.errorBody()?.string().orEmpty()
            )
            Result.Error(
                message = definedError?.message ?: errorResponse.message,
                exception = definedError?.cause,
            )
        }
    } catch (e: Exception) {
        Result.Error(e.message, exception = e)
    }
}
