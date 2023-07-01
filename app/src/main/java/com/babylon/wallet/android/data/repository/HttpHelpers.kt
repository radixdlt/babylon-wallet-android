package com.babylon.wallet.android.data.repository

import com.babylon.wallet.android.data.gateway.RadixGatewayException
import com.babylon.wallet.android.data.gateway.generated.infrastructure.Serializer
import com.babylon.wallet.android.data.gateway.generated.models.ErrorResponse
import com.babylon.wallet.android.data.repository.cache.CacheParameters
import com.babylon.wallet.android.domain.common.Result
import kotlinx.serialization.json.Json.Default.serializersModule
import kotlinx.serialization.serializer
import retrofit2.Call
import retrofit2.awaitResponse

suspend inline fun <reified T, A> Call<T>.execute(
    cacheParameters: CacheParameters? = null,
    map: (T) -> A,
    error: () -> Exception? = { null }
): Result<A> {
    return try {
        val restored = if (cacheParameters != null && !cacheParameters.isCacheOverridden) {
            cacheParameters.httpCache.restore(
                call = this,
                serializer = serializersModule.serializer(),
                timeoutDuration = cacheParameters.timeoutDuration
            )
        } else {
            null
        }

        if (restored != null) return Result.Success(map(restored))

        val response = awaitResponse()
        val responseBody = response.body()
        if (response.isSuccessful && responseBody != null) {
            cacheParameters?.httpCache?.store(
                call = this,
                response = responseBody,
                serializer = serializersModule.serializer()
            )

            Result.Success(data = map(responseBody))
        } else {
            tryParseServerError(error, response.errorBody()?.string().orEmpty())
        }
    } catch (e: Exception) {
        val exception = RadixGatewayException(e.message, e.cause)
        Result.Error(exception = exception)
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
