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
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsResponseItem
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityFungiblesPageRequest
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityNonFungiblesPageRequest
import com.babylon.wallet.android.data.gateway.model.ExplicitMetadataKey
import com.babylon.wallet.android.data.repository.executeSafe
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import rdx.works.profile.data.model.pernetwork.Network

class StateApiDelegate(
    private val stateApi: StateApi
) {

    suspend fun fetchAllResources(
        accounts: List<Network.Account>,
        onAccount: (
            account: Network.Account,
            ledgerState: LedgerState,
            accountMetadata: EntityMetadataCollection?,
            fungibles: List<FungibleResourcesCollectionItem>,
            nonFungibles: List<NonFungibleResourcesCollectionItem>
        ) -> Unit
    ) {
        accounts
            .chunked(ENTITY_DETAILS_PAGE_LIMIT)
            .map { accountsChunked ->
                val details = stateApi.entityDetails(
                    StateEntityDetailsRequest(
                        addresses = accountsChunked.map { it.address },
                        aggregationLevel = ResourceAggregationLevel.vault,
                        optIns = StateEntityDetailsOptIns(
                            explicitMetadata = listOf(
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
                                ExplicitMetadataKey.DAPP_DEFINITIONS,
                            ).map { it.key }
                        )
                    )
                ).executeSafe().getOrThrow()

                accountsChunked.forEach { account ->
                    val accountOnLedger = details.items.find { it.address == account.address }

                    val allFungibles = mutableListOf<FungibleResourcesCollectionItem>()
                    val allNonFungibles = mutableListOf<NonFungibleResourcesCollectionItem>()

                    allFungibles.addAll(accountOnLedger?.fungibleResources?.items.orEmpty())
                    allNonFungibles.addAll(accountOnLedger?.nonFungibleResources?.items.orEmpty())

                    coroutineScope {
                        val allFungiblePagesForAccount = async {
                            accountOnLedger?.paginateFungibles(
                                ledgerState = details.ledgerState,
                                onPage = allFungibles::addAll
                            )
                        }

                        val allNonFungiblePagesForAccount = async {
                            accountOnLedger?.paginateNonFungibles(
                                ledgerState = details.ledgerState,
                                onPage = allNonFungibles::addAll
                            )
                        }

                        awaitAll(allFungiblePagesForAccount, allNonFungiblePagesForAccount)
                    }

                    onAccount(account, details.ledgerState, accountOnLedger?.explicitMetadata, allFungibles, allNonFungibles)
                }
            }
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
            ).executeSafe().getOrThrow()

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
            ).executeSafe().getOrThrow()

            onPage(pageResponse.items)
            nextCursor = pageResponse.nextCursor
        }
    }

    companion object {
        const val ENTITY_DETAILS_PAGE_LIMIT = 20
    }
}
