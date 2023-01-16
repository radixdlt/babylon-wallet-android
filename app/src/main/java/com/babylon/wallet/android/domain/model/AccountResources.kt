package com.babylon.wallet.android.domain.model

import java.math.BigDecimal

data class AccountResources(
    val address: String,
    val displayName: String,
    val currencySymbol: String,
    val value: String,
    val fungibleTokens: List<OwnedFungibleToken> = emptyList(),
    val nonFungibleTokens: List<OwnedNonFungibleToken> = emptyList(),
    val appearanceID: Int,
) {
    fun hasXrdToken(): Boolean {
        return fungibleTokens.any {
            it.token.metadata[TokenMetadataConstants.KEY_SYMBOL] == TokenMetadataConstants.SYMBOL_XRD
        }
    }

    fun hasXrdWithBalance(): Boolean {
        return fungibleTokens.any {
            it.token.metadata[TokenMetadataConstants.KEY_SYMBOL] == TokenMetadataConstants.SYMBOL_XRD &&
                it.amount >= BigDecimal.ONE
        }
    }
}
