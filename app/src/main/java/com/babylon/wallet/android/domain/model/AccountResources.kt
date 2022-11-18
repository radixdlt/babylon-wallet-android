package com.babylon.wallet.android.domain.model

data class AccountResources(
    val address: String,
    val fungibleTokens: List<OwnedFungibleToken> = emptyList(),
    val nonFungibleTokens: List<OwnedNonFungibleToken> = emptyList()
) {
    fun hasXrdToken(): Boolean {
        return fungibleTokens.any { it.token.metadata[TokenMetadataConstants.KEY_SYMBOL] == TokenMetadataConstants.SYMBOL_XRD }
    }
}
