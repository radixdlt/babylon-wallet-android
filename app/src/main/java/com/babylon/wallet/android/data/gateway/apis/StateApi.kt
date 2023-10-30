package com.babylon.wallet.android.data.gateway.apis

import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsRequest
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsResponse
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityFungibleResourceVaultsPageRequest
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityFungibleResourceVaultsPageResponse
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityFungiblesPageRequest
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityFungiblesPageResponse
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityMetadataPageRequest
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityMetadataPageResponse
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityNonFungibleIdsPageRequest
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityNonFungibleIdsPageResponse
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityNonFungibleResourceVaultsPageRequest
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityNonFungibleResourceVaultsPageResponse
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityNonFungiblesPageRequest
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityNonFungiblesPageResponse
import com.babylon.wallet.android.data.gateway.generated.models.StateKeyValueStoreDataRequest
import com.babylon.wallet.android.data.gateway.generated.models.StateKeyValueStoreDataResponse
import com.babylon.wallet.android.data.gateway.generated.models.StateNonFungibleDataRequest
import com.babylon.wallet.android.data.gateway.generated.models.StateNonFungibleDataResponse
import com.babylon.wallet.android.data.gateway.generated.models.StateNonFungibleIdsRequest
import com.babylon.wallet.android.data.gateway.generated.models.StateNonFungibleIdsResponse
import com.babylon.wallet.android.data.gateway.generated.models.StateNonFungibleLocationRequest
import com.babylon.wallet.android.data.gateway.generated.models.StateNonFungibleLocationResponse
import com.babylon.wallet.android.data.gateway.generated.models.StateValidatorsListRequest
import com.babylon.wallet.android.data.gateway.generated.models.StateValidatorsListResponse
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface StateApi {

    /**
     * Get vault page of Entity Fungible resource aggregated per vault
     * Returns vaults for fungible resource owned by a given global entity. The returned response is in a paginated format, ordered by the resource&#39;s first appearance on the ledger.
     * Responses:
     *  - 200: Entity Fungibles (paginated)
     *  - 4XX: Client-originated request error
     *
     * @param stateEntityFungibleResourceVaultsPageRequest
     * @return [StateEntityFungibleResourceVaultsPageResponse]
     */
    @POST("state/entity/page/fungible-vaults/")
    fun entityFungibleResourceVaultPage(
        @Body stateEntityFungibleResourceVaultsPageRequest: StateEntityFungibleResourceVaultsPageRequest
    ): Call<StateEntityFungibleResourceVaultsPageResponse>

    /**
     * Get Entity Fungible Resource Totals Page aggregated globally
     * Returns the total amount of each fungible resource owned by a given global entity. Result can be aggregated globally or per vault. The returned response is in a paginated format, ordered by the resource&#39;s first appearance on the ledger.
     * Responses:
     *  - 200: Entity Fungibles (paginated)
     *  - 4XX: Client-originated request error
     *
     * @param stateEntityFungiblesPageRequest
     * @return [StateEntityFungiblesPageResponse]
     */
    @POST("state/entity/page/fungibles/")
    fun entityFungiblesPage(
        @Body stateEntityFungiblesPageRequest: StateEntityFungiblesPageRequest
    ): Call<StateEntityFungiblesPageResponse>

    /**
     * Get Entity Metadata Page
     * Returns all the metadata properties associated with a given global entity. The returned response is in a paginated format, ordered by first appearance on the ledger.
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
     * Get Entity Non-Fungible IDs
     * Returns all non-fungible IDs of a given non-fungible resource owned by a given entity. The returned response is in a paginated format, ordered by the resource&#39;s first appearence on the ledger.
     * Responses:
     *  - 200: Entity Non-Fungible IDs (paginated)
     *  - 4XX: Client-originated request error
     *
     * @param stateEntityNonFungibleIdsPageRequest
     * @return [StateEntityNonFungibleIdsPageResponse]
     */
    @POST("state/entity/page/non-fungible-vault/ids")
    fun entityNonFungibleIdsPage(
        @Body stateEntityNonFungibleIdsPageRequest: StateEntityNonFungibleIdsPageRequest
    ): Call<StateEntityNonFungibleIdsPageResponse>

    /**
     * Get vault page of Entity Non Fungible aggregated per vault
     * Returns vaults for non fungible resource owned by a given global entity. The returned response is in a paginated format, ordered by the resource&#39;s first appearance on the ledger.
     * Responses:
     *  - 200: Entity Fungibles (paginated)
     *  - 4XX: Client-originated request error
     *
     * @param stateEntityNonFungibleResourceVaultsPageRequest
     * @return [StateEntityNonFungibleResourceVaultsPageResponse]
     */
    @POST("state/entity/page/non-fungible-vaults/")
    fun entityNonFungibleResourceVaultPage(
        @Body stateEntityNonFungibleResourceVaultsPageRequest: StateEntityNonFungibleResourceVaultsPageRequest
    ): Call<StateEntityNonFungibleResourceVaultsPageResponse>

    /**
     * Get Entity Non-Fungible Resource Totals Page aggregated globally
     * Returns the total amount of each non-fungible resource owned by a given global entity. Result can be aggregated globally or per vault. The returned response is in a paginated format, ordered by the resource&#39;s first appearance on the ledger.
     * Responses:
     *  - 200: Entity Non-Fungibles (paginated)
     *  - 4XX: Client-originated request error
     *
     * @param stateEntityNonFungiblesPageRequest
     * @return [StateEntityNonFungiblesPageResponse]
     */
    @POST("state/entity/page/non-fungibles/")
    fun entityNonFungiblesPage(
        @Body stateEntityNonFungiblesPageRequest: StateEntityNonFungiblesPageRequest
    ): Call<StateEntityNonFungiblesPageResponse>

    /**
     * Get KeyValueStore Data
     * Returns data (value) associated with a given key of a given key-value store.
     * Responses:
     *  - 200: Non-Fungible ID Data
     *  - 4XX: Client-originated request error
     *
     * @param stateKeyValueStoreDataRequest
     * @return [StateKeyValueStoreDataResponse]
     */
    @POST("state/key-value-store/data")
    fun keyValueStoreData(
        @Body stateKeyValueStoreDataRequest: StateKeyValueStoreDataRequest
    ): Call<StateKeyValueStoreDataResponse>

    /**
     * Get Non-Fungible Data
     * Returns data associated with a given non-fungible ID of a given non-fungible resource.
     * Responses:
     *  - 200: Non-Fungible ID Data
     *  - 4XX: Client-originated request error
     *
     * @param stateNonFungibleDataRequest
     * @return [StateNonFungibleDataResponse]
     */
    @POST("state/non-fungible/data")
    fun nonFungibleData(
        @Body stateNonFungibleDataRequest: StateNonFungibleDataRequest
    ): Call<StateNonFungibleDataResponse>

    /**
     * Get Non-Fungible Collection
     * Returns the non-fungible IDs of a given non-fungible resource. Returned response is in a paginated format, ordered by their first appearance on the ledger.
     * Responses:
     *  - 200: Non-Fungible IDs (paginated)
     *  - 4XX: Client-originated request error
     *
     * @param stateNonFungibleIdsRequest
     * @return [StateNonFungibleIdsResponse]
     */
    @POST("state/non-fungible/ids")
    fun nonFungibleIds(
        @Body stateNonFungibleIdsRequest: StateNonFungibleIdsRequest
    ): Call<StateNonFungibleIdsResponse>

    /**
     * Get Non-Fungible Location
     * Returns location of a given non-fungible ID.
     * Responses:
     *  - 200: Non-Fungible ID Location
     *  - 4XX: Client-originated request error
     *
     * @param stateNonFungibleLocationRequest
     * @return [StateNonFungibleLocationResponse]
     */
    @POST("state/non-fungible/location")
    fun nonFungibleLocation(
        @Body stateNonFungibleLocationRequest: StateNonFungibleLocationRequest
    ): Call<StateNonFungibleLocationResponse>

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
    fun stateEntityDetails(
        @Body stateEntityDetailsRequest: StateEntityDetailsRequest
    ): Call<StateEntityDetailsResponse>

    /**
     * Get Validators List
     *
     * Responses:
     *  - 200: Validators List
     *  - 4XX: Client-originated request error
     *
     * @param stateValidatorsListRequest
     * @return [StateValidatorsListResponse]
     */
    @POST("state/validators/list")
    fun stateValidatorsList(
        @Body stateValidatorsListRequest: StateValidatorsListRequest
    ): Call<StateValidatorsListResponse>
}
