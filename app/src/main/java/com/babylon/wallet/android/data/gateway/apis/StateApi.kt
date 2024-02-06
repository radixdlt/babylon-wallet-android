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
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

@Suppress("TooManyFunctions")
interface StateApi {
    /**
     * Get page of Global Entity Fungible Resource Vaults
     * Returns vaults for fungible resource owned by a given global entity. The returned response is in a paginated format,
     * ordered by the resource&#39;s first appearance on the ledger.
     * Responses:
     *  - 200: Entity Fungibles (paginated)
     *  - 4XX: Client-originated request error
     *
     * @param stateEntityFungibleResourceVaultsPageRequest
     * @return [StateEntityFungibleResourceVaultsPageResponse]
     */
    @POST("state/entity/page/fungible-vaults/")
    suspend fun entityFungibleResourceVaultPage(
        @Body stateEntityFungibleResourceVaultsPageRequest: StateEntityFungibleResourceVaultsPageRequest
    ): Response<StateEntityFungibleResourceVaultsPageResponse>

    /**
     * Get page of Global Entity Fungible Resource Balances
     * Returns the total amount of each fungible resource owned by a given global entity.
     * Result can be aggregated globally or per vault. The returned response is in a paginated format,
     * ordered by the resource&#39;s first appearance on the ledger.
     * Responses:
     *  - 200: Entity Fungibles (paginated)
     *  - 4XX: Client-originated request error
     *
     * @param stateEntityFungiblesPageRequest
     * @return [StateEntityFungiblesPageResponse]
     */
    @POST("state/entity/page/fungibles/")
    suspend fun entityFungiblesPage(
        @Body stateEntityFungiblesPageRequest: StateEntityFungiblesPageRequest
    ): Response<StateEntityFungiblesPageResponse>

    /**
     * Get Entity Metadata Page
     * Returns all the metadata properties associated with a given global entity. The returned response is in a paginated format,
     * ordered by first appearance on the ledger.
     * Responses:
     *  - 200: Entity Metadata (paginated)
     *  - 4XX: Client-originated request error
     *
     * @param stateEntityMetadataPageRequest
     * @return [StateEntityMetadataPageResponse]
     */
    @POST("state/entity/page/metadata")
    suspend fun entityMetadataPage(
        @Body stateEntityMetadataPageRequest: StateEntityMetadataPageRequest
    ): Response<StateEntityMetadataPageResponse>

    /**
     * Get page of Non-Fungibles in Vault
     * Returns all non-fungible IDs of a given non-fungible resource owned by a given entity.
     * The returned response is in a paginated format, ordered by the resource&#39;s first appearence on the ledger.
     * Responses:
     *  - 200: Entity Non-Fungible IDs (paginated)
     *  - 4XX: Client-originated request error
     *
     * @param stateEntityNonFungibleIdsPageRequest
     * @return [StateEntityNonFungibleIdsPageResponse]
     */
    @POST("state/entity/page/non-fungible-vault/ids")
    suspend fun entityNonFungibleIdsPage(
        @Body stateEntityNonFungibleIdsPageRequest: StateEntityNonFungibleIdsPageRequest
    ): Response<StateEntityNonFungibleIdsPageResponse>

    /**
     * Get page of Global Entity Non-Fungible Resource Vaults
     * Returns vaults for non fungible resource owned by a given global entity. The returned response is in a paginated format,
     * ordered by the resource&#39;s first appearance on the ledger.
     * Responses:
     *  - 200: Entity Fungibles (paginated)
     *  - 4XX: Client-originated request error
     *
     * @param stateEntityNonFungibleResourceVaultsPageRequest
     * @return [StateEntityNonFungibleResourceVaultsPageResponse]
     */
    @POST("state/entity/page/non-fungible-vaults/")
    suspend fun entityNonFungibleResourceVaultPage(
        @Body stateEntityNonFungibleResourceVaultsPageRequest: StateEntityNonFungibleResourceVaultsPageRequest
    ): Response<StateEntityNonFungibleResourceVaultsPageResponse>

    /**
     * Get page of Global Entity Non-Fungible Resource Balances
     * Returns the total amount of each non-fungible resource owned by a given global entity.
     * Result can be aggregated globally or per vault. The returned response is in a paginated format,
     * ordered by the resource&#39;s first appearance on the ledger.
     * Responses:
     *  - 200: Entity Non-Fungibles (paginated)
     *  - 4XX: Client-originated request error
     *
     * @param stateEntityNonFungiblesPageRequest
     * @return [StateEntityNonFungiblesPageResponse]
     */
    @POST("state/entity/page/non-fungibles/")
    suspend fun entityNonFungiblesPage(
        @Body stateEntityNonFungiblesPageRequest: StateEntityNonFungiblesPageRequest
    ): Response<StateEntityNonFungiblesPageResponse>

    /**
     * Get KeyValueStore Data
     * Returns data (value) associated with a given key of a given key-value store.
     * [Check detailed documentation for explanation](#section/How-to-query-the-content-of-a-key-value-store-inside-a-component)
     * Responses:
     *  - 200: Non-Fungible ID Data
     *  - 4XX: Client-originated request error
     *
     * @param stateKeyValueStoreDataRequest
     * @return [StateKeyValueStoreDataResponse]
     */
    @POST("state/key-value-store/data")
    suspend fun keyValueStoreData(
        @Body stateKeyValueStoreDataRequest: StateKeyValueStoreDataRequest
    ): Response<StateKeyValueStoreDataResponse>

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
    suspend fun nonFungibleData(@Body stateNonFungibleDataRequest: StateNonFungibleDataRequest): Response<StateNonFungibleDataResponse>

    /**
     * Get page of Non-Fungible Ids in Resource Collection
     * Returns the non-fungible IDs of a given non-fungible resource. Returned response is in a paginated format,
     * ordered by their first appearance on the ledger.
     * Responses:
     *  - 200: Non-Fungible IDs (paginated)
     *  - 4XX: Client-originated request error
     *
     * @param stateNonFungibleIdsRequest
     * @return [StateNonFungibleIdsResponse]
     */
    @POST("state/non-fungible/ids")
    suspend fun nonFungibleIds(@Body stateNonFungibleIdsRequest: StateNonFungibleIdsRequest): Response<StateNonFungibleIdsResponse>

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
    suspend fun nonFungibleLocation(
        @Body stateNonFungibleLocationRequest: StateNonFungibleLocationRequest
    ): Response<StateNonFungibleLocationResponse>

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
    suspend fun stateEntityDetails(@Body stateEntityDetailsRequest: StateEntityDetailsRequest): Response<StateEntityDetailsResponse>

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
    suspend fun stateValidatorsList(@Body stateValidatorsListRequest: StateValidatorsListRequest): Response<StateValidatorsListResponse>
}
