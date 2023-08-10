package com.babylon.wallet.android.data.gateway.generated.apis

import com.babylon.wallet.android.data.gateway.generated.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

import com.babylon.wallet.android.data.gateway.generated.models.ErrorResponse
import com.babylon.wallet.android.data.gateway.generated.models.StreamTransactionsRequest
import com.babylon.wallet.android.data.gateway.generated.models.StreamTransactionsResponse

interface StreamApi {
    /**
     * Get Transactions Stream
     * Returns transactions which have been committed to the ledger. 
     * Responses:
     *  - 200: Transactions (paginated)
     *  - 4XX: Client-originated request error
     *
     * @param streamTransactionsRequest 
     * @return [StreamTransactionsResponse]
     */
    @POST("stream/transactions")
    suspend fun streamTransactions(@Body streamTransactionsRequest: StreamTransactionsRequest): Response<StreamTransactionsResponse>

}
