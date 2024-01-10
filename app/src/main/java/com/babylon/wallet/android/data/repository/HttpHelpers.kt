package com.babylon.wallet.android.data.repository

import com.babylon.wallet.android.data.gateway.generated.infrastructure.Serializer
import com.babylon.wallet.android.data.gateway.generated.models.ErrorResponse
import com.babylon.wallet.android.domain.RadixWalletException
import kotlinx.coroutines.CancellationException
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
            try {
                val error = Serializer.kotlinxSerializationJson.decodeFromString<ErrorResponse>(response.errorBody()?.string().orEmpty())
                Result.failure(RadixWalletException.GatewayException.HttpError(code = error.code, message = error.message))
            } catch (e: Exception) {
                Result.failure(RadixWalletException.GatewayException.ClientError(cause = e))
            }
        }
    } catch (e: Exception) {
        if (e is CancellationException) {
            // In this case we don't need to swallow this error but throw it,
            // so the coroutines can cancel themselves
            throw e
        } else {
            Result.failure(RadixWalletException.GatewayException.ClientError(cause = e))
        }
    }
}
