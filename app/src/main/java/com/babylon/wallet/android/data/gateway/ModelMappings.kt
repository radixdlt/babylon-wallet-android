package com.babylon.wallet.android.data.gateway

import com.babylon.wallet.android.data.gateway.generated.model.EntityDetailsResponse
import com.babylon.wallet.android.data.gateway.generated.model.EntityMetadataCollection
import com.babylon.wallet.android.data.gateway.generated.model.FungibleResourcesCollection
import com.babylon.wallet.android.data.gateway.generated.model.NonFungibleLocalIdsResponse
import com.babylon.wallet.android.data.gateway.generated.model.NonFungibleResourcesCollection
import com.babylon.wallet.android.domain.model.AccountAddress
import com.babylon.wallet.android.domain.model.FungibleToken
import com.babylon.wallet.android.domain.model.NonFungibleMetadataContainer
import com.babylon.wallet.android.domain.model.NonFungibleToken
import com.babylon.wallet.android.domain.model.NonFungibleTokenIdContainer
import com.babylon.wallet.android.domain.model.SimpleOwnedFungibleToken
import com.babylon.wallet.android.domain.model.SimpleOwnedNonFungibleToken
import java.math.BigDecimal

fun EntityDetailsResponse.toFungibleToken(): FungibleToken {
    return FungibleToken(
        address = address,
        totalSupply = BigDecimal(details.totalSupply),
        totalMinted = BigDecimal(details.totalMinted),
        totalBurnt = BigDecimal(details.totalBurnt),
        metadata = metadata.items.associate { it.key to it.value }
    )
}

fun FungibleResourcesCollection.toSimpleFungibleTokens(ownerAddress: String): List<SimpleOwnedFungibleToken> {
    return items.map { fungibleResourceItem ->
        SimpleOwnedFungibleToken(
            owner = AccountAddress(ownerAddress),
            amount = BigDecimal(fungibleResourceItem.amount.value),
            address = fungibleResourceItem.address
        )
    }
}

fun NonFungibleLocalIdsResponse.toDomainModel(): NonFungibleTokenIdContainer {
    return NonFungibleTokenIdContainer(ids = this.nonFungibleIds.items.map { it.nonFungibleId })
}

fun EntityDetailsResponse.toNonFungibleToken(
    nonFungibleTokenIdContainer: NonFungibleTokenIdContainer?
): NonFungibleToken {
    return NonFungibleToken(
        address = address,
        metadataContainer = metadata.toNonFungibleMetadataContainer(),
        nonFungibleIdContainer = nonFungibleTokenIdContainer
    )
}

fun EntityMetadataCollection.toNonFungibleMetadataContainer(): NonFungibleMetadataContainer {
    return NonFungibleMetadataContainer(
        metadata = items.associate { it.key to it.value },
        nextCursor = nextCursor,
        previousCursor = previousCursor
    )
}

fun NonFungibleResourcesCollection.toSimpleNonFungibleTokens(ownerAddress: String): List<SimpleOwnedNonFungibleToken> {
    return items.map { nftResource ->
        SimpleOwnedNonFungibleToken(
            owner = AccountAddress(ownerAddress),
            amount = BigDecimal(nftResource.amount),
            tokenResourceAddress = nftResource.address
        )
    }
}
