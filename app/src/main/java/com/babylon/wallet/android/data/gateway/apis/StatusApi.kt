package com.babylon.wallet.android.data.gateway.apis

import com.babylon.wallet.android.data.gateway.generated.models.GatewayStatusResponse
import retrofit2.Call
import retrofit2.http.POST
import retrofit2.http.Url

interface StatusApi {

    @POST
    fun gatewayStatus(@Url gatewayUrl: String): Call<GatewayStatusResponse>

    companion object {
        private const val GATEWAY_STATUS_API = "status/gateway-status"

        fun gatewayStatusUrl(fromBaseUrl: String) = "$fromBaseUrl/$GATEWAY_STATUS_API"
    }
}
