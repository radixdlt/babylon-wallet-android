package com.babylon.wallet.android.domain.model

import com.babylon.wallet.android.data.gateway.generated.model.EntityResourcesResponseNonFungibleResources
import java.math.BigDecimal

data class SimpleOwnedNonFungibleToken(
    val owner: AccountAddress,
    val amount: BigDecimal,
    val tokenResourceAddress: String
)

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
