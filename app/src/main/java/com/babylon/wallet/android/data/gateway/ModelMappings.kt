package com.babylon.wallet.android.data.gateway

import com.babylon.wallet.android.data.gateway.generated.models.*
import com.babylon.wallet.android.domain.model.*
import java.math.BigDecimal

fun StateEntityDetailsResponseItem.toFungibleToken() = FungibleToken(
    address = address,
    metadata = metadata.asMetadataStringMap()
)

fun StateEntityDetailsResponseItem.toNonFungibleToken() = NonFungibleToken(
    address = address,
    nonFungibleIdContainer = NonFungibleTokenIdContainer(ids = listOf()), // TODO 1181
    metadataContainer = NonFungibleMetadataContainer(
        metadata = metadata.asMetadataStringMap()
    )
)

fun NonFungibleIdsResponse.toDomainModel(): NonFungibleTokenIdContainer {
    return NonFungibleTokenIdContainer(
        ids = nonFungibleIds.items.map { nonFungibleIdsCollectionItem ->
            nonFungibleIdsCollectionItem.nonFungibleId
        }
    )
}
