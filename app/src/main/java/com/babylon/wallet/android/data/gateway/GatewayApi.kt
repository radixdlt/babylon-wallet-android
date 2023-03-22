@file:Suppress("TooManyFunctions")

package com.babylon.wallet.android.data.gateway

import com.babylon.wallet.android.data.gateway.generated.models.EntityDetailsRequest
import com.babylon.wallet.android.data.gateway.generated.models.EntityDetailsResponse
import com.babylon.wallet.android.data.gateway.generated.models.EntityFungiblesRequest
import com.babylon.wallet.android.data.gateway.generated.models.EntityFungiblesResponse
import com.babylon.wallet.android.data.gateway.generated.models.EntityMetadataRequest
import com.babylon.wallet.android.data.gateway.generated.models.EntityMetadataResponse
import com.babylon.wallet.android.data.gateway.generated.models.EntityNonFungibleIdsRequest
import com.babylon.wallet.android.data.gateway.generated.models.EntityNonFungibleIdsResponse
import com.babylon.wallet.android.data.gateway.generated.models.EntityNonFungiblesRequest
import com.babylon.wallet.android.data.gateway.generated.models.EntityNonFungiblesResponse
import com.babylon.wallet.android.data.gateway.generated.models.EntityOverviewRequest
import com.babylon.wallet.android.data.gateway.generated.models.EntityOverviewResponse
import com.babylon.wallet.android.data.gateway.generated.models.EntityResourcesRequest
import com.babylon.wallet.android.data.gateway.generated.models.EntityResourcesResponse
import com.babylon.wallet.android.data.gateway.generated.models.NonFungibleDataRequest
import com.babylon.wallet.android.data.gateway.generated.models.NonFungibleDataResponse
import com.babylon.wallet.android.data.gateway.generated.models.NonFungibleIdsRequest
import com.babylon.wallet.android.data.gateway.generated.models.NonFungibleIdsResponse
import com.babylon.wallet.android.data.gateway.generated.models.TransactionCommittedDetailsRequest
import com.babylon.wallet.android.data.gateway.generated.models.TransactionCommittedDetailsResponse
import com.babylon.wallet.android.data.gateway.generated.models.TransactionConstructionResponse
import com.babylon.wallet.android.data.gateway.generated.models.TransactionRecentRequest
import com.babylon.wallet.android.data.gateway.generated.models.TransactionRecentResponse
import com.babylon.wallet.android.data.gateway.generated.models.TransactionStatusRequest
import com.babylon.wallet.android.data.gateway.generated.models.TransactionStatusResponse
import com.babylon.wallet.android.data.gateway.generated.models.TransactionSubmitRequest
import com.babylon.wallet.android.data.gateway.generated.models.TransactionSubmitResponse
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
