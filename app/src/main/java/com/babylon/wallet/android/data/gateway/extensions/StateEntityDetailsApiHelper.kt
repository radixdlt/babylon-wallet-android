package com.babylon.wallet.android.data.gateway.extensions

import com.babylon.wallet.android.data.gateway.apis.StateApi
import com.babylon.wallet.android.data.gateway.generated.models.FungibleResourcesCollection
import com.babylon.wallet.android.data.gateway.generated.models.FungibleResourcesCollectionItem
import com.babylon.wallet.android.data.gateway.generated.models.LedgerState
import com.babylon.wallet.android.data.gateway.generated.models.LedgerStateSelector
import com.babylon.wallet.android.data.gateway.generated.models.NonFungibleResourcesCollection
import com.babylon.wallet.android.data.gateway.generated.models.NonFungibleResourcesCollectionItem
import com.babylon.wallet.android.data.gateway.generated.models.ResourceAggregationLevel
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsOptIns
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsRequest
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsResponse
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsResponseFungibleVaultDetails
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsResponseItem
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityFungiblesPageRequest
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityFungiblesPageRequestOptIns
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityNonFungibleIdsPageRequest
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityNonFungiblesPageRequest
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityNonFungiblesPageRequestOptIns
import com.babylon.wallet.android.data.gateway.generated.models.StateNonFungibleDataRequest
import com.babylon.wallet.android.data.gateway.generated.models.StateNonFungibleDataResponse
import com.babylon.wallet.android.data.gateway.generated.models.StateNonFungibleDetailsResponseItem
import com.babylon.wallet.android.data.repository.toResult
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import rdx.works.core.domain.resources.ExplicitMetadataKey
import rdx.works.core.domain.resources.metadata.claimedEntities
import rdx.works.core.domain.resources.metadata.dAppDefinition
import rdx.works.core.domain.resources.metadata.poolUnit
import java.math.BigDecimal

const val ENTITY_DETAILS_PAGE_LIMIT = 20
const val NFT_DETAILS_PAGE_LIMIT = 50

suspend fun StateApi.fetchAccountGatewayDetails(
    accountsToRequest: Set<String>,
    onStateVersion: Long?
) = runCatching {
    val items = mutableListOf<Pair<StateEntityDetailsResponseItem, LedgerState>>()
    paginateDetails(
        addresses = accountsToRequest,
        metadataKeys = ExplicitMetadataKey.forAssets + ExplicitMetadataKey.ACCOUNT_TYPE,
        stateVersion = onStateVersion
    ) { chunkedAccounts ->
        val page = chunkedAccounts.items.map { accountOnLedger ->
            val allFungibles = mutableListOf<FungibleResourcesCollectionItem>()
            val allNonFungibles = mutableListOf<NonFungibleResourcesCollectionItem>()

            allFungibles.addAll(accountOnLedger.fungibleResources?.items.orEmpty())
            allNonFungibles.addAll(accountOnLedger.nonFungibleResources?.items.orEmpty())

            coroutineScope {
                val allFungiblePagesForAccount = async {
                    paginateFungibles(
                        item = accountOnLedger,
                        ledgerState = chunkedAccounts.ledgerState,
                        onPage = allFungibles::addAll
                    )
                }

                val allNonFungiblePagesForAccount = async {
                    paginateNonFungibles(
                        item = accountOnLedger,
                        ledgerState = chunkedAccounts.ledgerState,
                        onPage = allNonFungibles::addAll
                    )
                }

                awaitAll(allFungiblePagesForAccount, allNonFungiblePagesForAccount)
            }

            accountOnLedger.copy(
                fungibleResources = FungibleResourcesCollection(
                    items = allFungibles,
                    totalCount = allFungibles.size.toLong(),
                ),
                nonFungibleResources = NonFungibleResourcesCollection(
                    items = allNonFungibles,
                    totalCount = allNonFungibles.size.toLong()
                )
            ) to chunkedAccounts.ledgerState
        }
        items.addAll(page)
    }
    items
}

