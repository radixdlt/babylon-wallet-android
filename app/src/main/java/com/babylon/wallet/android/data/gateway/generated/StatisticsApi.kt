package com.babylon.wallet.android.data.gateway.generated.apis

import com.babylon.wallet.android.data.gateway.generated.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

import com.babylon.wallet.android.data.gateway.generated.models.ErrorResponse
import com.babylon.wallet.android.data.gateway.generated.models.ValidatorsUptimeRequest
import com.babylon.wallet.android.data.gateway.generated.models.ValidatorsUptimeResponse

interface StatisticsApi {
    /**
     * Get Validators Uptime
     * 
     * Responses:
     *  - 200: Validators Uptime
     *  - 4XX: Client-originated request error
     *
     * @param validatorsUptimeRequest 
     * @return [ValidatorsUptimeResponse]
     */
    @POST("statistics/validators/uptime")
    suspend fun validatorsUptime(@Body validatorsUptimeRequest: ValidatorsUptimeRequest): Response<ValidatorsUptimeResponse>

}
