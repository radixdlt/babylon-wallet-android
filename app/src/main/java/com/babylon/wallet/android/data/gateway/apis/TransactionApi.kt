@file:Suppress("TooManyFunctions")

package com.babylon.wallet.android.data.gateway.apis

import com.babylon.wallet.android.data.gateway.generated.models.TransactionConstructionResponse
import com.babylon.wallet.android.data.gateway.generated.models.TransactionPreviewRequest
import com.babylon.wallet.android.data.gateway.generated.models.TransactionPreviewResponse
import com.babylon.wallet.android.data.gateway.generated.models.TransactionRecentRequest
import com.babylon.wallet.android.data.gateway.generated.models.TransactionRecentResponse
import com.babylon.wallet.android.data.gateway.generated.models.TransactionStatusRequest
import com.babylon.wallet.android.data.gateway.generated.models.TransactionStatusResponse
import com.babylon.wallet.android.data.gateway.generated.models.TransactionSubmitRequest
import com.babylon.wallet.android.data.gateway.generated.models.TransactionSubmitResponse
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

    @POST("transaction/preview")
    fun transactionPreview(
        @Body transactionPreviewRequest: TransactionPreviewRequest
    ): Call<TransactionPreviewResponse>
}
