package com.babylon.wallet.android.domain.model.assets

import android.net.Uri
import com.babylon.wallet.android.domain.model.resources.AccountDetails
import com.babylon.wallet.android.domain.model.resources.Resource
import com.babylon.wallet.android.domain.model.resources.isXrd
import com.babylon.wallet.android.domain.model.resources.metadata.AccountType
import com.babylon.wallet.android.domain.model.resources.metadata.Metadata
import com.babylon.wallet.android.domain.model.resources.metadata.description
import com.babylon.wallet.android.domain.model.resources.metadata.iconUrl
import com.babylon.wallet.android.domain.model.resources.metadata.name
import rdx.works.profile.data.model.pernetwork.Network
import java.math.BigDecimal

data class AccountWithAssets(
    val account: Network.Account,
    val details: AccountDetails? = null,
    val assets: Assets? = null,
) {

    val isDappDefinitionAccountType: Boolean
        get() = details?.accountType == AccountType.DAPP_DEFINITION
}

data class Assets(
    val fungibles: List<Resource.FungibleResource> = emptyList(),
    val nonFungibles: List<Resource.NonFungibleResource> = emptyList(),
    val poolUnits: List<PoolUnit> = emptyList(),
    val validatorsWithStakes: List<ValidatorWithStakes> = emptyList()
) {

    // Owned assets are assets that appear in the lists, but the user owns 0 amounts.
    // That is usually the case where a user may have sent all their NFTs from a collection
    // to another account. Still the collection is associated with the account, but the
    // user, on ui level, does not need to see it.
    // Also we don't filter those values out in the data layer since, we actually need this
    // information when for example we need to know what resources an account is familiar with
    // so we can apply the correct deposit rule warnings in transfer screen when the rule
    // is "Only accept known"

    val ownedXrd: Resource.FungibleResource? by lazy {
        fungibles.find { it.isXrd && it.ownedAmount != BigDecimal.ZERO }
    }
    val ownedNonXrdFungibles: List<Resource.FungibleResource> by lazy {
        fungibles.filterNot { it.isXrd || it.ownedAmount == BigDecimal.ZERO }
    }
    val ownedFungibles: List<Resource.FungibleResource> by lazy {
        ownedXrd?.let { listOf(it) + ownedNonXrdFungibles } ?: ownedNonXrdFungibles
    }

    val ownedNonFungibles: List<Resource.NonFungibleResource> by lazy {
        nonFungibles.filterNot { it.amount == 0L }
    }

    val ownedPoolUnits: List<PoolUnit> by lazy {
        poolUnits.filterNot { it.stake.ownedAmount == BigDecimal.ZERO }
    }

    val ownedValidatorsWithStakes: List<ValidatorWithStakes> by lazy {
        validatorsWithStakes.filterNot {
            (it.liquidStakeUnit == null || it.liquidStakeUnit.fungibleResource.ownedAmount == BigDecimal.ZERO) &&
                (it.stakeClaimNft == null || it.stakeClaimNft.nonFungibleResource.amount == 0L)
        }
    }

    // knownResources of an account is when
    // it contains a resource with an amount greater than 0
    // or it had a resource in the past but the amount is 0 now
    val knownResources: List<Resource> by lazy {
        fungibles + nonFungibles +
                poolUnits.map { it.stake } +
                validatorsWithStakes
                    .mapNotNull { it.liquidStakeUnit }
                    .map { it.fungibleResource } +
                validatorsWithStakes
                    .mapNotNull { it.stakeClaimNft }
                    .map { it.nonFungibleResource }
    }

    fun hasXrd(minimumBalance: BigDecimal = BigDecimal(1)): Boolean = ownedXrd?.let {
        it.ownedAmount?.let { amount ->
            amount >= minimumBalance
        }
    } == true

    fun fungiblesSize(): Int = ownedFungibles.size

    fun nonFungiblesSize(): Int = ownedNonFungibles.size

    fun validatorsWithStakesSize() = ownedValidatorsWithStakes.size

    fun poolUnitsSize(): Int = ownedPoolUnits.size

    fun stakeSummary(epoch: Long?): StakeSummary? {
        if (epoch == null || ownedValidatorsWithStakes.any { !it.isDetailsAvailable }) return null

        return StakeSummary(
            staked = ownedValidatorsWithStakes.sumOf { it.stakeValue() ?: BigDecimal.ZERO },
            unstaking = ownedValidatorsWithStakes.sumOf { validator ->
                validator.stakeClaimNft?.unstakingNFTs(epoch)?.sumOf { it.claimAmountXrd ?: BigDecimal.ZERO } ?: BigDecimal.ZERO
            },
            readyToClaim = ownedValidatorsWithStakes.sumOf { validator ->
                validator.stakeClaimNft?.readyToClaimNFTs(epoch)?.sumOf { it.claimAmountXrd ?: BigDecimal.ZERO } ?: BigDecimal.ZERO
            }
        )
    }
}

data class ValidatorDetail(
    val address: String,
    val totalXrdStake: BigDecimal?,
    val stakeUnitResourceAddress: String? = null,
    val claimTokenResourceAddress: String? = null,
    val metadata: List<Metadata> = emptyList()
) {
    val name: String
        get() = metadata.name().orEmpty()

    val url: Uri?
        get() = metadata.iconUrl()

    val description: String?
        get() = metadata.description()
}

data class ValidatorWithStakes(
    val validatorDetail: ValidatorDetail,
    val liquidStakeUnit: LiquidStakeUnit? = null,
    val stakeClaimNft: StakeClaim? = null
) {

    val isDetailsAvailable: Boolean
        get() = validatorDetail.totalXrdStake != null && liquidStakeUnit != null && liquidStakeUnit.fungibleResource.isDetailsAvailable &&
                (stakeClaimNft == null || stakeClaimNft.nonFungibleResource.amount.toInt() == stakeClaimNft.nonFungibleResource.items.size)

    val hasClaims: Boolean
        get() = (stakeClaimNft?.nonFungibleResource?.amount ?: 0) > 0L

    fun stakeValue(): BigDecimal? {
        if (validatorDetail.totalXrdStake == null) return null
        return liquidStakeUnit?.stakeValueInXRD(validatorDetail.totalXrdStake)
    }
}

data class StakeSummary(
    val staked: BigDecimal,
    val unstaking: BigDecimal,
    val readyToClaim: BigDecimal
) {
    val hasStakedValue: Boolean
        get() = staked > BigDecimal.ZERO

    val hasReadyToClaimValue: Boolean
        get() = readyToClaim > BigDecimal.ZERO
}
