package com.babylon.wallet.android.data.repository.state

import com.babylon.wallet.android.data.gateway.apis.StateApi
import com.babylon.wallet.android.data.gateway.generated.models.EntityMetadataCollection
import com.babylon.wallet.android.data.gateway.generated.models.FungibleResourcesCollectionItem
import com.babylon.wallet.android.data.gateway.generated.models.LedgerState
import com.babylon.wallet.android.data.gateway.generated.models.LedgerStateSelector
import com.babylon.wallet.android.data.gateway.generated.models.NonFungibleResourcesCollectionItem
import com.babylon.wallet.android.data.gateway.generated.models.ResourceAggregationLevel
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsOptIns
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsRequest
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsResponse
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsResponseItem
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsResponseItemDetails
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityFungiblesPageRequest
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityNonFungibleIdsPageRequest
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityNonFungiblesPageRequest
import com.babylon.wallet.android.data.gateway.generated.models.StateNonFungibleDataRequest
import com.babylon.wallet.android.data.gateway.generated.models.StateNonFungibleDataResponse
import com.babylon.wallet.android.data.gateway.generated.models.StateNonFungibleDetailsResponseItem
import com.babylon.wallet.android.data.gateway.model.ExplicitMetadataKey
import com.babylon.wallet.android.data.repository.toResult
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import rdx.works.profile.data.model.pernetwork.Network

