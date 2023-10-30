package com.babylon.wallet.android.data.gateway.extensions

import com.babylon.wallet.android.data.gateway.apis.StateApi
import com.babylon.wallet.android.data.gateway.generated.models.FungibleResourcesCollectionItem
import com.babylon.wallet.android.data.gateway.generated.models.LedgerState
import com.babylon.wallet.android.data.gateway.generated.models.LedgerStateSelector
import com.babylon.wallet.android.data.gateway.generated.models.NonFungibleResourcesCollectionItem
import com.babylon.wallet.android.data.gateway.generated.models.ResourceAggregationLevel
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsOptIns
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsRequest
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsResponse
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsResponseItem
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityFungiblesPageRequest
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityNonFungiblesPageRequest
import com.babylon.wallet.android.data.gateway.model.ExplicitMetadataKey
import com.babylon.wallet.android.data.repository.state.StateApiDelegate
import com.babylon.wallet.android.data.repository.toResult

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
    metadataKeys: Set<ExplicitMetadataKey>,
    aggregationLevel: ResourceAggregationLevel = ResourceAggregationLevel.vault,
    stateVersion: Long? = null,
    onPage: suspend (StateEntityDetailsResponse) -> Unit
) = addresses
    .chunked(StateApiDelegate.ENTITY_DETAILS_PAGE_LIMIT)
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
