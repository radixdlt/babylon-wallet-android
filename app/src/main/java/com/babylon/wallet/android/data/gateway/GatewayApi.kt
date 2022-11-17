package com.babylon.wallet.android.data.gateway

import com.babylon.wallet.android.data.gateway.generated.model.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface GatewayApi {

    /**
     * Get Gateway Info
     * Returns the Gateway API version, network and current ledger state.
     * Responses:
     *  - 200: The Network
     *
     * @param body
     * @return [GatewayInfoResponse]
     */
    @POST("gateway")
    suspend fun gatewayInfo(@Body body: Any): Response<GatewayInfoResponse>

    @POST("entity/details")
    suspend fun entityDetails(@Body entityDetailsRequest: EntityDetailsRequest): Response<EntityDetailsResponse>

    /**
     * Entity Metadata
     * TBD
     * Responses:
     *  - 200: Entity Metadata
     *
     * @param entityMetadataRequest
     * @return [EntityMetadataResponse]
     */
    @POST("entity/metadata")
    suspend fun entityMetadata(@Body entityMetadataRequest: EntityMetadataRequest): Response<EntityMetadataResponse>

    /**
     * Entity Overview
     * TBD
     * Responses:
     *  - 200: Entity Details
     *
     * @param entityOverviewRequest
     * @return [EntityOverviewResponse]
     */
    @POST("entity/overview")
    suspend fun entityOverview(@Body entityOverviewRequest: EntityOverviewRequest): Response<EntityOverviewResponse>

    /**
     * Entity Resources
     * TBD
     * Responses:
     *  - 200: Entity Resources
     *
     * @param entityResourcesRequest
     * @return [EntityResourcesResponse]
     */
    @POST("entity/resources")
    suspend fun entityResources(@Body entityResourcesRequest: EntityResourcesRequest): Response<EntityResourcesResponse>

//    /**
//     * Preview Transaction
//     * Previews transaction against the network.
//     * Responses:
//     *  - 200: Successful Preview
//     *
//     * @param body
//     * @return [kotlin.Any]
//     */
//    @POST("transaction/preview")
//    suspend fun previewTransaction(@Body body: kotlin.Any): Response<kotlin.Any>

    /**
     * Get Recent Transactions
     * Returns user-initiated transactions which have been succesfully committed to the ledger. The transactions are returned in a paginated format, ordered by most recent.
     * Responses:
     *  - 200: A page of the most recent transactions
     *
     * @param recentTransactionsRequest
     * @return [RecentTransactionsResponse]
     */
    @POST("transaction/recent")
    suspend fun recentTransactions(@Body recentTransactionsRequest: RecentTransactionsRequest): Response<RecentTransactionsResponse>

    /**
     * Submit Transaction
     * Submits a signed transaction payload to the network. The transaction identifier from finalize or submit can then be used to track the transaction status.
     * Responses:
     *  - 200: Successful Submission
     *
     * @param transactionSubmitRequest
     * @return [TransactionSubmitResponse]
     */
    @POST("transaction/submit")
    suspend fun submitTransaction(@Body transactionSubmitRequest: TransactionSubmitRequest): Response<TransactionSubmitResponse>

    /**
     * Transaction Details
     * Returns the status and contents of the transaction with the given transaction identifier. Transaction identifiers which aren&#39;t recognised as either belonging to a committed transaction or a transaction submitted through this Network Gateway may return a &#x60;TransactionNotFoundError&#x60;. Transaction identifiers relating to failed transactions will, after a delay, also be reported as a &#x60;TransactionNotFoundError&#x60;.
     * Responses:
     *  - 200: Transaction Status
     *
     * @param transactionDetailsRequest
     * @return [TransactionDetailsResponse]
     */
    @POST("transaction/details")
    suspend fun transactionDetails(@Body transactionDetailsRequest: TransactionDetailsRequest): Response<TransactionDetailsResponse>

    /**
     * Transaction Status
     * Returns the status and contents of the transaction with the given transaction identifier. Transaction identifiers which aren&#39;t recognised as either belonging to a committed transaction or a transaction submitted through this Network Gateway may return a &#x60;TransactionNotFoundError&#x60;. Transaction identifiers relating to failed transactions will, after a delay, also be reported as a &#x60;TransactionNotFoundError&#x60;.
     * Responses:
     *  - 200: Transaction Status
     *
     * @param transactionStatusRequest
     * @return [TransactionStatusResponse]
     */
    @POST("transaction/status")
    suspend fun transactionStatus(@Body transactionStatusRequest: TransactionStatusRequest): Response<TransactionStatusResponse>
}