@Suppress("LongMethod")
suspend fun StateApi.fetchPools(
    poolAddresses: Set<String>,
    stateVersion: Long?
): PoolsResponse {
    if (poolAddresses.isEmpty()) return PoolsResponse(emptyList(), stateVersion)

    val poolUnitToPool = mutableMapOf<String, String>()
    val poolToResources = mutableMapOf<String, List<FungibleResourcesCollectionItem>>()
    val poolToDAppDefinition = mutableMapOf<String, String>()

    val poolDetails = mutableMapOf<String, StateEntityDetailsResponseItem>()
    val dApps = mutableMapOf<String, StateEntityDetailsResponseItem>()

    var resolvedVersion = stateVersion
    paginateDetails(
        addresses = poolAddresses,
        metadataKeys = ExplicitMetadataKey.forPools,
        stateVersion = resolvedVersion,
    ) { page ->
        page.items.forEach { pool ->
            poolDetails[pool.address] = pool
            val metadata = pool.explicitMetadata?.toMetadata().orEmpty()
            val poolUnit = metadata.poolUnit().orEmpty()

            // Associate Pool with Pool Unit
            poolUnitToPool[poolUnit] = pool.address

            // Associate Pool with resources
            poolToResources[pool.address] = pool.fungibleResources?.items.orEmpty()

            // Associate pool with dApp definition
            val dAppDefinition = metadata.dAppDefinition()
            if (dAppDefinition != null) {
                poolToDAppDefinition[pool.address] = dAppDefinition
            }
        }
        resolvedVersion = page.ledgerState.stateVersion
    }

    // Resolve associated dApps
    val dAppDefinitionAddresses = poolToDAppDefinition.values.toSet()
    if (dAppDefinitionAddresses.isNotEmpty()) {
        paginateDetails(
            addresses = dAppDefinitionAddresses,
            metadataKeys = ExplicitMetadataKey.forDApps,
            stateVersion = resolvedVersion
        ) { page ->
            page.items.forEach { item ->
                val dAppDefinition = item.address
                val claimedEntities = item.explicitMetadata?.toMetadata().orEmpty().claimedEntities()
                if (claimedEntities != null) {
                    val poolAddress = poolToDAppDefinition.entries.find { entry ->
                        entry.value == dAppDefinition
                    }?.key

                    if (poolAddress in claimedEntities) {
                        // Two way linking exists, store dApp information
                        dApps[dAppDefinition] = item
                    }
                }
            }
        }
    }

    // Resolve Pool Units
    val poolItems = mutableListOf<PoolsResponse.PoolItem>()
    paginateDetails(
        addresses = poolUnitToPool.keys,
        metadataKeys = ExplicitMetadataKey.forAssets,
        stateVersion = resolvedVersion
    ) { page ->
        page.items.forEach { poolUnitDetails ->
            val poolAddress = poolUnitToPool[poolUnitDetails.address]
            poolDetails[poolAddress]?.let { poolDetails ->
                val dAppDefinition = poolToDAppDefinition[poolDetails.address]
                val dAppDetails = if (dAppDefinition != null) {
                    dApps[dAppDefinition]
                } else {
                    null
                }
                poolItems.add(
                    PoolsResponse.PoolItem(
                        poolDetails = poolDetails,
                        poolUnitDetails = poolUnitDetails,
                        associatedDAppDetails = dAppDetails,
                        poolResourcesDetails = poolToResources[poolAddress].orEmpty(),
                    )
                )
            }
        }
    }

    return PoolsResponse(
        poolItems = poolItems,
        stateVersion = resolvedVersion
    )
}

data class PoolsResponse(
    val poolItems: List<PoolItem>,
    val stateVersion: Long?
) {
    data class PoolItem(
        val poolDetails: StateEntityDetailsResponseItem,
        val poolUnitDetails: StateEntityDetailsResponseItem,
        val associatedDAppDetails: StateEntityDetailsResponseItem?,
        val poolResourcesDetails: List<FungibleResourcesCollectionItem>
    )
}

suspend fun StateApi.fetchValidators(
    validatorsAddresses: Set<String>,
    stateVersion: Long?
): ValidatorsResponse {
    if (validatorsAddresses.isEmpty()) return ValidatorsResponse(emptyList(), stateVersion)

    val validators = mutableListOf<StateEntityDetailsResponseItem>()
    var returnedStateVersion = stateVersion
    paginateDetails(
        addresses = validatorsAddresses,
        metadataKeys = ExplicitMetadataKey.forValidators,
        stateVersion = stateVersion,
    ) { poolsChunked ->
        validators.addAll(poolsChunked.items)
        returnedStateVersion = poolsChunked.ledgerState.stateVersion
    }

    return ValidatorsResponse(
        validators = validators,
        stateVersion = returnedStateVersion
    )
}

data class ValidatorsResponse(
    val validators: List<StateEntityDetailsResponseItem>,
    val stateVersion: Long? = null
)

suspend fun StateApi.fetchVaultDetails(vaultAddresses: Set<String>): Map<String, BigDecimal> {
    val vaultAmount = mutableMapOf<String, BigDecimal>()
    paginateDetails(vaultAddresses) { page ->
        page.items.forEach { item ->
            val vaultDetails = item.details as? StateEntityDetailsResponseFungibleVaultDetails ?: return@forEach
            val amount = vaultDetails.balance.amount.toBigDecimalOrNull() ?: return@forEach
            vaultAmount[vaultDetails.balance.vaultAddress] = amount
        }
    }
    return vaultAmount
}

