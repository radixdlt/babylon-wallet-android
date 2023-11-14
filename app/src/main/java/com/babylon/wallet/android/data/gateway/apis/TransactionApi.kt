package com.babylon.wallet.android.data.gateway.apis

import com.babylon.wallet.android.data.gateway.generated.models.TransactionCommittedDetailsRequest
import com.babylon.wallet.android.data.gateway.generated.models.TransactionCommittedDetailsResponse
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
     * Get Committed Transaction Details
     * Returns the committed details and receipt of the transaction for a given transaction identifier. Transaction identifiers which
     * don&#39;t correspond to a committed transaction will return a &#x60;TransactionNotFoundError&#x60;.
     * Responses:
     *  - 200: Transaction Status
     *  - 4XX: Client-originated request error
     *
     * @param transactionCommittedDetailsRequest
     * @return [TransactionCommittedDetailsResponse]
     */
    @POST("transaction/committed-details")
    fun transactionCommittedDetails(
        @Body transactionCommittedDetailsRequest: TransactionCommittedDetailsRequest
    ): Call<TransactionCommittedDetailsResponse>

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
     * Preview Transaction
     * Previews transaction against the network. This endpoint is effectively a proxy towards the Core API
     * &#x60;/v0/transaction/preview&#x60; endpoint. See the Core API documentation for more details.
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
    fun transactionSubmit(
        @Body transactionSubmitRequest: TransactionSubmitRequest
    ): Call<TransactionSubmitResponse>
}
