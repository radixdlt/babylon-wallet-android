package com.babylon.wallet.android.data.repository

import com.babylon.wallet.android.data.gateway.RadixGatewayException
import com.babylon.wallet.android.data.gateway.generated.converter.Serializer
import com.babylon.wallet.android.data.gateway.generated.model.ErrorResponse
import com.babylon.wallet.android.data.repository.cache.HttpCache
import com.babylon.wallet.android.domain.common.Result
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json.Default.serializersModule
import kotlinx.serialization.serializer
import retrofit2.Call
import retrofit2.awaitResponse

suspend inline fun <reified T, A> Call<T>.execute(
    httpCache: HttpCache? = null,
    map: (T) -> A,
    error: () -> Exception? = { null }
): Result<A> {
    try {
        val restored = httpCache?.restore(
            call = this,
            deserializationStrategy = serializersModule.serializer()
        )

        if (restored != null) {
            return Result.Success(map(restored))
        }

        val response = awaitResponse()
        val responseBody = response.body()
        return if (response.isSuccessful && responseBody != null) {
            httpCache?.store(
                call = this,
                response = responseBody,
                serializationStrategy = serializersModule.serializer()
            )

            Result.Success(data = map(responseBody))
        } else {
            tryParseServerError(error, response.errorBody()?.string().orEmpty())
        }
    } catch (e: Exception) {
        val exception = RadixGatewayException(e.message, e.cause)
        return Result.Error(exception = exception)
    }
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
