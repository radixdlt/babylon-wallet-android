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

sealed interface Asset {

    val resource: Resource

    // Asset that can have an amount like
    // - token
    // - LSU
    // - pool unit
    sealed interface Fungible : Asset {
        override val resource: Resource.FungibleResource
    }

    // Asset that is non fungible and needs a local id from a collection
    // - NFT
    // - stake claim
    sealed interface NonFungible : Asset {
        override val resource: Resource.NonFungibleResource
    }
}

data class Assets(
    val tokens: List<Token> = emptyList(),
    val nonFungibles: List<NonFungibleCollection> = emptyList(),
    val poolUnits: List<PoolUnit> = emptyList(),
    val liquidStakeUnits: List<LiquidStakeUnit> = emptyList(),
    val stakeClaims: List<StakeClaim> = emptyList()
) {

    // Owned assets are assets that appear in the lists, but the user owns 0 amounts.
    // That is usually the case where a user may have sent all their NFTs from a collection
    // to another account. Still the collection is associated with the account, but the
    // user, on ui level, does not need to see it.
    // Also we don't filter those values out in the data layer since, we actually need this
    // information when for example we need to know what resources an account is familiar with
    // so we can apply the correct deposit rule warnings in transfer screen when the rule
    // is "Only accept known"

    val ownedXrd: Token? by lazy {
        tokens.find { it.resource.isXrd && it.resource.ownedAmount != BigDecimal.ZERO }
    }
    val ownedNonXrdTokens: List<Token> by lazy {
        tokens.filterNot { it.resource.isXrd || it.resource.ownedAmount == BigDecimal.ZERO }
    }
    val ownedTokens: List<Token> by lazy {
        ownedXrd?.let { listOf(it) + ownedNonXrdTokens } ?: ownedNonXrdTokens
    }

    val ownedNonFungibles: List<NonFungibleCollection> by lazy {
        nonFungibles.filterNot { it.collection.amount == 0L }
    }

    val ownedLiquidStakeUnits: List<LiquidStakeUnit> by lazy {
        liquidStakeUnits.filter { it.fungibleResource.ownedAmount != BigDecimal.ZERO }
    }

    val ownedStakeClaims: List<StakeClaim> by lazy {
        stakeClaims.filter { it.nonFungibleResource.amount > 0L }
    }

    val ownedPoolUnits: List<PoolUnit> by lazy {
        poolUnits.filterNot { it.stake.ownedAmount == BigDecimal.ZERO }
    }

    // knownResources of an account is when
    // it contains a resource with an amount greater than 0
    // or it had a resource in the past but the amount is 0 now
    val knownResources: List<Resource> by lazy {
        tokens.map { it.resource } +
            nonFungibles.map { it.collection } +
            poolUnits.map { it.stake } +
            liquidStakeUnits.map { it.fungibleResource } +
            stakeClaims.map { it.nonFungibleResource }
    }

    val ownedAssets: List<Asset> by lazy {
        ownedTokens + ownedNonFungibles + ownedPoolUnits + ownedLiquidStakeUnits + ownedStakeClaims
    }

    val ownsAnyAssetsThatContributeToBalance: Boolean by lazy {
        ownedTokens.isNotEmpty() || ownedPoolUnits.isNotEmpty() || ownedLiquidStakeUnits.isNotEmpty() || ownedStakeClaims.isNotEmpty()
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
        get() = validatorDetail.totalXrdStake != null &&
            (liquidStakeUnit == null || liquidStakeUnit.fungibleResource.isDetailsAvailable) &&
            (stakeClaimNft == null || stakeClaimNft.nonFungibleResource.amount.toInt() == stakeClaimNft.nonFungibleResource.items.size)

    val hasLSU: Boolean
        get() = liquidStakeUnit != null && (liquidStakeUnit.fungibleResource.ownedAmount ?: BigDecimal.ZERO) > BigDecimal.ZERO

    val hasClaims: Boolean
        get() = stakeClaimNft != null && stakeClaimNft.nonFungibleResource.amount > 0L

    fun stakeValue(): BigDecimal? {
        if (validatorDetail.totalXrdStake == null) return null
        return liquidStakeUnit?.stakeValueInXRD(validatorDetail.totalXrdStake)
    }

    companion object {
        fun from(
            liquidStakeUnits: List<LiquidStakeUnit>,
            stakeClaims: List<StakeClaim>
        ): List<ValidatorWithStakes> {
            val validators = (liquidStakeUnits.map { it.validator } + stakeClaims.map { it.validator }).toSet()

            return validators.mapNotNull { validator ->
                val lsu = liquidStakeUnits.find { it.validator == validator }
                val claimCollection = stakeClaims.find { it.validator == validator }
                if (lsu == null && claimCollection == null) return@mapNotNull null

                ValidatorWithStakes(
                    validatorDetail = validator,
                    liquidStakeUnit = lsu,
                    stakeClaimNft = claimCollection
                )
            }
        }
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
