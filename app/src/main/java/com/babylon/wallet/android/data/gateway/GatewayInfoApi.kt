@file:Suppress("TooManyFunctions")
package com.babylon.wallet.android.data.gateway

import com.babylon.wallet.android.data.gateway.generated.model.GatewayInfoResponse
import retrofit2.Response
import retrofit2.http.POST
import retrofit2.http.Url

interface GatewayInfoApi {

    @POST
    suspend fun gatewayInfo(@Url gatewayUrl: String): Response<GatewayInfoResponse>
}
