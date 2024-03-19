package com.babylon.wallet.android.data.gateway.apis

import com.babylon.wallet.android.data.gateway.model.TokenPriceResponse
import com.babylon.wallet.android.data.gateway.model.TokensAndLsusPricesRequest
import com.babylon.wallet.android.data.gateway.model.TokensAndLsusPricesResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface TokenPriceApi {

    @POST("price/tokens")
    fun priceTokens(@Body tokensAndLsusPricesRequest: TokensAndLsusPricesRequest): Call<TokensAndLsusPricesResponse>

    @POST("tokens")
    fun tokens(): Call<List<TokenPriceResponse>>

    companion object {
        const val BASE_URL = "https://token-price-service.radixdlt.com/"
    }
}
