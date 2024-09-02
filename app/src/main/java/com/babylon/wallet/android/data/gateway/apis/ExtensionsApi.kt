package com.babylon.wallet.android.data.gateway.apis

import com.babylon.wallet.android.data.gateway.generated.models.ResourceHoldersRequest
import com.babylon.wallet.android.data.gateway.generated.models.ResourceHoldersResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface ExtensionsApi {
    /**
     * Get Resource Holders Page
     * A paginated endpoint to discover which global entities hold the most of a given resource.
     * More specifically, it returns a page of global entities which hold the given resource,
     * ordered descending by the total fungible balance / total count of non-fungibles stored in vaults in the state
     * tree of that entity (excluding unclaimed royalty balances). This endpoint operates only at the **current state version**,
     * it is not possible to browse historical data. Because of that, it is not possible to offer stable pagination
     * as data constantly changes. Balances might change between pages being read, which might result in gaps or some entries
     * being returned twice. Under default Gateway configuration, up to 100 entries are returned per response.
     * This can be increased up to 1000 entries per page with the &#x60;limit_per_page&#x60; parameter.
     * Responses:
     *  - 200: Resource holders
     *  - 4XX: Client-originated request error
     *
     * @param resourceHoldersRequest
     * @return [ResourceHoldersResponse]
     */
    @POST("extensions/resource-holders/page")
    fun resourceHoldersPage(@Body resourceHoldersRequest: ResourceHoldersRequest): Call<ResourceHoldersResponse>
}
