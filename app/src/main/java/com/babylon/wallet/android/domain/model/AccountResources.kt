package com.babylon.wallet.android.domain.model

import rdx.works.profile.data.model.pernetwork.OnNetwork
import java.math.BigDecimal

data class AccountResources(
    val address: String,
    val displayName: String = "",
    val currencySymbol: String = "",
    val value: String = "",
    val fungibleTokens: List<OwnedFungibleToken> = emptyList(),
    val nonFungibleTokens: List<OwnedNonFungibleToken> = emptyList(),
    val appearanceID: Int,
    val isStub: Boolean = false
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

fun OnNetwork.Account.toDomainModel(): AccountResources {
    return AccountResources(
        address = this.address,
        displayName = displayName.orEmpty(),
        isStub = true,
        appearanceID = this.appearanceID
    )
}
