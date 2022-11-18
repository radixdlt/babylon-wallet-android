package com.babylon.wallet.android.domain.model

import com.babylon.wallet.android.data.gateway.generated.model.EntityResourcesResponseFungibleResources
import java.math.BigDecimal

data class SimpleOwnedFungibleToken(
    val owner: AccountAddress,
    val amount: BigDecimal,
    val address: String,
)

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
