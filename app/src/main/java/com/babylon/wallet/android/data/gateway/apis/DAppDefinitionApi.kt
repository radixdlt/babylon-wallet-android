package com.babylon.wallet.android.data.gateway.apis

import com.babylon.wallet.android.BuildConfig
import com.babylon.wallet.android.data.gateway.model.DappMetadataResponse
import retrofit2.Call
import retrofit2.http.GET

interface DAppDefinitionApi {

    @GET(BuildConfig.WELL_KNOWN_URL_SUFFIX)
    fun wellKnownDAppDefinition(): Call<DappMetadataResponse>
}
