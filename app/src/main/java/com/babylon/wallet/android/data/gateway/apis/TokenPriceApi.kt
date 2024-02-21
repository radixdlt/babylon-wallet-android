package com.babylon.wallet.android.data.gateway.apis

import com.babylon.wallet.android.data.gateway.model.TokenPriceResponse
import retrofit2.Call
import retrofit2.http.POST

interface TokenPriceApi {

    @POST("tokens/")
    fun tokens(): Call<List<TokenPriceResponse>>
}