suspend fun StateApi.getNextNftItems(
    accountAddress: String,
    resourceAddress: String,
    vaultAddress: String,
    nextCursor: String?,
    stateVersion: Long
): Pair<String?, List<StateNonFungibleDetailsResponseItem>> = entityNonFungibleIdsPage(
    StateEntityNonFungibleIdsPageRequest(
        address = accountAddress,
        vaultAddress = vaultAddress,
        resourceAddress = resourceAddress,
        cursor = nextCursor,
        atLedgerState = LedgerStateSelector(stateVersion = stateVersion)
    )
).toResult().map { ids ->
    val data = mutableListOf<StateNonFungibleDetailsResponseItem>()
    ids.items.chunked(ENTITY_DETAILS_PAGE_LIMIT).forEach { idsPage ->
        nonFungibleData(
            StateNonFungibleDataRequest(
                resourceAddress = resourceAddress,
                nonFungibleIds = idsPage,
                atLedgerState = LedgerStateSelector(stateVersion = stateVersion)
            )
        ).toResult().onSuccess {
            data.addAll(it.nonFungibleIds)
        }
    }
    ids.nextCursor to data
}.getOrThrow()

suspend fun StateApi.getSingleEntityDetails(
    address: String,
    metadataKeys: Set<ExplicitMetadataKey>,
    aggregationLevel: ResourceAggregationLevel = ResourceAggregationLevel.vault,
    stateVersion: Long? = null
): StateEntityDetailsResponseItem {
    return stateEntityDetails(
        StateEntityDetailsRequest(
            addresses = listOf(address),
            aggregationLevel = aggregationLevel,
            atLedgerState = stateVersion?.let { LedgerStateSelector(stateVersion = it) },
            optIns = StateEntityDetailsOptIns(
                explicitMetadata = metadataKeys.map { it.key }
            )
        )
    ).toResult().getOrThrow().items.first()
}

suspend fun StateApi.paginateDetails(
    addresses: Set<String>,
    metadataKeys: Set<ExplicitMetadataKey> = setOf(),
    aggregationLevel: ResourceAggregationLevel = ResourceAggregationLevel.vault,
    stateVersion: Long? = null,
    onPage: suspend (StateEntityDetailsResponse) -> Unit
) = addresses
    .chunked(ENTITY_DETAILS_PAGE_LIMIT)
    .forEach { addressesChunk ->
        val response = stateEntityDetails(
            StateEntityDetailsRequest(
                addresses = addressesChunk,
                aggregationLevel = aggregationLevel,
                atLedgerState = stateVersion?.let { LedgerStateSelector(stateVersion = it) },
                optIns = StateEntityDetailsOptIns(
                    explicitMetadata = metadataKeys.map { it.key }
                )
            )
        ).toResult().getOrThrow()

        onPage(response)
    }

suspend fun StateApi.paginateFungibles(
    item: StateEntityDetailsResponseItem,
    ledgerState: LedgerState,
    onPage: (items: List<FungibleResourcesCollectionItem>) -> Unit
) {
    var nextCursor: String? = item.fungibleResources?.nextCursor
    while (nextCursor != null) {
        val pageResponse = entityFungiblesPage(
            StateEntityFungiblesPageRequest(
                address = item.address,
                cursor = nextCursor,
                aggregationLevel = ResourceAggregationLevel.vault,
                atLedgerState = LedgerStateSelector(stateVersion = ledgerState.stateVersion),
                optIns = StateEntityFungiblesPageRequestOptIns(
                    explicitMetadata = ExplicitMetadataKey.forAssets.map { it.key }
                )
            )
        ).toResult().getOrThrow()

        onPage(pageResponse.items)
        nextCursor = pageResponse.nextCursor
    }
}

suspend fun StateApi.paginateNonFungibles(
    item: StateEntityDetailsResponseItem,
    ledgerState: LedgerState,
    onPage: (items: List<NonFungibleResourcesCollectionItem>) -> Unit
) {
    var nextCursor: String? = item.nonFungibleResources?.nextCursor
    while (nextCursor != null) {
        val pageResponse = entityNonFungiblesPage(
            StateEntityNonFungiblesPageRequest(
                address = item.address,
                cursor = nextCursor,
                aggregationLevel = ResourceAggregationLevel.vault,
                atLedgerState = LedgerStateSelector(stateVersion = ledgerState.stateVersion),
                optIns = StateEntityNonFungiblesPageRequestOptIns(
                    explicitMetadata = ExplicitMetadataKey.forAssets.map { it.key }
                )
            )
        ).toResult().getOrThrow()

        onPage(pageResponse.items)
        nextCursor = pageResponse.nextCursor
    }
}

suspend fun StateApi.paginateNonFungibles(
    resourceAddress: String,
    nonFungibleIds: List<String>,
    onPage: (StateNonFungibleDataResponse) -> Unit
) {
    nonFungibleIds.chunked(NFT_DETAILS_PAGE_LIMIT).forEach { idsChunk ->
        val response = nonFungibleData(
            StateNonFungibleDataRequest(
                resourceAddress = resourceAddress,
                nonFungibleIds = idsChunk
            )
        ).toResult().getOrThrow()
        onPage(response)
    }
}
