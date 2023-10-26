package com.babylon.wallet.android.data.repository

import com.babylon.wallet.android.data.gateway.RadixGatewayException
import com.babylon.wallet.android.data.gateway.generated.infrastructure.Serializer
import com.babylon.wallet.android.data.gateway.generated.models.ErrorResponse
import com.babylon.wallet.android.data.repository.cache.CacheParameters
import kotlinx.serialization.json.Json.Default.serializersModule
import kotlinx.serialization.serializer
import retrofit2.Call
import retrofit2.awaitResponse

@Suppress("SwallowedException")
suspend inline fun <T> Call<T>.toResult(): Result<T> {
    return try {
        val response = awaitResponse()
        val responseBody = response.body()
        if (response.isSuccessful && responseBody != null) {
            Result.success(responseBody)
        } else {
            val errorResponse = try {
                Serializer.kotlinxSerializationJson.decodeFromString<ErrorResponse>(response.errorBody()?.string().orEmpty())
            } catch (e: Exception) {
                null
            }
            Result.failure(RadixGatewayException(errorResponse?.message))
        }
    } catch (e: Exception) {
        Result.failure(RadixGatewayException(e.message, e.cause))
    }
}

@Suppress("SwallowedException")
suspend inline fun <reified T, A> Call<T>.execute(
    cacheParameters: CacheParameters? = null,
    map: (T) -> A,
    error: () -> Throwable? = { null }
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

        if (restored != null) return Result.success(map(restored))

        val response = awaitResponse()
        val responseBody = response.body()
        if (response.isSuccessful && responseBody != null) {
            cacheParameters?.httpCache?.store(
                call = this,
                response = responseBody,
                serializer = serializersModule.serializer()
            )

            Result.success(
                map(responseBody)
            )
        } else {
            val definedError = error.invoke()
            val errorResponse = try {
                Serializer.kotlinxSerializationJson.decodeFromString<ErrorResponse>(response.errorBody()?.string().orEmpty())
            } catch (e: Exception) {
                null
            }
            val exception: Throwable = definedError ?: RadixGatewayException(
                errorResponse?.message
            )
            return Result.failure(exception)
        }
    } catch (e: Exception) {
        val exception = RadixGatewayException(e.message, e.cause)
        Result.failure(exception)
    }
}
