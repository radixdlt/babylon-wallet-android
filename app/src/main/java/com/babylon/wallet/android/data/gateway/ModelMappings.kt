package com.babylon.wallet.android.data.gateway

import com.babylon.wallet.android.data.gateway.generated.models.EntityDetailsResponse
import com.babylon.wallet.android.data.gateway.generated.models.EntityMetadataCollection
import com.babylon.wallet.android.data.gateway.generated.models.FungibleResourcesCollection
import com.babylon.wallet.android.data.gateway.generated.models.NonFungibleIdsResponse
import com.babylon.wallet.android.data.gateway.generated.models.NonFungibleResourcesCollection
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
        totalSupply = BigDecimal(details?.totalSupply ?: "0.0"), // TODO 1181
        totalMinted = BigDecimal(details?.totalMinted ?: "0.0"),
        totalBurnt = BigDecimal(details?.totalBurnt ?: "0.0"),
        metadata = mapOf()
//        metadata.items.associate { entityMetadataItem ->
//            entityMetadataItem.key to entityMetadataItem.value
//        }
    )
}

fun FungibleResourcesCollection.toSimpleFungibleTokens(ownerAddress: String): List<SimpleOwnedFungibleToken> {
    return items.map { fungibleResourceItem ->
        SimpleOwnedFungibleToken(
            owner = AccountAddress(ownerAddress),
            amount = BigDecimal.ZERO,//BigDecimal(fungibleResourceItem.amount.value),
            address = ""//fungibleResourceItem.address
        )
    }
}

fun NonFungibleIdsResponse.toDomainModel(): NonFungibleTokenIdContainer {
    return NonFungibleTokenIdContainer(
        ids = nonFungibleIds.items.map { nonFungibleIdsCollectionItem ->
            nonFungibleIdsCollectionItem.nonFungibleId
        }
    )
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
        metadata = mapOf(),
//        items.associate { entityMetadataItem ->
//            entityMetadataItem.key to entityMetadataItem.value
//        },
        nextCursor = nextCursor,
        previousCursor = previousCursor
    )
}

fun NonFungibleResourcesCollection.toSimpleNonFungibleTokens(ownerAddress: String): List<SimpleOwnedNonFungibleToken> {
    return items.map { nftResource ->
        SimpleOwnedNonFungibleToken(
            owner = AccountAddress(ownerAddress),
            amount = BigDecimal.ZERO,//BigDecimal(nftResource.amount),
            tokenResourceAddress = ""//nftResource.address
        )
    }
}
