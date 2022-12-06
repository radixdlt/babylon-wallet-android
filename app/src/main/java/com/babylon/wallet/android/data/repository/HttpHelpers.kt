@file:Suppress("TooGenericExceptionCaught")

package com.babylon.wallet.android.data.repository

import com.babylon.wallet.android.data.gateway.RadixGatewayException
import com.babylon.wallet.android.data.gateway.generated.converter.Serializer
import com.babylon.wallet.android.data.gateway.generated.model.ErrorResponse
import com.babylon.wallet.android.data.gateway.hasMessageOrDetails
import com.babylon.wallet.android.data.gateway.model.GatewayErrorResponse
import com.babylon.wallet.android.domain.common.Result
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
            tryParseServerError(error, response.errorBody()?.string().orEmpty())
        }
    } catch (e: Exception) {
        val exception = RadixGatewayException(e.message, e.cause)
        Result.Error(exception = exception)
    }
}

// Jakub: API schema uses ErrorResponse, but somehow on some errors I'm getting different error message format, this is
// why there is 2nd error parsing going on.
private fun tryParseServerError(
    error: (() -> Exception)?,
    errorBodyString: String
): Result.Error {
    val definedError = error?.invoke()
    val errorResponse = Serializer.kotlinxSerializationJson.decodeFromString<ErrorResponse>(
        errorBodyString
    )
    var gatewayErrorResponse: GatewayErrorResponse? = null
    if (!errorResponse.hasMessageOrDetails()) {
        gatewayErrorResponse = Serializer.kotlinxSerializationJson.decodeFromString<GatewayErrorResponse>(
            errorBodyString
        )
    }
    val exception = RadixGatewayException(
        definedError?.message ?: (
            errorResponse.message ?: gatewayErrorResponse?.errors?.values?.firstOrNull()
                ?.firstOrNull()
            )
    )
    return Result.Error(exception = exception)
}
