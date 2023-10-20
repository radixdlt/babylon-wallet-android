package com.babylon.wallet.android.domain.model.assets

import android.net.Uri
import com.babylon.wallet.android.domain.model.resources.metadata.AccountTypeMetadataItem
import com.babylon.wallet.android.domain.model.resources.Resource
import com.babylon.wallet.android.domain.model.resources.isXrd
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
    val poolUnits: List<PoolUnit> = emptyList(),
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
    val liquidStakeUnits: List<LiquidStakeUnit> = emptyList(),
    val stakeClaimNft: StakeClaim? = null
)

fun List<Resource.NonFungibleResource>.allNftItemsSize() = map { it.items }.flatten().size
