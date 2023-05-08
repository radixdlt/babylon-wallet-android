package com.babylon.wallet.android.data.gateway.apis

import com.babylon.wallet.android.data.gateway.generated.models.TransactionConstructionResponse
import com.babylon.wallet.android.data.gateway.generated.models.TransactionPreviewRequest
import com.babylon.wallet.android.data.gateway.generated.models.TransactionPreviewResponse
import com.babylon.wallet.android.data.gateway.generated.models.TransactionStatusRequest
import com.babylon.wallet.android.data.gateway.generated.models.TransactionStatusResponse
import com.babylon.wallet.android.data.gateway.generated.models.TransactionSubmitRequest
import com.babylon.wallet.android.data.gateway.generated.models.TransactionSubmitResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface TransactionApi {

    /**
     * Get Construction Metadata
     * Returns information needed to construct a new transaction including current &#x60;epoch&#x60; number.
     * Responses:
     *  - 200: Returns information needed to construct transaction.
     *
     * @return [TransactionConstructionResponse]
     */
    @POST("transaction/construction")
    fun transactionConstruction(): Call<TransactionConstructionResponse>

    /**
     * Submit Transaction
     * Submits a signed transaction payload to the network.
     * Responses:
     *  - 200: Successful Submission
     *  - 4XX: Client-originated request error
     *
     * @param transactionSubmitRequest
     * @return [TransactionSubmitResponse]
     */
    @POST("transaction/submit")
    fun submitTransaction(
        @Body transactionSubmitRequest: TransactionSubmitRequest
    ): Call<TransactionSubmitResponse>

    /**
     * Get Transaction Status
     * Returns overall transaction status and all of its known payloads based on supplied intent hash.
     * Responses:
     *  - 200: Transaction Status
     *  - 4XX: Client-originated request error
     *
     * @param transactionStatusRequest
     * @return [TransactionStatusResponse]
     */
    @POST("transaction/status")
    fun transactionStatus(
        @Body transactionStatusRequest: TransactionStatusRequest
    ): Call<TransactionStatusResponse>

    /**
     * Preview Transaction
     * Previews transaction against the network.
     * This endpoint is effectively a proxy towards CoreApi&#39;s &#x60;/v0/transaction/preview&#x60; endpoint.
     * See CoreApi&#39;s documentation for more details.
     * Responses:
     *  - 200: Successful Preview
     *  - 4XX: Client-originated request error
     *
     * @param transactionPreviewRequest
     * @return [TransactionPreviewResponse]
     */
    @POST("transaction/preview")
    fun transactionPreview(
        @Body transactionPreviewRequest: TransactionPreviewRequest
    ): Call<TransactionPreviewResponse>
}
