package com.babylon.wallet.android.data.gateway

import com.babylon.wallet.android.data.gateway.generated.model.EntityDetailsRequest
import com.babylon.wallet.android.data.gateway.generated.model.EntityDetailsResponse
import com.babylon.wallet.android.data.gateway.generated.model.EntityMetadataRequest
import com.babylon.wallet.android.data.gateway.generated.model.EntityMetadataResponse
import com.babylon.wallet.android.data.gateway.generated.model.EntityOverviewRequest
import com.babylon.wallet.android.data.gateway.generated.model.EntityOverviewResponse
import com.babylon.wallet.android.data.gateway.generated.model.EntityResourcesRequest
import com.babylon.wallet.android.data.gateway.generated.model.EntityResourcesResponse
import com.babylon.wallet.android.data.gateway.generated.model.GatewayInfoResponse
import com.babylon.wallet.android.data.gateway.generated.model.RecentTransactionsRequest
import com.babylon.wallet.android.data.gateway.generated.model.RecentTransactionsResponse
import com.babylon.wallet.android.data.gateway.generated.model.TransactionDetailsRequest
import com.babylon.wallet.android.data.gateway.generated.model.TransactionDetailsResponse
import com.babylon.wallet.android.data.gateway.generated.model.TransactionStatusRequest
import com.babylon.wallet.android.data.gateway.generated.model.TransactionStatusResponse
import com.babylon.wallet.android.data.gateway.generated.model.TransactionSubmitRequest
import com.babylon.wallet.android.data.gateway.generated.model.TransactionSubmitResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface GatewayApi {

    @POST("gateway")
    suspend fun gatewayInfo(@Body body: Any): Response<GatewayInfoResponse>

    @POST("entity/details")
    suspend fun entityDetails(@Body entityDetailsRequest: EntityDetailsRequest): Response<EntityDetailsResponse>

    @POST("entity/metadata")
    suspend fun entityMetadata(@Body entityMetadataRequest: EntityMetadataRequest): Response<EntityMetadataResponse>

    @POST("entity/overview")
    suspend fun entityOverview(@Body entityOverviewRequest: EntityOverviewRequest): Response<EntityOverviewResponse>

    @POST("entity/resources")
    suspend fun entityResources(@Body entityResourcesRequest: EntityResourcesRequest): Response<EntityResourcesResponse>

    @POST("transaction/recent")
    suspend fun recentTransactions(
        @Body recentTransactionsRequest: RecentTransactionsRequest
    ): Response<RecentTransactionsResponse>

    @POST("transaction/submit")
    suspend fun submitTransaction(
        @Body transactionSubmitRequest: TransactionSubmitRequest
    ): Response<TransactionSubmitResponse>

    @POST("transaction/details")
    suspend fun transactionDetails(
        @Body transactionDetailsRequest: TransactionDetailsRequest
    ): Response<TransactionDetailsResponse>

    @POST("transaction/status")
    suspend fun transactionStatus(
        @Body transactionStatusRequest: TransactionStatusRequest
    ): Response<TransactionStatusResponse>
}
