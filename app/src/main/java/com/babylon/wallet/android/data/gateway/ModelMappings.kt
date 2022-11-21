package com.babylon.wallet.android.data.gateway

import com.babylon.wallet.android.data.gateway.generated.model.EntityDetailsResponse
import com.babylon.wallet.android.data.gateway.generated.model.EntityResourcesResponse
import com.babylon.wallet.android.data.gateway.generated.model.EntityResourcesResponseFungibleResources
import com.babylon.wallet.android.data.gateway.generated.model.EntityResourcesResponseNonFungibleResources
import com.babylon.wallet.android.domain.model.AccountAddress
import com.babylon.wallet.android.domain.model.AccountResourcesSlim
import com.babylon.wallet.android.domain.model.FungibleToken
import com.babylon.wallet.android.domain.model.NonFungibleToken
import com.babylon.wallet.android.domain.model.SimpleOwnedFungibleToken
import com.babylon.wallet.android.domain.model.SimpleOwnedNonFungibleToken
import java.math.BigDecimal

fun EntityDetailsResponse.toFungibleToken(): FungibleToken {
    return FungibleToken(
        address = address,
        totalSupply = BigDecimal(details.totalSupply?.value),
        totalMinted = BigDecimal(details.totalMinted?.value),
        totalBurnt = BigDecimal(details.totalBurnt?.value),
        metadata = metadata.items.associate { it.key to it.value }
    )
}

fun EntityResourcesResponse.toAccountResourceSlim(): AccountResourcesSlim {
    return AccountResourcesSlim(
        address = address,
        simpleFungibleTokens = fungibleResources.toSimpleFungibleTokens(address),
        simpleNonFungibleTokens = nonFungibleResources.toSimpleNonFungibleTokens(address)
    )
}

fun EntityDetailsResponse.toNonFungibleToken(): NonFungibleToken {
    return NonFungibleToken(
        address = address,
    )
}

fun EntityResourcesResponseFungibleResources.toSimpleFungibleTokens(
    ownerAddress: String
): List<SimpleOwnedFungibleToken> {
    return items.map { fungibleResourceItem ->
        SimpleOwnedFungibleToken(
            owner = AccountAddress(ownerAddress),
            amount = BigDecimal(fungibleResourceItem.amount.value),
            address = fungibleResourceItem.address
        )
    }
}

fun EntityResourcesResponseNonFungibleResources.toSimpleNonFungibleTokens(
    ownerAddress: String
): List<SimpleOwnedNonFungibleToken> {
    return items.map { nftResource ->
        SimpleOwnedNonFungibleToken(
            owner = AccountAddress(ownerAddress),
            amount = nftResource.amount,
            tokenResourceAddress = nftResource.address
        )
    }
}
