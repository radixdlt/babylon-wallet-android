package com.babylon.wallet.android.data.gateway.apis

import com.babylon.wallet.android.data.gateway.generated.models.StreamTransactionsRequest
import com.babylon.wallet.android.data.gateway.generated.models.StreamTransactionsResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

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
    fun streamTransactions(@Body streamTransactionsRequest: StreamTransactionsRequest): Call<StreamTransactionsResponse>
}
