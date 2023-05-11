package com.babylon.wallet.android.domain.model

import rdx.works.profile.data.model.pernetwork.Network
import java.math.BigDecimal

data class AccountWithResources(
    val account: Network.Account,
    val resources: Resources?
) {

    val fungibleResources: List<Resource.FungibleResource>
        get() = resources?.fungibleResources ?: emptyList()

    val nonFungibleResources: List<Resource.NonFungibleResource>
        get() = resources?.nonFungibleResources ?: emptyList()

    fun hasXrd(minimumBalance: Long = 1L): Boolean {
        return fungibleResources.any {
            it.symbol == MetadataConstants.SYMBOL_XRD && it.amount >= BigDecimal(minimumBalance)
        }
    }
}

data class Resources(
    val fungibleResources: List<Resource.FungibleResource>,
    val nonFungibleResources: List<Resource.NonFungibleResource>,
)

fun List<AccountWithResources>.findAccountWithEnoughXRDBalance(minimumBalance: Long) = find {
    it.hasXrd(minimumBalance)
}
