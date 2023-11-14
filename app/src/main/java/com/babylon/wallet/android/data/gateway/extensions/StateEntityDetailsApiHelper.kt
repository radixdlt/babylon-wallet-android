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
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityNonFungibleIdsPageRequest
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityNonFungiblesPageRequest
import com.babylon.wallet.android.data.gateway.generated.models.StateNonFungibleDataRequest
import com.babylon.wallet.android.data.gateway.generated.models.StateNonFungibleDetailsResponseItem
import com.babylon.wallet.android.data.gateway.model.ExplicitMetadataKey
import com.babylon.wallet.android.data.repository.toResult
import com.babylon.wallet.android.domain.model.resources.metadata.MetadataItem.Companion.consume
import com.babylon.wallet.android.domain.model.resources.metadata.PoolUnitMetadataItem
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.math.BigDecimal

const val ENTITY_DETAILS_PAGE_LIMIT = 25

suspend fun StateApi.fetchAccountGatewayDetails(
    accountsToRequest: Set<String>,
    onStateVersion: Long?
) = runCatching {
    val items = mutableListOf<Pair<StateEntityDetailsResponseItem, LedgerState>>()
    paginateDetails(
        addresses = accountsToRequest,
        metadataKeys = setOf(
            ExplicitMetadataKey.ACCOUNT_TYPE,

            ExplicitMetadataKey.NAME,
            ExplicitMetadataKey.SYMBOL,
            ExplicitMetadataKey.DESCRIPTION,
            ExplicitMetadataKey.RELATED_WEBSITES,
            ExplicitMetadataKey.ICON_URL,
            ExplicitMetadataKey.INFO_URL,
            ExplicitMetadataKey.VALIDATOR,
            ExplicitMetadataKey.POOL,
            ExplicitMetadataKey.TAGS,
            ExplicitMetadataKey.DAPP_DEFINITIONS
        ),
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

suspend fun StateApi.fetchPools(
    poolAddresses: Set<String>,
    stateVersion: Long
): Map<StateEntityDetailsResponseItem, List<FungibleResourcesCollectionItem>> {
    if (poolAddresses.isEmpty()) return emptyMap()

    val poolWithResources = mutableMapOf<String, List<FungibleResourcesCollectionItem>>()
    val resourceToPoolComponentAssociation = mutableMapOf<String, String>()

    paginateDetails(
        addresses = poolAddresses,
        metadataKeys = setOf(
            ExplicitMetadataKey.NAME,
            ExplicitMetadataKey.ICON_URL,
            ExplicitMetadataKey.POOL_UNIT
        ),
        stateVersion = stateVersion,
    ) { poolComponents ->
        poolComponents.items.forEach { pool ->
            val metadata = pool.explicitMetadata?.asMetadataItems().orEmpty().toMutableList()
            val associatedResource = metadata.consume<PoolUnitMetadataItem>()?.resourceAddress.orEmpty()
            resourceToPoolComponentAssociation[associatedResource] = pool.address
            poolWithResources[pool.address] = pool.fungibleResources?.items.orEmpty()
        }
    }

    val result = mutableMapOf<StateEntityDetailsResponseItem, List<FungibleResourcesCollectionItem>>()
    paginateDetails(
        addresses = resourceToPoolComponentAssociation.keys, // Request details for resources
        metadataKeys = setOf(
            ExplicitMetadataKey.NAME,
            ExplicitMetadataKey.SYMBOL,
            ExplicitMetadataKey.DESCRIPTION,
            ExplicitMetadataKey.RELATED_WEBSITES,
            ExplicitMetadataKey.ICON_URL,
            ExplicitMetadataKey.INFO_URL,
            ExplicitMetadataKey.VALIDATOR,
            ExplicitMetadataKey.POOL,
            ExplicitMetadataKey.TAGS,
            ExplicitMetadataKey.DAPP_DEFINITIONS
        ),
        stateVersion = stateVersion
    ) { resourcesDetails ->
        resourcesDetails.items.forEach { resourceDetails ->
            val poolAddress = resourceToPoolComponentAssociation[resourceDetails.address]
            result[resourceDetails] = poolWithResources[poolAddress].orEmpty()
        }
    }

    return result
}

suspend fun StateApi.fetchValidators(
    validatorsAddresses: Set<String>,
    stateVersion: Long
): List<StateEntityDetailsResponseItem> {
    if (validatorsAddresses.isEmpty()) return emptyList()

    val validators = mutableListOf<StateEntityDetailsResponseItem>()
    paginateDetails(
        addresses = validatorsAddresses,
        metadataKeys = setOf(
            ExplicitMetadataKey.NAME,
            ExplicitMetadataKey.ICON_URL,
            ExplicitMetadataKey.DESCRIPTION,
            ExplicitMetadataKey.CLAIM_NFT,
        ),
        stateVersion = stateVersion,
    ) { poolsChunked ->
        validators.addAll(poolsChunked.items)
    }

    return validators
}

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
                atLedgerState = LedgerStateSelector(stateVersion = ledgerState.stateVersion)
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
                atLedgerState = LedgerStateSelector(stateVersion = ledgerState.stateVersion)
            )
        ).toResult().getOrThrow()

        onPage(pageResponse.items)
        nextCursor = pageResponse.nextCursor
    }
}
