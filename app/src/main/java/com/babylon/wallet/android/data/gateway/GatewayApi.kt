@file:Suppress("TooManyFunctions")

package com.babylon.wallet.android.data.gateway

import com.babylon.wallet.android.data.gateway.generated.models.*
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface GatewayApi {

    @POST("state/entity/details")
    fun stateEntityDetails(@Body stateEntityDetailsRequest: StateEntityDetailsRequest): Call<StateEntityDetailsResponse>

    @POST("state/non-fungible/ids")
    fun stateNonFungibleIds(@Body stateNonFungibleIdsRequest: StateNonFungibleIdsRequest): Call<StateNonFungibleIdsResponse>

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
}
