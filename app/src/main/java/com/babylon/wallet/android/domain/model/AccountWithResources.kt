package com.babylon.wallet.android.domain.model

import android.net.Uri
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
    val poolUnits: List<Resource.PoolUnitResource>,
    val validatorsWithStakeResources: ValidatorsWithStakeResources = ValidatorsWithStakeResources()
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

    fun poolUnitsSize(): Int {
        return poolUnits.size + validatorsWithStakeResources.validators.size
    }

    companion object {
        val EMPTY = Resources(
            fungibleResources = emptyList(),
            nonFungibleResources = emptyList(),
            poolUnits = emptyList(),
            validatorsWithStakeResources = ValidatorsWithStakeResources()
        )
    }
}

data class ValidatorsWithStakeResources(
    val validators: List<ValidatorWithStakeResources> = emptyList()
) {
    val isEmpty
        get() = validators.isEmpty()
}

data class ValidatorWithStakeResources(
    val address: String,
    val name: String,
    val url: Uri?,
    val totalXrdStake: BigDecimal?,
    val liquidStakeUnits: List<Resource.LiquidStakeUnitResource> = emptyList(),
    val stakeClaimNft: Resource.StakeClaimResource? = null
) {
    fun stakeValueInXRD(lsuAddress: String): BigDecimal? {
        val lsuPercentageOwned = liquidStakeUnits.find { it.resourceAddress == lsuAddress }?.percentageOwned
        return lsuPercentageOwned?.multiply(totalXrdStake)
    }
}

fun List<AccountWithResources>.findAccountWithEnoughXRDBalance(minimumBalance: BigDecimal) = find {
    it.resources?.hasXrd(minimumBalance) ?: false
}

fun List<Resource.NonFungibleResource>.allNftItemsSize() = map { it.items }.flatten().size
