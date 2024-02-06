package com.babylon.wallet.android.data.gateway.apis

import com.babylon.wallet.android.data.gateway.generated.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

import com.babylon.wallet.android.data.gateway.generated.models.GatewayStatusResponse
import com.babylon.wallet.android.data.gateway.generated.models.NetworkConfigurationResponse

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
