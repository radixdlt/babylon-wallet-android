package com.babylon.wallet.android.data.gateway.apis

import com.babylon.wallet.android.data.gateway.generated.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

import com.babylon.wallet.android.data.gateway.generated.models.ErrorResponse
import com.babylon.wallet.android.data.gateway.generated.models.TransactionCommittedDetailsRequest
import com.babylon.wallet.android.data.gateway.generated.models.TransactionCommittedDetailsResponse
import com.babylon.wallet.android.data.gateway.generated.models.TransactionConstructionResponse
import com.babylon.wallet.android.data.gateway.generated.models.TransactionPreviewRequest
import com.babylon.wallet.android.data.gateway.generated.models.TransactionPreviewResponse
import com.babylon.wallet.android.data.gateway.generated.models.TransactionStatusRequest
import com.babylon.wallet.android.data.gateway.generated.models.TransactionStatusResponse
import com.babylon.wallet.android.data.gateway.generated.models.TransactionSubmitRequest
import com.babylon.wallet.android.data.gateway.generated.models.TransactionSubmitResponse

interface TransactionApi {
    /**
     * Get Committed Transaction Details
     * Returns the committed details and receipt of the transaction for a given transaction identifier. Transaction identifiers which don&#39;t correspond to a committed transaction will return a &#x60;TransactionNotFoundError&#x60;. 
     * Responses:
     *  - 200: Transaction Status
     *  - 4XX: Client-originated request error
     *
     * @param transactionCommittedDetailsRequest 
     * @return [TransactionCommittedDetailsResponse]
     */
    @POST("transaction/committed-details")
    suspend fun transactionCommittedDetails(@Body transactionCommittedDetailsRequest: TransactionCommittedDetailsRequest): Response<TransactionCommittedDetailsResponse>

    /**
     * Get Construction Metadata
     * Returns information needed to construct a new transaction including current &#x60;epoch&#x60; number. 
     * Responses:
     *  - 200: Returns information needed to construct transaction. 
     *
     * @return [TransactionConstructionResponse]
     */
    @POST("transaction/construction")
    suspend fun transactionConstruction(): Response<TransactionConstructionResponse>

    /**
     * Preview Transaction
     * Previews transaction against the network. This endpoint is effectively a proxy towards the Core API &#x60;/v0/transaction/preview&#x60; endpoint. See the Core API documentation for more details. 
     * Responses:
     *  - 200: Successful Preview
     *  - 4XX: Client-originated request error
     *
     * @param transactionPreviewRequest 
     * @return [TransactionPreviewResponse]
     */
    @POST("transaction/preview")
    suspend fun transactionPreview(@Body transactionPreviewRequest: TransactionPreviewRequest): Response<TransactionPreviewResponse>

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
    suspend fun transactionStatus(@Body transactionStatusRequest: TransactionStatusRequest): Response<TransactionStatusResponse>

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
    suspend fun transactionSubmit(@Body transactionSubmitRequest: TransactionSubmitRequest): Response<TransactionSubmitResponse>

}
