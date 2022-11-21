package com.babylon.wallet.android.domain.model

import com.babylon.wallet.android.data.gateway.generated.model.EntityResourcesResponse
import com.babylon.wallet.android.data.gateway.toSimpleFungibleTokens
import com.babylon.wallet.android.data.gateway.toSimpleNonFungibleTokens

data class AccountResourcesSlim(
    val address: String,
    val simpleFungibleTokens: List<SimpleOwnedFungibleToken> = emptyList(),
    val simpleNonFungibleTokens: List<SimpleOwnedNonFungibleToken> = emptyList()
)

fun EntityResourcesResponse.toAccountResourceSlim(): AccountResourcesSlim {
    return AccountResourcesSlim(
        address = address,
        simpleFungibleTokens = fungibleResources.toSimpleFungibleTokens(address),
        simpleNonFungibleTokens = nonFungibleResources.toSimpleNonFungibleTokens(address)
    )
}
