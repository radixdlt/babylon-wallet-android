package com.babylon.wallet.android.domain.model

import android.net.Uri
import com.babylon.wallet.android.domain.model.metadata.AccountTypeMetadataItem
import rdx.works.profile.data.model.pernetwork.Network
import java.math.BigDecimal

data class AccountWithAssets(
    val account: Network.Account,
    private val accountTypeMetadataItem: AccountTypeMetadataItem? = null,
    val assets: Assets?,
) {

    val isDappDefinitionAccountType: Boolean
        get() = accountTypeMetadataItem?.type == AccountTypeMetadataItem.AccountType.DAPP_DEFINITION
}

fun List<AccountWithAssets>.findAccountWithEnoughXRDBalance(minimumBalance: BigDecimal) = find {
    it.assets?.hasXrd(minimumBalance) ?: false
}

data class Assets(
    val fungibles: List<Resource.FungibleResource> = emptyList(),
    val nonFungibles: List<Resource.NonFungibleResource> = emptyList(),
    val poolUnits: List<Resource.PoolUnitResource> = emptyList(),
    val validatorsWithStakeResources: ValidatorsWithStakeResources = ValidatorsWithStakeResources()
) {

    val xrd: Resource.FungibleResource? by lazy {
        fungibles.find { it.isXrd }
    }
    val nonXrdFungibles: List<Resource.FungibleResource> by lazy {
        fungibles.filterNot { it.isXrd }
    }

    fun hasXrd(minimumBalance: BigDecimal = BigDecimal(1)): Boolean = xrd?.let {
        it.ownedAmount?.let { amount ->
            amount >= minimumBalance
        }
    } == true

    fun poolUnitsSize(): Int {
        return poolUnits.size + validatorsWithStakeResources.validators.size
    }
}

data class Resources(
    val fungibleResources: List<Resource.FungibleResource>,
    val nonFungibleResources: List<Resource.NonFungibleResource>,
    val poolUnits: List<Resource.PoolUnitResource>,
    val validatorsWithStakeResources: ValidatorsWithStakeResources = ValidatorsWithStakeResources()
) {

    val xrd: Resource.FungibleResource? by lazy {
        fungibleResources.find { it.isXrd }
    }
    val nonXrdFungibles: List<Resource.FungibleResource> by lazy {
        fungibleResources.filterNot { it.isXrd }
    }

    val isNotEmpty: Boolean
        get() = fungibleResources.isNotEmpty() || nonFungibleResources.isNotEmpty()

    fun hasXrd(minimumBalance: BigDecimal = BigDecimal(1)): Boolean = xrd?.let {
        it.ownedAmount?.let { amount ->
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

data class ValidatorDetail(
    val address: String,
    val name: String,
    val url: Uri?,
    val description: String?,
    val totalXrdStake: BigDecimal?
)

data class ValidatorWithStakeResources(
    val validatorDetail: ValidatorDetail,
    val liquidStakeUnits: List<Resource.LiquidStakeUnitResource> = emptyList(),
    val stakeClaimNft: Resource.StakeClaimResource? = null
)

fun List<Resource.NonFungibleResource>.allNftItemsSize() = map { it.items }.flatten().size
