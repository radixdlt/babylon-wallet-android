package com.babylon.wallet.android.domain.model

import rdx.works.profile.data.model.pernetwork.Network
import java.math.BigDecimal

data class AccountResources(
    val address: String,
    val displayName: String,
    val appearanceID: Int,
    val isOlympiaAccount: Boolean = false,
    val fungibleTokens: List<OwnedFungibleToken> = emptyList(),
    val nonFungibleTokens: List<OwnedNonFungibleToken> = emptyList()
) {
    fun hasXrdToken(): Boolean {
        return fungibleTokens.any {
            it.token.metadata[MetadataConstants.KEY_SYMBOL] == MetadataConstants.SYMBOL_XRD
        }
    }

    fun hasXrdWithEnoughBalance(minimumBalance: Long): Boolean {
        return fungibleTokens.any {
            it.token.metadata[MetadataConstants.KEY_SYMBOL] == MetadataConstants.SYMBOL_XRD &&
                it.amount >= BigDecimal(minimumBalance)
        }
    }
}

fun List<AccountResources>.findAccountWithEnoughXRDBalance(minimumBalance: Long) = find {
    it.hasXrdWithEnoughBalance(minimumBalance)
}

fun Network.Account.toDomainModel(): AccountResources {
    return AccountResources(
        address = this.address,
        displayName = displayName,
        appearanceID = this.appearanceID,
        isOlympiaAccount = isOlympiaAccount()
    )
}
