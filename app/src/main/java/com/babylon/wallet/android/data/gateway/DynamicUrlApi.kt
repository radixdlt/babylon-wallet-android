package com.babylon.wallet.android.data.gateway

import com.babylon.wallet.android.data.gateway.generated.models.GatewayStatusResponse
import com.babylon.wallet.android.data.gateway.model.DappMetadataResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Url

interface DynamicUrlApi {

    @POST
    fun gatewayStatus(@Url gatewayUrl: String): Call<GatewayStatusResponse>

    @GET
    fun wellKnownDappDefinition(@Url dappWellKnownUrl: String): Call<DappMetadataResponse>

    companion object {
        private const val GATEWAY_STATUS_API = "status/gateway-status"

        fun gatewayStatusUrl(fromBaseUrl: String) = "$fromBaseUrl/$GATEWAY_STATUS_API"
    }
}
