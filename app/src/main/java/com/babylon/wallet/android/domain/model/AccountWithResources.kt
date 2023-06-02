package com.babylon.wallet.android.domain.model

import rdx.works.profile.data.model.pernetwork.Network
import java.math.BigDecimal

data class AccountWithResources(
    val account: Network.Account,
    val resources: Resources?
) {

    val fungibleResources: List<Resource.FungibleResource>
        get() = resources?.fungibleResources.orEmpty()

    val nonFungibleResources: List<Resource.NonFungibleResource>
        get() = resources?.nonFungibleResources.orEmpty()

    fun hasXrd(minimumBalance: Long = 1L): Boolean {
        return fungibleResources.any {
            it.symbol == MetadataConstants.SYMBOL_XRD && it.amount >= BigDecimal(minimumBalance)
        }
    }
}

data class Resources(
    val fungibleResources: List<Resource.FungibleResource>,
    val nonFungibleResources: List<Resource.NonFungibleResource>,
) {

    companion object {
        val EMPTY = Resources(fungibleResources = emptyList(), nonFungibleResources = emptyList())
    }
}

fun List<AccountWithResources>.findAccountWithEnoughXRDBalance(minimumBalance: Long) = find {
    it.hasXrd(minimumBalance)
}

fun List<Resource.NonFungibleResource>.allNftItemsSize() = map { it.items }.flatten().size
