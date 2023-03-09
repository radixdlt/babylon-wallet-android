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
import com.babylon.wallet.android.data.gateway.generated.model.TransactionCommittedDetailsRequest
import com.babylon.wallet.android.data.gateway.generated.model.TransactionCommittedDetailsResponse
import com.babylon.wallet.android.data.gateway.generated.model.TransactionConstructionResponse
import com.babylon.wallet.android.data.gateway.generated.model.TransactionRecentRequest
import com.babylon.wallet.android.data.gateway.generated.model.TransactionRecentResponse
import com.babylon.wallet.android.data.gateway.generated.model.TransactionStatusRequest
import com.babylon.wallet.android.data.gateway.generated.model.TransactionStatusResponse
import com.babylon.wallet.android.data.gateway.generated.model.TransactionSubmitRequest
import com.babylon.wallet.android.data.gateway.generated.model.TransactionSubmitResponse
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface GatewayApi {

    @POST("entity/details")
    fun entityDetails(@Body entityDetailsRequest: EntityDetailsRequest): Call<EntityDetailsResponse>

    @POST("entity/fungibles")
    suspend fun entityFungibles(@Body entityFungiblesRequest: EntityFungiblesRequest): Response<EntityFungiblesResponse>

    @POST("entity/metadata")
    fun entityMetadata(@Body entityMetadataRequest: EntityMetadataRequest): Call<EntityMetadataResponse>

    @POST("entity/non-fungible/ids")
    suspend fun entityNonFungibleIds(
        @Body entityNonFungibleIdsRequest: EntityNonFungibleIdsRequest
    ): Call<EntityNonFungibleIdsResponse>

    @POST("entity/non-fungibles")
    fun entityNonFungibles(
        @Body entityNonFungiblesRequest: EntityNonFungiblesRequest
    ): Call<EntityNonFungiblesResponse>

    @POST("non-fungible/data")
    fun nonFungibleData(@Body nonFungibleDataRequest: NonFungibleDataRequest): Call<NonFungibleDataResponse>

    @POST("non-fungible/ids")
    fun nonFungibleIds(@Body nonFungibleIdsRequest: NonFungibleIdsRequest): Call<NonFungibleIdsResponse>

    @POST("entity/overview")
    fun entityOverview(@Body entityOverviewRequest: EntityOverviewRequest): Call<EntityOverviewResponse>

    @POST("entity/resources")
    fun entityResources(@Body entityResourcesRequest: EntityResourcesRequest): Call<EntityResourcesResponse>

    @POST("transaction/committed-details")
    fun transactionCommittedDetails(
        @Body transactionCommittedDetailsRequest: TransactionCommittedDetailsRequest
    ): Call<TransactionCommittedDetailsResponse>

    @POST("transaction/construction")
    fun transactionConstruction(): Call<TransactionConstructionResponse>

    @POST("transaction/recent")
    fun transactionRecent(
        @Body recentTransactionsRequest: TransactionRecentRequest
    ): Call<TransactionRecentResponse>

    @POST("transaction/submit")
    fun submitTransaction(
        @Body transactionSubmitRequest: TransactionSubmitRequest
    ): Call<TransactionSubmitResponse>

    @POST("transaction/status")
    fun transactionStatus(
        @Body transactionStatusRequest: TransactionStatusRequest
    ): Call<TransactionStatusResponse>

    @POST("transaction/preview")
    fun transactionPreview(@Body body: Any): Call<Any>
}
