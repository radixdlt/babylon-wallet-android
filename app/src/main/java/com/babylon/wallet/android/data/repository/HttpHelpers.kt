package com.babylon.wallet.android.data.repository

import com.babylon.wallet.android.data.gateway.RadixGatewayException
import com.babylon.wallet.android.data.gateway.generated.converter.Serializer
import com.babylon.wallet.android.data.gateway.generated.model.ErrorResponse
import com.babylon.wallet.android.domain.common.Result
import kotlinx.serialization.decodeFromString
import retrofit2.Call
import retrofit2.awaitResponse

suspend inline fun <reified T, A> Call<T>.execute(
    map: (T) -> A,
    error: () -> Exception? = { null }
): Result<A> = try {
    val response = awaitResponse()
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

@Suppress("SwallowedException")
inline fun tryParseServerError(
    error: () -> Exception?,
    errorBodyString: String
): Result.Error {
    val definedError = error.invoke()
    val errorResponse = try {
        Serializer.kotlinxSerializationJson.decodeFromString<ErrorResponse>(errorBodyString)
    } catch (e: Exception) {
        null
    }
    val exception: Exception = definedError ?: RadixGatewayException(
        errorResponse?.message
    )
    return Result.Error(exception = exception)
}
