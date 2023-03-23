@file:Suppress("TooManyFunctions")

package com.babylon.wallet.android.data.gateway.apis

import com.babylon.wallet.android.data.gateway.generated.models.*
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface TransactionApi {

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
