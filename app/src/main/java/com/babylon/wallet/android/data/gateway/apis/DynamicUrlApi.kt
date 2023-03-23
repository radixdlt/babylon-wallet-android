package com.babylon.wallet.android.data.gateway.apis

import com.babylon.wallet.android.data.gateway.model.DappMetadataResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Url

interface DynamicUrlApi {

    @GET
    fun wellKnownDappDefinition(@Url dappWellKnownUrl: String): Call<DappMetadataResponse>
}
