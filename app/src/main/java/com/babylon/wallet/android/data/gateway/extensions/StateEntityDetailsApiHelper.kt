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
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.Decimal192
import com.radixdlt.sargon.NonFungibleLocalId
import com.radixdlt.sargon.PoolAddress
import com.radixdlt.sargon.ResourceAddress
import com.radixdlt.sargon.ValidatorAddress
import com.radixdlt.sargon.VaultAddress
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.string
import com.radixdlt.sargon.extensions.toDecimal192OrNull
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import rdx.works.core.domain.resources.ExplicitMetadataKey
import rdx.works.core.domain.resources.metadata.claimedEntities
import rdx.works.core.domain.resources.metadata.dAppDefinition
import rdx.works.core.domain.resources.metadata.poolUnit

const val ENTITY_DETAILS_PAGE_LIMIT = 20
const val NFT_DETAILS_PAGE_LIMIT = 50

suspend fun StateApi.fetchAccountGatewayDetails(
    accountsToRequest: Set<AccountAddress>,
    onStateVersion: Long?
) = runCatching {
    val items = mutableListOf<Pair<StateEntityDetailsResponseItem, LedgerState>>()
    paginateDetails(
        addresses = accountsToRequest.map { it.string }.toSet(),
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
    poolAddresses: Set<PoolAddress>,
    stateVersion: Long?
): PoolsResponse {
    if (poolAddresses.isEmpty()) return PoolsResponse(emptyList(), stateVersion)

    val poolUnitToPool = mutableMapOf<ResourceAddress, PoolAddress>()
    val poolToResources = mutableMapOf<PoolAddress, List<FungibleResourcesCollectionItem>>()
    val poolToDAppDefinition = mutableMapOf<PoolAddress, AccountAddress>()

    val poolDetails = mutableMapOf<PoolAddress, StateEntityDetailsResponseItem>()
    val dApps = mutableMapOf<AccountAddress, StateEntityDetailsResponseItem>()

    var resolvedVersion = stateVersion
    paginateDetails(
        addresses = poolAddresses.map { it.string }.toSet(),
        metadataKeys = ExplicitMetadataKey.forPools,
        stateVersion = resolvedVersion,
    ) { page ->
        page.items.forEach { pool ->
            val poolAddress = PoolAddress.init(pool.address)

            poolDetails[poolAddress] = pool
            val metadata = pool.explicitMetadata?.toMetadata().orEmpty()
            val poolUnit = metadata.poolUnit()

            // Associate Pool with Pool Unit
            if (poolUnit != null) {
                poolUnitToPool[poolUnit] = poolAddress
            }

            // Associate Pool with resources
            poolToResources[poolAddress] = pool.fungibleResources?.items.orEmpty()

            // Associate pool with dApp definition
            val dAppDefinition = metadata.dAppDefinition()
            if (dAppDefinition != null) {
                poolToDAppDefinition[poolAddress] = AccountAddress.init(dAppDefinition)
            }
        }
        resolvedVersion = page.ledgerState.stateVersion
    }

    // Resolve associated dApps
    val dAppDefinitionAddresses = poolToDAppDefinition.values.toSet()
    if (dAppDefinitionAddresses.isNotEmpty()) {
        paginateDetails(
            addresses = dAppDefinitionAddresses.map { it.string }.toSet(),
            metadataKeys = ExplicitMetadataKey.forDApps,
            stateVersion = resolvedVersion
        ) { page ->
            page.items.forEach { item ->
                val dAppDefinition = AccountAddress.init(item.address)
                val claimedEntities = item.explicitMetadata?.toMetadata().orEmpty().claimedEntities()
                if (claimedEntities != null) {
                    val poolAddress = poolToDAppDefinition.entries.find { entry ->
                        entry.value == dAppDefinition
                    }?.key?.string

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
        addresses = poolUnitToPool.keys.map { it.string }.toSet(),
        metadataKeys = ExplicitMetadataKey.forAssets,
        stateVersion = resolvedVersion
    ) { page ->
        page.items.forEach { poolUnitDetails ->
            val poolUnitAddress = ResourceAddress.init(poolUnitDetails.address)
            val poolAddress = poolUnitToPool[poolUnitAddress]
            poolDetails[poolAddress]?.let { poolDetails ->
                val dAppDefinition = poolToDAppDefinition[PoolAddress.init(poolDetails.address)]
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
    validatorsAddresses: Set<ValidatorAddress>,
    stateVersion: Long?
): ValidatorsResponse {
    if (validatorsAddresses.isEmpty()) return ValidatorsResponse(emptyList(), stateVersion)

    val validators = mutableListOf<StateEntityDetailsResponseItem>()
    var returnedStateVersion = stateVersion
    paginateDetails(
        addresses = validatorsAddresses.map { it.string }.toSet(),
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

suspend fun StateApi.fetchVaultDetails(vaultAddresses: Set<VaultAddress>): Map<VaultAddress, Decimal192> {
    val vaultAmount = mutableMapOf<VaultAddress, Decimal192>()
    paginateDetails(vaultAddresses.map { it.string }.toSet()) { page ->
        page.items.forEach { item ->
            val vaultDetails = item.details as? StateEntityDetailsResponseFungibleVaultDetails ?: return@forEach
            val amount = vaultDetails.balance.amount.toDecimal192OrNull() ?: return@forEach
            vaultAmount[VaultAddress.init(vaultDetails.balance.vaultAddress)] = amount
        }
    }
    return vaultAmount
}

suspend fun StateApi.getNextNftItems(
    accountAddress: AccountAddress,
    resourceAddress: ResourceAddress,
    vaultAddress: VaultAddress,
    nextCursor: String?,
    stateVersion: Long
): Pair<String?, List<StateNonFungibleDetailsResponseItem>> = entityNonFungibleIdsPage(
    StateEntityNonFungibleIdsPageRequest(
        address = accountAddress.string,
        vaultAddress = vaultAddress.string,
        resourceAddress = resourceAddress.string,
        cursor = nextCursor,
        atLedgerState = LedgerStateSelector(stateVersion = stateVersion)
    )
).toResult().map { ids ->
    val data = mutableListOf<StateNonFungibleDetailsResponseItem>()
    ids.items.chunked(ENTITY_DETAILS_PAGE_LIMIT).forEach { idsPage ->
        nonFungibleData(
            StateNonFungibleDataRequest(
                resourceAddress = resourceAddress.string,
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
    resourceAddress: ResourceAddress,
    nonFungibleIds: Set<NonFungibleLocalId>,
    onPage: (StateNonFungibleDataResponse) -> Unit
) {
    nonFungibleIds.chunked(NFT_DETAILS_PAGE_LIMIT).forEach { idsChunk ->
        val response = nonFungibleData(
            StateNonFungibleDataRequest(
                resourceAddress = resourceAddress.string,
                nonFungibleIds = idsChunk.map { it.string }
            )
        ).toResult().getOrThrow()
        onPage(response)
    }
}
