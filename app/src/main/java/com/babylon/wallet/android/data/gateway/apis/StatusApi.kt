package com.babylon.wallet.android.data.gateway.apis

import com.babylon.wallet.android.data.gateway.generated.models.GatewayStatusResponse
import com.babylon.wallet.android.data.gateway.generated.models.NetworkConfigurationResponse
import retrofit2.Response
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
    suspend fun gatewayStatus(): Response<GatewayStatusResponse>

    /**
     * Get Network Configuration
     * Returns network identifier, network name and well-known network addresses.
     * Responses:
     *  - 200: Network Configuration
     *
     * @return [NetworkConfigurationResponse]
     */
    @POST("status/network-configuration")
    suspend fun networkConfiguration(): Response<NetworkConfigurationResponse>
}
