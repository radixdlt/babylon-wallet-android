package com.babylon.wallet.android.data.gateway.apis

import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsRequest
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsResponse
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityFungiblesPageRequest
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityFungiblesPageResponse
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityMetadataPageRequest
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityMetadataPageResponse
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityNonFungiblesPageRequest
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityNonFungiblesPageResponse
import com.babylon.wallet.android.data.gateway.generated.models.StateNonFungibleDataRequest
import com.babylon.wallet.android.data.gateway.generated.models.StateNonFungibleDataResponse
import com.babylon.wallet.android.data.gateway.generated.models.StateNonFungibleIdsRequest
import com.babylon.wallet.android.data.gateway.generated.models.StateNonFungibleIdsResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface StateApi {

    /**
     * Get Entity Details
     * Returns detailed information for collection of entities. Aggregate resources globally by default.
     * Responses:
     *  - 200: Entity Details
     *  - 4XX: Client-originated request error
     *
     * @param stateEntityDetailsRequest
     * @return [StateEntityDetailsResponse]
     */
    @POST("state/entity/details")
    fun entityDetails(
        @Body stateEntityDetailsRequest: StateEntityDetailsRequest
    ): Call<StateEntityDetailsResponse>

    /**
     * Get Entity Metadata Page
     * Returns all the metadata properties associated with a given global entity.
     * The returned response is in a paginated format, ordered by first appearance on the ledger.
     * Responses:
     *  - 200: Entity Metadata (paginated)
     *  - 4XX: Client-originated request error
     *
     * @param stateEntityMetadataPageRequest
     * @return [StateEntityMetadataPageResponse]
     */
    @POST("state/entity/page/metadata")
    fun entityMetadataPage(
        @Body stateEntityMetadataPageRequest: StateEntityMetadataPageRequest
    ): Call<StateEntityMetadataPageResponse>

    /**
     * Get Entity Fungible Resource Totals Page aggregated globally
     * Returns the total amount of each fungible resource owned by a given global entity.
     * Result can be aggregated globally or per vault.
     * The returned response is in a paginated format, ordered by the resource&#39;s first appearance on the ledger.
     * Responses:
     *  - 200: Entity Fungibles (paginated)
     *  - 4XX: Client-originated request error
     *
     * @param stateEntityFungiblesPageRequest
     * @return [StateEntityFungiblesPageResponse]
     */
    @POST("state/entity/page/fungibles")
    fun entityFungiblesPage(
        @Body stateEntityFungiblesPageRequest: StateEntityFungiblesPageRequest
    ): Call<StateEntityFungiblesPageResponse>

    /**
     * Get Non-Fungible Collection
     * Returns the non-fungible IDs of a given non-fungible resource.
     * Returned response is in a paginated format, ordered by their first appearance on the ledger.
     * Responses:
     *  - 200: Non-Fungible IDs (paginated)
     *  - 4XX: Client-originated request error
     *
     * @param stateNonFungibleIdsRequest
     * @return [StateNonFungibleIdsResponse]
     */
    @POST("state/non-fungible/ids") // TODO not the right one ?
    fun nonFungibleIds(
        @Body stateNonFungibleIdsRequest: StateNonFungibleIdsRequest
    ): Call<StateNonFungibleIdsResponse>

    /**
     * Get Entity Non-Fungible Resource Totals Page aggregated globally
     * Returns the total amount of each non-fungible resource owned by a given global entity.
     * Result can be aggregated globally or per vault.
     * The returned response is in a paginated format, ordered by the resource&#39;s first appearance on the ledger.
     * Responses:
     *  - 200: Entity Non-Fungibles (paginated)
     *  - 4XX: Client-originated request error
     *
     * @param stateEntityNonFungiblesPageRequest
     * @return [StateEntityNonFungiblesPageResponse]
     */
    @POST("state/entity/page/non-fungibles")
    fun nonFungiblesPage(
        @Body stateEntityNonFungiblesPageRequest: StateEntityNonFungiblesPageRequest
    ): Call<StateEntityNonFungiblesPageResponse>

    @POST("state/non-fungible/data") // TODO
    fun nonFungibleData(
        @Body stateNonFungibleDataRequest: StateNonFungibleDataRequest
    ): Call<StateNonFungibleDataResponse>
}
