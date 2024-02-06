package com.babylon.wallet.android.data.gateway.apis

import com.babylon.wallet.android.data.gateway.generated.infrastructure.CollectionFormats.*
import com.babylon.wallet.android.data.gateway.generated.models.StreamTransactionsRequest
import com.babylon.wallet.android.data.gateway.generated.models.StreamTransactionsResponse
import retrofit2.Response
import retrofit2.http.*

interface StreamApi {
    /**
     * Get Transactions Stream
     * Returns transactions which have been committed to the ledger. [Check detailed documentation for brief explanation](#section/Using-the-streamtransactions-endpoint) 
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
