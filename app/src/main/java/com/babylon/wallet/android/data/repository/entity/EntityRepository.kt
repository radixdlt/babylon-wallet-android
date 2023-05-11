package com.babylon.wallet.android.data.repository.entity

import com.babylon.wallet.android.data.gateway.apis.StateApi
import com.babylon.wallet.android.data.gateway.extensions.asMetadataItems
import com.babylon.wallet.android.data.gateway.generated.models.FungibleResourcesCollection
import com.babylon.wallet.android.data.gateway.generated.models.FungibleResourcesCollectionItemVaultAggregated
import com.babylon.wallet.android.data.gateway.generated.models.NonFungibleResourcesCollection
import com.babylon.wallet.android.data.gateway.generated.models.NonFungibleResourcesCollectionItemVaultAggregated
import com.babylon.wallet.android.data.gateway.generated.models.ResourceAggregationLevel
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsOptIns
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsRequest
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsResponse
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityFungiblesPageRequest
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityFungiblesPageResponse
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityNonFungiblesPageRequest
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityNonFungiblesPageResponse
import com.babylon.wallet.android.data.gateway.generated.models.StateNonFungibleDataRequest
import com.babylon.wallet.android.data.gateway.generated.models.StateNonFungibleDataResponse
import com.babylon.wallet.android.data.gateway.generated.models.StateNonFungibleIdsRequest
import com.babylon.wallet.android.data.gateway.model.ExplicitMetadataKey
import com.babylon.wallet.android.data.repository.cache.CacheParameters
import com.babylon.wallet.android.data.repository.cache.HttpCache
import com.babylon.wallet.android.data.repository.cache.TimeoutDuration
import com.babylon.wallet.android.data.repository.cache.TimeoutDuration.NO_CACHE
import com.babylon.wallet.android.data.repository.execute
import com.babylon.wallet.android.domain.common.Result
import com.babylon.wallet.android.domain.common.map
import com.babylon.wallet.android.domain.common.switchMap
import com.babylon.wallet.android.domain.common.value
import com.babylon.wallet.android.domain.model.AccountWithResources
import com.babylon.wallet.android.domain.model.NonFungibleTokenIdContainer
import com.babylon.wallet.android.domain.model.Resource
import com.babylon.wallet.android.domain.model.Resources
import com.babylon.wallet.android.domain.model.metadata.MetadataItem.Companion.consume
import rdx.works.profile.data.model.pernetwork.Network
import javax.inject.Inject

interface EntityRepository {

    suspend fun getAccountsWithResources(
        accounts: List<Network.Account>,
        // we pass a combination of fungible AND non fungible explicit metadata keys
        explicitMetadataForAssets: Set<ExplicitMetadataKey> = ExplicitMetadataKey.forAssets,
        isRefreshing: Boolean = true
    ): Result<List<AccountWithResources>>

    suspend fun stateEntityDetails(
        addresses: List<String>,
        isRefreshing: Boolean = true
    ): Result<StateEntityDetailsResponse>

    suspend fun nonFungibleData(
        address: String,
        nonFungibleIds: List<String>,
        page: String? = null,
        limit: Int? = null,
        isRefreshing: Boolean
    ): Result<StateNonFungibleDataResponse>

    suspend fun getNonFungibleIds(
        address: String,
        page: String? = null,
        limit: Int? = null,
        isRefreshing: Boolean
    ): Result<NonFungibleTokenIdContainer>
}

