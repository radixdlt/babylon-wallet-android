package com.babylon.wallet.android.data.repository.entity

import android.net.Uri
import androidx.core.net.toUri
import com.babylon.wallet.android.data.gateway.apis.StateApi
import com.babylon.wallet.android.data.gateway.extensions.asMetadataItems
import com.babylon.wallet.android.data.gateway.extensions.asMetadataStringMap
import com.babylon.wallet.android.data.gateway.generated.models.FungibleResourcesCollection
import com.babylon.wallet.android.data.gateway.generated.models.FungibleResourcesCollectionItemVaultAggregated
import com.babylon.wallet.android.data.gateway.generated.models.NonFungibleResourcesCollection
import com.babylon.wallet.android.data.gateway.generated.models.NonFungibleResourcesCollectionItemVaultAggregated
import com.babylon.wallet.android.data.gateway.generated.models.ResourceAggregationLevel
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsOptIns
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsRequest
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsResponse
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsResponseItemDetailsType
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityFungiblesPageRequest
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityFungiblesPageResponse
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityNonFungibleIdsPageRequest
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityNonFungiblesPageRequest
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityNonFungiblesPageResponse
import com.babylon.wallet.android.data.gateway.generated.models.StateNonFungibleDataRequest
import com.babylon.wallet.android.data.gateway.generated.models.StateNonFungibleDetailsResponseItem
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
import com.babylon.wallet.android.domain.model.DAppResources
import com.babylon.wallet.android.domain.model.DAppWithMetadata
import com.babylon.wallet.android.domain.model.Resource
import com.babylon.wallet.android.domain.model.Resources
import com.babylon.wallet.android.domain.model.metadata.DescriptionMetadataItem
import com.babylon.wallet.android.domain.model.metadata.IconUrlMetadataItem
import com.babylon.wallet.android.domain.model.metadata.MetadataItem.Companion.consume
import com.babylon.wallet.android.domain.model.metadata.NameMetadataItem
import com.babylon.wallet.android.domain.model.metadata.OwnerKeysMetadataItem
import com.babylon.wallet.android.domain.model.metadata.SymbolMetadataItem
import com.babylon.wallet.android.presentation.model.ActionableAddress
import rdx.works.profile.data.model.pernetwork.Network
import java.io.IOException
import java.math.BigDecimal
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

    suspend fun getDAppResources(
        dAppMetadata: DAppWithMetadata,
        isRefreshing: Boolean = true
    ): Result<DAppResources>

    suspend fun getEntityOwnerKeys(entityAddress: String, isRefreshing: Boolean = false): Result<OwnerKeysMetadataItem?>
}

