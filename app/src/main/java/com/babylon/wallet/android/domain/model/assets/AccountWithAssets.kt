package com.babylon.wallet.android.domain.model.assets

import android.net.Uri
import com.babylon.wallet.android.domain.model.resources.AccountDetails
import com.babylon.wallet.android.domain.model.resources.Resource
import com.babylon.wallet.android.domain.model.resources.isXrd
import com.babylon.wallet.android.domain.model.resources.metadata.AccountTypeMetadataItem
import rdx.works.profile.data.model.pernetwork.Network
import java.math.BigDecimal

data class AccountWithAssets(
    val account: Network.Account,
    val details: AccountDetails = AccountDetails(),
    val assets: Assets? = null,
) {

    val isDappDefinitionAccountType: Boolean
        get() = details.type == AccountTypeMetadataItem.AccountType.DAPP_DEFINITION
}

fun List<AccountWithAssets>.findAccountWithEnoughXRDBalance(minimumBalance: BigDecimal) = find {
    it.assets?.hasXrd(minimumBalance) ?: false
}

data class Assets(
    val fungibles: List<Resource.FungibleResource> = emptyList(),
    val nonFungibles: List<Resource.NonFungibleResource> = emptyList(),
    val poolUnits: List<PoolUnit> = emptyList(),
    val validatorsWithStakeResources: List<ValidatorWithStakeResources> = emptyList()
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
        return poolUnits.size + validatorsWithStakeResources.size
    }
}

data class ValidatorDetail(
    val address: String,
    val name: String,
    val url: Uri?,
    val description: String?,
    val totalXrdStake: BigDecimal?,
    val stakeUnitResourceAddress: String? = null,
    val claimTokenResourceAddress: String? = null
)

data class ValidatorWithStakeResources(
    val validatorDetail: ValidatorDetail,
    val liquidStakeUnits: List<LiquidStakeUnit> = emptyList(),
    val stakeClaimNft: StakeClaim? = null
)

fun List<Resource.NonFungibleResource>.allNftItemsSize() = sumOf { it.amount }