class StateApiDelegate(
    private val stateApi: StateApi
) {

    suspend fun fetchAllResources(
        accounts: Set<Network.Account>,
        onStateVersion: Long? = null,
        onAccount: suspend (
            account: Network.Account,
            accountGatewayDetails: AccountGatewayDetails
        ) -> Unit
    ) {
        entityDetails(
            addresses = accounts.map { it.address }.toSet(),
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
            chunkedAccounts.items.forEach { accountOnLedger ->
                val account = accounts.find { it.address == accountOnLedger.address } ?: return@forEach

                val allFungibles = mutableListOf<FungibleResourcesCollectionItem>()
                val allNonFungibles = mutableListOf<NonFungibleResourcesCollectionItem>()

                allFungibles.addAll(accountOnLedger.fungibleResources?.items.orEmpty())
                allNonFungibles.addAll(accountOnLedger.nonFungibleResources?.items.orEmpty())

                coroutineScope {
                    val allFungiblePagesForAccount = async {
                        accountOnLedger.paginateFungibles(
                            ledgerState = chunkedAccounts.ledgerState,
                            onPage = allFungibles::addAll
                        )
                    }

                    val allNonFungiblePagesForAccount = async {
                        accountOnLedger.paginateNonFungibles(
                            ledgerState = chunkedAccounts.ledgerState,
                            onPage = allNonFungibles::addAll
                        )
                    }

                    awaitAll(allFungiblePagesForAccount, allNonFungiblePagesForAccount)
                }

                val gatewayDetails = AccountGatewayDetails(
                    ledgerState = chunkedAccounts.ledgerState,
                    accountMetadata = accountOnLedger.explicitMetadata,
                    fungibles = allFungibles,
                    nonFungibles = allNonFungibles
                )
                onAccount(account, gatewayDetails)
            }
        }
    }

    suspend fun getPoolDetails(
        poolAddresses: Set<String>,
        stateVersion: Long
    ): Map<StateEntityDetailsResponseItem, Map<String, StateEntityDetailsResponseItemDetails>> {
        if (poolAddresses.isEmpty()) return emptyMap()
        val pools = mutableListOf<StateEntityDetailsResponseItem>()
        entityDetails(
            addresses = poolAddresses,
            metadataKeys = setOf(
                ExplicitMetadataKey.NAME,
                ExplicitMetadataKey.ICON_URL,
                ExplicitMetadataKey.POOL_UNIT,
            ),
            stateVersion = stateVersion,
        ) { poolsChunked ->
            pools.addAll(poolsChunked.items)
        }

        // We actually need to fetch details for each item involved in the pool
        // since total supply and divisibility of each item are needed in order
        // to know what is the user's owned amount out of each item
        return pools.associateWith { pool ->
            val itemsWithDetails = mutableMapOf<String, StateEntityDetailsResponseItemDetails>()
            val itemsInPool = pool.fungibleResources?.items?.map { it.resourceAddress }.orEmpty().toSet()

            entityDetails(
                addresses = itemsInPool,
                metadataKeys = setOf(
                    ExplicitMetadataKey.NAME,
                    ExplicitMetadataKey.SYMBOL,
                    ExplicitMetadataKey.ICON_URL
                ),
                stateVersion = stateVersion
            ) { itemsChuncked ->
                itemsChuncked.items.forEach { item ->
                    val details = item.details
                    if (details != null) {
                        itemsWithDetails[item.address] = details
                    }
                }
            }

            itemsWithDetails
        }
    }

    suspend fun getValidatorsDetails(
        validatorsAddresses: Set<String>,
        stateVersion: Long
    ): List<StateEntityDetailsResponseItem> {
        if (validatorsAddresses.isEmpty()) return emptyList()

        val validators = mutableListOf<StateEntityDetailsResponseItem>()
        entityDetails(
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

    suspend fun getNextNftItems(
        accountAddress: String,
        resourceAddress: String,
        vaultAddress: String,
        nextCursor: String?,
        stateVersion: Long
    ): Pair<String?, List<StateNonFungibleDetailsResponseItem>> = stateApi.entityNonFungibleIdsPage(
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
            stateApi.nonFungibleData(
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

    private suspend fun entityDetails(
        addresses: Set<String>,
        metadataKeys: Set<ExplicitMetadataKey>,
        aggregationLevel: ResourceAggregationLevel = ResourceAggregationLevel.vault,
        stateVersion: Long? = null,
        onPage: suspend (StateEntityDetailsResponse) -> Unit
    ) = addresses
        .chunked(ENTITY_DETAILS_PAGE_LIMIT)
        .forEach { addressesChunk ->
            val response = stateApi.entityDetails(
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

    private suspend fun StateEntityDetailsResponseItem.paginateFungibles(
        ledgerState: LedgerState,
        onPage: (items: List<FungibleResourcesCollectionItem>) -> Unit
    ) {
        var nextCursor: String? = fungibleResources?.nextCursor
        while (nextCursor != null) {
            val pageResponse = stateApi.entityFungiblesPage(
                StateEntityFungiblesPageRequest(
                    address = address,
                    cursor = nextCursor,
                    aggregationLevel = ResourceAggregationLevel.vault,
                    atLedgerState = LedgerStateSelector(stateVersion = ledgerState.stateVersion)
                )
            ).toResult().getOrThrow()

            onPage(pageResponse.items)
            nextCursor = pageResponse.nextCursor
        }
    }

    private suspend fun StateEntityDetailsResponseItem.paginateNonFungibles(
        ledgerState: LedgerState,
        onPage: (items: List<NonFungibleResourcesCollectionItem>) -> Unit
    ) {
        var nextCursor: String? = nonFungibleResources?.nextCursor
        while (nextCursor != null) {
            val pageResponse = stateApi.entityNonFungiblesPage(
                StateEntityNonFungiblesPageRequest(
                    address = address,
                    cursor = nextCursor,
                    aggregationLevel = ResourceAggregationLevel.vault,
                    atLedgerState = LedgerStateSelector(stateVersion = ledgerState.stateVersion)
                )
            ).toResult().getOrThrow()

            onPage(pageResponse.items)
            nextCursor = pageResponse.nextCursor
        }
    }

    data class AccountGatewayDetails(
        val ledgerState: LedgerState,
        val accountMetadata: EntityMetadataCollection?,
        val fungibles: List<FungibleResourcesCollectionItem>,
        val nonFungibles: List<NonFungibleResourcesCollectionItem>
    )

    companion object {
        const val ENTITY_DETAILS_PAGE_LIMIT = 20
    }
}
