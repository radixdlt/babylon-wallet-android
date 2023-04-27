package com.babylon.wallet.android.data.gateway.apis

import com.babylon.wallet.android.data.gateway.generated.models.GatewayStatusResponse
import retrofit2.Call
import retrofit2.http.POST

interface StatusApi {

    /**
     * Get Gateway Status
     * Returns the Gateway API version and current ledger state.
     * Responses:
     *  - 200: Network Gateway Information
     *
     * @return [GatewayStatusResponse]
     */
    @POST("status/gateway-status")
    fun gatewayStatus(): Call<GatewayStatusResponse>
}
