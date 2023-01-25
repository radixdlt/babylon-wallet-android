package com.babylon.wallet.android.data.gateway

import com.babylon.wallet.android.data.gateway.generated.model.GatewayInfoResponse
import com.babylon.wallet.android.data.gateway.model.DappMetadataDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Url

interface DynamicUrlApi {

    @POST
    suspend fun gatewayInfo(@Url gatewayUrl: String): Response<GatewayInfoResponse>

    @GET
    suspend fun wellKnownDappDefinition(@Url dappWellKnownUrl: String): Response<List<DappMetadataDto>>
}
