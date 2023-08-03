package com.babylon.wallet.android.domain.model

import com.babylon.wallet.android.domain.model.metadata.AccountTypeMetadataItem
import rdx.works.profile.data.model.pernetwork.Network
import java.math.BigDecimal

data class AccountWithResources(
    val account: Network.Account,
    private val accountTypeMetadataItem: AccountTypeMetadataItem? = null,
    val resources: Resources?,
) {

    val isDappDefinitionAccountType: Boolean
        get() = accountTypeMetadataItem?.type == AccountTypeMetadataItem.AccountType.DAPP_DEFINITION
}

data class Resources(
    val fungibleResources: List<Resource.FungibleResource>,
    val nonFungibleResources: List<Resource.NonFungibleResource>,
) {

    val xrd: Resource.FungibleResource? = fungibleResources.find { it.isXrd }
    val nonXrdFungibles: List<Resource.FungibleResource> = fungibleResources.filterNot { it.isXrd }

    val isNotEmpty: Boolean
        get() = fungibleResources.isNotEmpty() || nonFungibleResources.isNotEmpty()

    fun hasXrd(minimumBalance: BigDecimal = BigDecimal(1)): Boolean = xrd?.let {
        it.amount?.let { amount ->
            amount >= minimumBalance
        }
    } == true

    companion object {
        val EMPTY = Resources(fungibleResources = emptyList(), nonFungibleResources = emptyList())
    }
}

fun List<AccountWithResources>.findAccountWithEnoughXRDBalance(minimumBalance: BigDecimal) = find {
    it.resources?.hasXrd(minimumBalance) ?: false
}

fun List<Resource.NonFungibleResource>.allNftItemsSize() = map { it.items }.flatten().size
