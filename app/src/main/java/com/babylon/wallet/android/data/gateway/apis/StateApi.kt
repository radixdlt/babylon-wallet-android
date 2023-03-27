package com.babylon.wallet.android.data.gateway.apis

import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsRequest
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsResponse
import com.babylon.wallet.android.data.gateway.generated.models.StateNonFungibleIdsRequest
import com.babylon.wallet.android.data.gateway.generated.models.StateNonFungibleIdsResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface StateApi {

    @POST("state/entity/details")
    fun entityDetails(
        @Body stateEntityDetailsRequest: StateEntityDetailsRequest
    ): Call<StateEntityDetailsResponse>

    @POST("state/non-fungible/ids")
    fun nonFungibleIds(
        @Body stateNonFungibleIdsRequest: StateNonFungibleIdsRequest
    ): Call<StateNonFungibleIdsResponse>
}
