package com.babylon.wallet.android.data.gateway.apis

import com.babylon.wallet.android.data.gateway.generated.models.ValidatorsUptimeRequest
import com.babylon.wallet.android.data.gateway.generated.models.ValidatorsUptimeResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface StatisticsApi {
    /**
     * Get Validators Uptime
     * Returns validators uptime data for time range limited by &#x60;from_state_version&#x60; and &#x60;at_state_version&#x60;.
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