class EntityRepositoryImpl @Inject constructor(
    private val stateApi: StateApi,
    private val cache: HttpCache
) : EntityRepository {

    override suspend fun getAccountsWithResources(
        accounts: List<Network.Account>,
        explicitMetadataForAssets: Set<ExplicitMetadataKey>,
        isRefreshing: Boolean
    ): Result<List<AccountWithResources>> {
        val listOfEntityDetailsResponsesResult = getStateEntityDetailsResponse(
            addresses = accounts.map { it.address },
            explicitMetadata = explicitMetadataForAssets,
            isRefreshing = isRefreshing
        )

        return listOfEntityDetailsResponsesResult.switchMap { entityDetailsResponses ->

            val mapOfAccountsWithFungibleResources = buildMapOfAccountsWithFungibles(entityDetailsResponses)

            val mapOfAccountsWithNonFungibleResources = buildMapOfAccountsWithNonFungibles(entityDetailsResponses)

            // build result list of accounts with resources
            val listOfAccountsWithResources = accounts.map { account ->
                AccountWithResources(
                    account = account,
                    resources = Resources(
                        fungibleResources = mapOfAccountsWithFungibleResources[account.address].orEmpty(),
                        nonFungibleResources = mapOfAccountsWithNonFungibleResources[account.address].orEmpty()
                    )
                )
            }

            Result.Success(listOfAccountsWithResources)
        }
    }

    private suspend fun buildMapOfAccountsWithFungibles(
        entityDetailsResponses: List<StateEntityDetailsResponse>
    ): Map<String, List<Resource.FungibleResource>> {
        return entityDetailsResponses.map { entityDetailsResponse ->
            entityDetailsResponse.items
                .groupingBy { entityDetailsResponseItem ->
                    entityDetailsResponseItem.address
                }
                .foldTo(mutableMapOf(), listOf<Resource.FungibleResource>()) { _, entityItem ->
                    val fungibleResourcesItemsList = if (entityItem.fungibleResources != null) {
                        getFungibleResourcesCollectionItemsForAccount(
                            accountAddress = entityItem.address,
                            fungibleResources = entityItem.fungibleResources
                        )
                    } else {
                        emptyList()
                    }
                    fungibleResourcesItemsList.map { fungibleResourcesItem ->
                        val metaDataItems = fungibleResourcesItem.explicitMetadata?.asMetadataItems().orEmpty()
                        Resource.FungibleResource(
                            resourceAddress = fungibleResourcesItem.resourceAddress,
                            amount = fungibleResourcesItem.vaults.items.first().amount.toBigDecimal(),
                            nameMetadataItem = metaDataItems.toMutableList().consume(),
                            symbolMetadataItem = metaDataItems.toMutableList().consume(),
                            descriptionMetadataItem = metaDataItems.toMutableList().consume(),
                            iconUrlMetadataItem = metaDataItems.toMutableList().consume()
                        )
                    }
                }
        }.flatMap { map ->
            map.asSequence()
        }.associate { map ->
            map.key to map.value
        }
    }

    private suspend fun buildMapOfAccountsWithNonFungibles(
        entityDetailsResponses: List<StateEntityDetailsResponse>
    ): Map<String, List<Resource.NonFungibleResource>> {
        return entityDetailsResponses.map { entityDetailsResponse ->
            entityDetailsResponse.items
                .groupingBy { entityDetailsResponseItem ->
                    entityDetailsResponseItem.address
                }
                .foldTo(mutableMapOf(), listOf<Resource.NonFungibleResource>()) { _, entityItem ->
                    val nonFungibleResourcesItemsList = if (entityItem.nonFungibleResources != null) {
                        getNonFungibleResourcesCollectionItemsForAccount(
                            accountAddress = entityItem.address,
                            nonFungibleResources = entityItem.nonFungibleResources
                        )
                    } else {
                        emptyList()
                    }
                    nonFungibleResourcesItemsList.map { nonFungibleResourcesItem ->
                        val metaDataItems = nonFungibleResourcesItem.explicitMetadata?.asMetadataItems().orEmpty()
                        Resource.NonFungibleResource(
                            resourceAddress = nonFungibleResourcesItem.resourceAddress,
                            amount = nonFungibleResourcesItem.vaults.items.first().totalCount,
                            nameMetadataItem = metaDataItems.toMutableList().consume(),
                            descriptionMetadataItem = metaDataItems.toMutableList().consume(),
                            nftIds = emptyList()
                        )
                    }
                }
        }.flatMap { map ->
            map.asSequence()
        }.associate { map ->
            map.key to map.value
        }
    }

    private suspend fun getStateEntityDetailsResponse(
        addresses: List<String>,
        explicitMetadata: Set<ExplicitMetadataKey>,
        isRefreshing: Boolean
    ): Result<List<StateEntityDetailsResponse>> {
        val responses = addresses
            .chunked(CHUNK_SIZE_OF_ADDRESSES)
            .map { chunkedAddresses ->
                stateApi.entityDetails( // TODO use stateEntityDetails if possible
                    StateEntityDetailsRequest(
                        addresses = chunkedAddresses,
                        aggregationLevel = ResourceAggregationLevel.vault,
                        optIns = StateEntityDetailsOptIns(
                            explicitMetadata = explicitMetadata.map { it.key }
                        )
                    )
                ).execute(
                    cacheParameters = CacheParameters(
                        httpCache = cache,
                        timeoutDuration = if (isRefreshing) NO_CACHE else TimeoutDuration.ONE_MINUTE
                    ),
                    map = {
                        it
                    }
                )
            }

        // if you find any error response in the list of StateEntityDetailsResponses then return error
        return if (responses.any { response -> response is Result.Error }) {
            val errorResponse = responses.first { response -> response is Result.Error }.map {
                listOf(it)
            }
            errorResponse
        } else { // otherwise all StateEntityDetailsResponses are success so return the list
            Result.Success(responses.map { it.value()!! })
        }
    }

    private suspend fun getFungibleResourcesCollectionItemsForAccount(
        accountAddress: String,
        fungibleResources: FungibleResourcesCollection
    ): List<FungibleResourcesCollectionItemVaultAggregated> {
        val allFungibles: MutableList<FungibleResourcesCollectionItemVaultAggregated> = mutableListOf()
        allFungibles.addAll(
            fungibleResources.items.map {
                it as FungibleResourcesCollectionItemVaultAggregated
            }
        )

        var nextCursor = fungibleResources.nextCursor
        while (nextCursor != null) {
            val stateEntityFungiblesPageResponse = nextFungiblesPage(
                accountAddress = accountAddress,
                nextCursor = nextCursor
            )
            stateEntityFungiblesPageResponse.map {
                allFungibles.addAll(
                    it.items.map { fungibleResourcesCollectionItem ->
                        fungibleResourcesCollectionItem as FungibleResourcesCollectionItemVaultAggregated
                    }
                )
                nextCursor = it.nextCursor
            }
        }
        return allFungibles
    }

    private suspend fun getNonFungibleResourcesCollectionItemsForAccount(
        accountAddress: String,
        nonFungibleResources: NonFungibleResourcesCollection
    ): List<NonFungibleResourcesCollectionItemVaultAggregated> {
        val allNonFungibles: MutableList<NonFungibleResourcesCollectionItemVaultAggregated> = mutableListOf()
        allNonFungibles.addAll(
            nonFungibleResources.items.map {
                it as NonFungibleResourcesCollectionItemVaultAggregated
            }
        )
        var nextCursor = nonFungibleResources.nextCursor
        while (nextCursor != null) {
            val stateEntityFungiblesPageResponse = nextNonFungiblesPage(
                accountAddress = accountAddress,
                nextCursor = nextCursor
            )
            stateEntityFungiblesPageResponse.map {
                allNonFungibles.addAll(
                    it.items.map { nonFungibleResourcesCollectionItem ->
                        nonFungibleResourcesCollectionItem as NonFungibleResourcesCollectionItemVaultAggregated
                    }
                )
                nextCursor = it.nextCursor
            }
        }
        return allNonFungibles
    }

    // TODO currently this is only used in GetTransactionComponentResourcesUseCase,
    //  we should better hide this function by wrapping it in another more meaningful function,
    //  for example: getTypeOfAddress() in case of the GetTransactionComponentResourcesUseCase
    override suspend fun stateEntityDetails(
        addresses: List<String>,
        isRefreshing: Boolean
    ): Result<StateEntityDetailsResponse> {
        return stateApi.entityDetails(
            StateEntityDetailsRequest(
                addresses = addresses,
                aggregationLevel = ResourceAggregationLevel.vault
            )
        ).execute(
            cacheParameters = CacheParameters(
                httpCache = cache,
                timeoutDuration = if (isRefreshing) NO_CACHE else TimeoutDuration.ONE_MINUTE
            ),
            map = { it }
        )
    }

    override suspend fun getNonFungibleIds(
        address: String,
        page: String?,
        limit: Int?,
        isRefreshing: Boolean
    ): Result<NonFungibleTokenIdContainer> {
        return stateApi.nonFungibleIds(StateNonFungibleIdsRequest(address)).execute(
            cacheParameters = CacheParameters(
                httpCache = cache,
                timeoutDuration = if (isRefreshing) NO_CACHE else TimeoutDuration.FIVE_MINUTES
            ),
            map = { response ->
                NonFungibleTokenIdContainer(
                    ids = response.nonFungibleIds.items.map { it.nonFungibleId },
                    nextCursor = response.nonFungibleIds.nextCursor,
                    previousCursor = response.nonFungibleIds.previousCursor
                )
            }
        )
    }

    private suspend fun nextFungiblesPage(
        accountAddress: String,
        nextCursor: String
    ): Result<StateEntityFungiblesPageResponse> {
        return stateApi.entityFungiblesPage(
            stateEntityFungiblesPageRequest = StateEntityFungiblesPageRequest(
                address = accountAddress,
                cursor = nextCursor,
                aggregationLevel = ResourceAggregationLevel.vault,

            )
        ).execute(
            cacheParameters = CacheParameters(
                httpCache = cache,
                timeoutDuration = NO_CACHE
            ),
            map = { it }
        )
    }

    private suspend fun nextNonFungiblesPage(
        accountAddress: String,
        nextCursor: String
    ): Result<StateEntityNonFungiblesPageResponse> {
        return stateApi.entityNonFungiblesPage(
            stateEntityNonFungiblesPageRequest = StateEntityNonFungiblesPageRequest(
                address = accountAddress,
                cursor = nextCursor,
                aggregationLevel = ResourceAggregationLevel.vault,

            )
        ).execute(
            cacheParameters = CacheParameters(
                httpCache = cache,
                timeoutDuration = NO_CACHE
            ),
            map = { it }
        )
    }

    override suspend fun nonFungibleData(
        address: String,
        nonFungibleIds: List<String>,
        page: String?,
        limit: Int?,
        isRefreshing: Boolean
    ): Result<StateNonFungibleDataResponse> {
        return stateApi.nonFungibleData(
            StateNonFungibleDataRequest(
                resourceAddress = address,
                nonFungibleIds = nonFungibleIds
            )
        ).execute(
            cacheParameters = CacheParameters(
                httpCache = cache,
                timeoutDuration = if (isRefreshing) NO_CACHE else TimeoutDuration.ONE_MINUTE
            ),
            map = { it }
        )
    }

    companion object {
        private const val CHUNK_SIZE_OF_ADDRESSES = 20
    }
}
