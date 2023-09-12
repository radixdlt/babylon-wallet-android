package com.babylon.wallet.android.data.gateway.apis

import com.babylon.wallet.android.data.gateway.generated.models.GatewayStatusResponse
import com.babylon.wallet.android.data.gateway.generated.models.NetworkConfigurationResponse
import com.babylon.wallet.android.data.gateway.model.MainnetNetworkStatus
import retrofit2.Call
import retrofit2.http.GET
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

    /**
     * Get Network Configuration
     * Returns network identifier, network name and well-known network addresses.
     * Responses:
     *  - 200: Network Configuration
     *
     * @return [NetworkConfigurationResponse]
     */
    @POST("status/network-configuration")
    fun networkConfiguration(): Call<NetworkConfigurationResponse>

    @GET(".")
    fun mainnetNetworkStatus(): Call<MainnetNetworkStatus>
}
