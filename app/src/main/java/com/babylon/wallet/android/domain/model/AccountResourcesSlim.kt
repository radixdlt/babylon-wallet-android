package com.babylon.wallet.android.domain.model

import com.babylon.wallet.android.data.gateway.generated.model.EntityResourcesResponse

data class AccountResourcesSlim(
    val address: String,
    val simpleFungibleTokens: List<SimpleOwnedFungibleToken> = emptyList(),
    val simpleNonFungibleTokens: List<SimpleOwnedNonFungibleToken> = emptyList()
) {
//    fun hasXrdToken(): Boolean {
//        return fungibleTokens.any { it.token.metadata[TokenMetadataConstants.KEY_SYMBOL] == TokenMetadataConstants.SYMBOL_XRD }
//    }
}

fun EntityResourcesResponse.toAccountResourceSlim(): AccountResourcesSlim {
    return AccountResourcesSlim(
        address = address,
        simpleFungibleTokens = fungibleResources.toSimpleFungibleTokens(address),
        simpleNonFungibleTokens = nonFungibleResources.toSimpleNonFungibleTokens(address)
    )
}
