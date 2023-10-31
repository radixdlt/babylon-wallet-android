package com.babylon.wallet.android.data.repository.state

import com.babylon.wallet.android.data.gateway.apis.StateApi
import com.babylon.wallet.android.data.gateway.extensions.getSingleEntityDetails
import com.babylon.wallet.android.data.gateway.extensions.paginateDetails
import com.babylon.wallet.android.data.gateway.extensions.paginateFungibles
import com.babylon.wallet.android.data.gateway.extensions.paginateNonFungibles
import com.babylon.wallet.android.data.gateway.generated.models.EntityMetadataCollection
import com.babylon.wallet.android.data.gateway.generated.models.FungibleResourcesCollectionItem
import com.babylon.wallet.android.data.gateway.generated.models.LedgerState
import com.babylon.wallet.android.data.gateway.generated.models.LedgerStateSelector
import com.babylon.wallet.android.data.gateway.generated.models.NonFungibleResourcesCollectionItem
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsResponseFungibleVaultDetails
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsResponseItem
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsResponseItemDetails
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityNonFungibleIdsPageRequest
import com.babylon.wallet.android.data.gateway.generated.models.StateNonFungibleDataRequest
import com.babylon.wallet.android.data.gateway.generated.models.StateNonFungibleDetailsResponseItem
import com.babylon.wallet.android.data.gateway.model.ExplicitMetadataKey
import com.babylon.wallet.android.data.repository.toResult
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import rdx.works.profile.data.model.pernetwork.Network
import java.math.BigDecimal

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
        stateApi.paginateDetails(
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
                        stateApi.paginateFungibles(
                            item = accountOnLedger,
                            ledgerState = chunkedAccounts.ledgerState,
                            onPage = allFungibles::addAll
                        )
                    }

                    val allNonFungiblePagesForAccount = async {
                        stateApi.paginateNonFungibles(
                            item = accountOnLedger,
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
        stateApi.paginateDetails(
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

            stateApi.paginateDetails(
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
        stateApi.paginateDetails(
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

    suspend fun getVaultsDetails(vaultAddresses: Set<String>): Map<String, BigDecimal> {
        val vaultAmount = mutableMapOf<String, BigDecimal>()
        stateApi.paginateDetails(vaultAddresses) { page ->
            page.items.forEach { item ->
                val vaultDetails = item.details as? StateEntityDetailsResponseFungibleVaultDetails ?: return@forEach
                val amount = vaultDetails.balance.amount.toBigDecimalOrNull() ?: return@forEach
                vaultAmount[vaultDetails.balance.vaultAddress] = amount
            }
        }
        return vaultAmount
    }

    suspend fun getResourceDetails(resourceAddress: String): StateEntityDetailsResponseItem {
        return stateApi.getSingleEntityDetails(
            address = resourceAddress,
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
            )
        )
    }

    suspend fun getDAppsDetails(definitionAddresses: Set<String>): List<StateEntityDetailsResponseItem> {
        val items = mutableListOf<StateEntityDetailsResponseItem>()
        stateApi.paginateDetails(
            addresses = definitionAddresses,
            metadataKeys = setOf(
                ExplicitMetadataKey.NAME,
                ExplicitMetadataKey.DESCRIPTION,
                ExplicitMetadataKey.ACCOUNT_TYPE,
                ExplicitMetadataKey.DAPP_DEFINITION,
                ExplicitMetadataKey.DAPP_DEFINITIONS,
                ExplicitMetadataKey.CLAIMED_WEBSITES,
                ExplicitMetadataKey.CLAIMED_ENTITIES,
                ExplicitMetadataKey.ICON_URL
            )
        ) { page ->
            items.addAll(page.items)
        }
        return items
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
