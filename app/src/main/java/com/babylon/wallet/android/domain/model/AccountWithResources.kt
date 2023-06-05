package com.babylon.wallet.android.domain.model

import rdx.works.profile.data.model.pernetwork.Network
import java.math.BigDecimal

data class AccountWithResources(
    val account: Network.Account,
    val resources: Resources?
)

data class Resources(
    val fungibleResources: List<Resource.FungibleResource>,
    val nonFungibleResources: List<Resource.NonFungibleResource>,
) {

    val xrd: Resource.FungibleResource? = fungibleResources.find { it.isXrd }
    val nonXrdFungibles: List<Resource.FungibleResource> = fungibleResources.filterNot { it.isXrd }

    fun hasXrd(minimumBalance: Long = 1L): Boolean = xrd?.let { it.amount >= BigDecimal(minimumBalance) } == true

    companion object {
        val EMPTY = Resources(fungibleResources = emptyList(), nonFungibleResources = emptyList())
    }
}

fun List<AccountWithResources>.findAccountWithEnoughXRDBalance(minimumBalance: Long) = find {
    it.resources?.hasXrd(minimumBalance) ?: false
}

fun List<Resource.NonFungibleResource>.allNftItemsSize() = map { it.items }.flatten().size