@Suppress("TooManyFunctions")
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
            val mapOfAccountsWithNonFungibleResources = buildMapOfAccountsWithNonFungibles(
                entityDetailsResponses = entityDetailsResponses,
                isRefreshing = isRefreshing
            )

            // build result list of accounts with resources
            val listOfAccountsWithResources = accounts.map { account ->
                AccountWithResources(
                    account = account,
                    resources = Resources(
                        fungibleResources = mapOfAccountsWithFungibleResources[account.address].orEmpty().sorted(),
                        nonFungibleResources = mapOfAccountsWithNonFungibleResources[account.address].orEmpty().sorted()
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
                    entityDetailsResponseItem.address // this is account address
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
                    fungibleResourcesItemsList.mapNotNull { fungibleResourcesItem ->
                        val metaDataItems = fungibleResourcesItem.explicitMetadata?.asMetadataItems().orEmpty()
                        val amount = fungibleResourcesItem.vaults.items.first().amount.toBigDecimal()

                        if (amount == BigDecimal.ZERO) return@mapNotNull null

                        Resource.FungibleResource(
                            resourceAddress = fungibleResourcesItem.resourceAddress,
                            amount = amount,
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
        entityDetailsResponses: List<StateEntityDetailsResponse>,
        isRefreshing: Boolean
    ): Map<String, List<Resource.NonFungibleResource>> {
        return entityDetailsResponses.map { entityDetailsResponse ->
            entityDetailsResponse.items
                .groupingBy { entityDetailsResponseItem ->
                    entityDetailsResponseItem.address // this is account address
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
                    nonFungibleResourcesItemsList.mapNotNull { nonFungibleResourcesItem ->
                        val metaDataItems = nonFungibleResourcesItem.explicitMetadata?.asMetadataItems().orEmpty()
                        val nfts = getNonFungibleResourceItemsForAccount(
                            accountAddress = entityItem.address,
                            vaultAddress = nonFungibleResourcesItem.vaults.items.first().vaultAddress,
                            resourceAddress = nonFungibleResourcesItem.resourceAddress,
                            isRefreshing
                        ).value().orEmpty().sorted()

                        if (nfts.isEmpty()) return@mapNotNull null

                        Resource.NonFungibleResource(
                            resourceAddress = nonFungibleResourcesItem.resourceAddress,
                            amount = nonFungibleResourcesItem.vaults.items.first().totalCount,
                            nameMetadataItem = metaDataItems.toMutableList().consume(),
                            descriptionMetadataItem = metaDataItems.toMutableList().consume(),
                            iconMetadataItem = metaDataItems.toMutableList().consume(),
                            items = nfts
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
            .chunked(CHUNK_SIZE_OF_ITEMS)
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
            Result.Success(responses.mapNotNull { it.value() })
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

    private suspend fun getNonFungibleResourceItemsForAccount(
        accountAddress: String,
        vaultAddress: String,
        resourceAddress: String,
        isRefreshing: Boolean = false
    ): Result<List<Resource.NonFungibleResource.Item>> {
        val stateEntityNonFungibleIdsPageResponse = stateApi.entityNonFungibleIdsPage(
            StateEntityNonFungibleIdsPageRequest(
                address = accountAddress,
                vaultAddress = vaultAddress,
                resourceAddress = resourceAddress
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

        val nonFungibleIds = stateEntityNonFungibleIdsPageResponse.value()?.items
        val nonFungibleDataResponsesListResult = nonFungibleIds
            ?.chunked(CHUNK_SIZE_OF_ITEMS)
            ?.map { ids ->
                stateApi.nonFungibleData(
                    StateNonFungibleDataRequest(
                        resourceAddress = resourceAddress,
                        nonFungibleIds = ids
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
            }.orEmpty()

        // if you find any error response in the list of StateNonFungibleDataResponse then return error
        return if (nonFungibleDataResponsesListResult.any { response -> response is Result.Error }) {
            Result.Error(IOException("Failed to fetch the nonFungibleData"))
        } else {
            val nonFungibleResourceItemsList =
                nonFungibleDataResponsesListResult.mapNotNull { nonFungibleDataResponse ->
                    nonFungibleDataResponse.map {
                        it.nonFungibleIds.map { stateNonFungibleDetailsResponseItem ->
                            Resource.NonFungibleResource.Item(
                                collectionAddress = resourceAddress,
                                localId = Resource.NonFungibleResource.Item.ID.from(stateNonFungibleDetailsResponseItem.nonFungibleId),
                                iconMetadataItem = stateNonFungibleDetailsResponseItem.nftImage()
                                    ?.let { imageUrl -> IconUrlMetadataItem(url = imageUrl) }
                            )
                        }
                    }.value()
                }.flatten()
            Result.Success(nonFungibleResourceItemsList)
        }
    }

    private fun StateNonFungibleDetailsResponseItem.nftImage(): Uri? = data.rawJson.fields.find { element ->
        val value = element.value
        value.contains("https")
    }?.value?.toUri()

    // TODO currently this is only used in GetTransactionComponentResourcesUseCase,
    //  we should refactor GetTransactionComponentResourcesUseCase to use the new domain models.
    override suspend fun stateEntityDetails(
        addresses: List<String>,
        isRefreshing: Boolean
    ): Result<StateEntityDetailsResponse> = stateApi.entityDetails(
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

    override suspend fun getDAppResources(
        dAppMetadata: DAppWithMetadata,
        isRefreshing: Boolean
    ): Result<DAppResources> {
        val claimedResources = dAppMetadata.claimedEntities.filter {
            ActionableAddress.Type.from(it) == ActionableAddress.Type.RESOURCE
        }

        val listOfEntityDetailsResponsesResult = getStateEntityDetailsResponse(
            addresses = claimedResources,
            explicitMetadata = ExplicitMetadataKey.forAssets,
            isRefreshing = isRefreshing
        )

        return listOfEntityDetailsResponsesResult.switchMap { entityDetailsResponses ->
            val allResources = entityDetailsResponses.map {
                it.items
            }.flatten()

            val fungibleItems = allResources.filter {
                it.details?.type == StateEntityDetailsResponseItemDetailsType.fungibleResource
            }
            val nonFungibleItems = allResources.filter {
                it.details?.type == StateEntityDetailsResponseItemDetailsType.nonFungibleResource
            }

            val fungibleResources = fungibleItems.map { fungibleItem ->
                val metadataMap = fungibleItem.metadata.asMetadataStringMap()
                Resource.FungibleResource(
                    resourceAddress = fungibleItem.address,
                    amount = null, // No amount given in metadata
                    nameMetadataItem = metadataMap[ExplicitMetadataKey.NAME.key]?.let { NameMetadataItem(it) },
                    symbolMetadataItem = metadataMap[ExplicitMetadataKey.SYMBOL.key]?.let { SymbolMetadataItem(it) },
                    descriptionMetadataItem = metadataMap[ExplicitMetadataKey.DESCRIPTION.key]?.let { DescriptionMetadataItem(it) },
                    iconUrlMetadataItem = metadataMap[ExplicitMetadataKey.ICON_URL.key]?.let { IconUrlMetadataItem(it.toUri()) }
                )
            }

            val nonFungibleResource = nonFungibleItems.map { nonFungibleItem ->
                val metadataMap = nonFungibleItem.metadata.asMetadataStringMap()

                Resource.NonFungibleResource.Item(
                    collectionAddress = nonFungibleItem.address,
                    localId = Resource.NonFungibleResource.Item.ID.from(
                        nonFungibleItem.ancestorIdentities?.globalAddress.orEmpty()
                    ),
                    iconMetadataItem = metadataMap[ExplicitMetadataKey.ICON_URL.key]?.let {
                        IconUrlMetadataItem(it.toUri())
                    }
                )
            }

            val dAppsWithResources = DAppResources(
                fungibleResources = fungibleResources,
                nonFungibleResources = nonFungibleResource,
            )

            Result.Success(dAppsWithResources)
        }
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

    override suspend fun getEntityOwnerKeys(entityAddress: String, isRefreshing: Boolean): Result<OwnerKeysMetadataItem?> {
        return stateApi.entityDetails(
            StateEntityDetailsRequest(
                addresses = listOf(entityAddress)
            )
        ).execute(
            cacheParameters = CacheParameters(
                httpCache = cache,
                timeoutDuration = if (isRefreshing) NO_CACHE else TimeoutDuration.FIVE_MINUTES
            ),
            map = { response ->
                response.items.first().metadata.asMetadataItems().filterIsInstance<OwnerKeysMetadataItem>().firstOrNull()
            },
        )
    }

    companion object {
        private const val CHUNK_SIZE_OF_ITEMS = 20
    }
}
