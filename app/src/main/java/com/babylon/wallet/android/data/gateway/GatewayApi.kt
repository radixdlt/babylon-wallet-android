@file:Suppress("TooManyFunctions")

package com.babylon.wallet.android.data.gateway

import com.babylon.wallet.android.data.gateway.generated.model.EntityDetailsRequest
import com.babylon.wallet.android.data.gateway.generated.model.EntityDetailsResponse
import com.babylon.wallet.android.data.gateway.generated.model.EntityFungiblesRequest
import com.babylon.wallet.android.data.gateway.generated.model.EntityFungiblesResponse
import com.babylon.wallet.android.data.gateway.generated.model.EntityMetadataRequest
import com.babylon.wallet.android.data.gateway.generated.model.EntityMetadataResponse
import com.babylon.wallet.android.data.gateway.generated.model.EntityNonFungibleIdsRequest
import com.babylon.wallet.android.data.gateway.generated.model.EntityNonFungibleIdsResponse
import com.babylon.wallet.android.data.gateway.generated.model.EntityNonFungiblesRequest
import com.babylon.wallet.android.data.gateway.generated.model.EntityNonFungiblesResponse
import com.babylon.wallet.android.data.gateway.generated.model.EntityOverviewRequest
import com.babylon.wallet.android.data.gateway.generated.model.EntityOverviewResponse
import com.babylon.wallet.android.data.gateway.generated.model.EntityResourcesRequest
import com.babylon.wallet.android.data.gateway.generated.model.EntityResourcesResponse
import com.babylon.wallet.android.data.gateway.generated.model.NonFungibleDataRequest
import com.babylon.wallet.android.data.gateway.generated.model.NonFungibleDataResponse
import com.babylon.wallet.android.data.gateway.generated.model.NonFungibleIdsRequest
import com.babylon.wallet.android.data.gateway.generated.model.NonFungibleIdsResponse
import com.babylon.wallet.android.data.gateway.generated.model.RecentTransactionsRequest
import com.babylon.wallet.android.data.gateway.generated.model.RecentTransactionsResponse
import com.babylon.wallet.android.data.gateway.generated.model.TransactionCommittedDetailsRequest
import com.babylon.wallet.android.data.gateway.generated.model.TransactionCommittedDetailsResponse
import com.babylon.wallet.android.data.gateway.generated.model.TransactionConstructionResponse
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

    @POST("entity/details")
    suspend fun entityDetails(@Body entityDetailsRequest: EntityDetailsRequest): Response<EntityDetailsResponse>

    @POST("entity/fungibles")
    suspend fun entityFungibles(@Body entityFungiblesRequest: EntityFungiblesRequest): Response<EntityFungiblesResponse>

    @POST("entity/metadata")
    suspend fun entityMetadata(@Body entityMetadataRequest: EntityMetadataRequest): Response<EntityMetadataResponse>

    @POST("entity/non-fungible/ids")
    suspend fun entityNonFungibleIds(
        @Body entityNonFungibleIdsRequest: EntityNonFungibleIdsRequest
    ): Response<EntityNonFungibleIdsResponse>

    @POST("entity/non-fungibles")
    suspend fun entityNonFungibles(
        @Body entityNonFungiblesRequest: EntityNonFungiblesRequest
    ): Response<EntityNonFungiblesResponse>

    @POST("non-fungible/data")
    suspend fun nonFungibleData(@Body nonFungibleDataRequest: NonFungibleDataRequest): Response<NonFungibleDataResponse>

    @POST("non-fungible/ids")
    suspend fun nonFungibleIds(@Body nonFungibleIdsRequest: NonFungibleIdsRequest): Response<NonFungibleIdsResponse>

    @POST("entity/overview")
    suspend fun entityOverview(@Body entityOverviewRequest: EntityOverviewRequest): Response<EntityOverviewResponse>

    @POST("entity/resources")
    suspend fun entityResources(@Body entityResourcesRequest: EntityResourcesRequest): Response<EntityResourcesResponse>

    @POST("transaction/committed-details")
    suspend fun transactionCommittedDetails(
        @Body transactionCommittedDetailsRequest: TransactionCommittedDetailsRequest
    ): Response<TransactionCommittedDetailsResponse>

    @POST("transaction/construction")
    suspend fun transactionConstruction(): Response<TransactionConstructionResponse>

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

    @POST("transaction/preview")
    suspend fun transactionPreview(@Body body: Any): Response<Any>
}
