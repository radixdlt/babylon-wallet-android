package com.babylon.wallet.android.data.gateway.apis

import com.babylon.wallet.android.data.gateway.model.RcrHandshakeRequest
import com.babylon.wallet.android.data.gateway.model.RcrHandshakeResponse
import com.babylon.wallet.android.data.gateway.model.RcrRequest
import com.babylon.wallet.android.data.gateway.model.RcrResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface RcrApi {
    @POST("api/v1")
    fun executeRequest(@Body request: RcrRequest): Call<RcrResponse>

    @POST("api/v1")
    fun executeHandshakeRequest(@Body request: RcrHandshakeRequest): Call<RcrHandshakeResponse>
}