package com.babylon.wallet.android.data.gateway

import com.babylon.wallet.android.data.gateway.generated.models.GatewayInformationResponse
import com.babylon.wallet.android.data.gateway.model.DappMetadataResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Url

interface DynamicUrlApi {

    @POST
    fun gatewayInfo(@Url gatewayUrl: String): Call<GatewayInformationResponse>

    @GET
    fun wellKnownDappDefinition(@Url dappWellKnownUrl: String): Call<DappMetadataResponse>
}
