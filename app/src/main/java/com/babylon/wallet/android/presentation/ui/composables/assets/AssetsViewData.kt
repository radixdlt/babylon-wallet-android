package com.babylon.wallet.android.presentation.ui.composables.assets

import com.radixdlt.sargon.extensions.orZero
import com.radixdlt.sargon.extensions.sumOf
import rdx.works.core.domain.assets.Asset
import rdx.works.core.domain.assets.AssetPrice
import rdx.works.core.domain.assets.Assets
import rdx.works.core.domain.assets.FiatPrice
import rdx.works.core.domain.assets.NonFungibleCollection
import rdx.works.core.domain.assets.PoolUnit
import rdx.works.core.domain.assets.StakeSummary
import rdx.works.core.domain.assets.Token
import rdx.works.core.domain.assets.TokensPriceSorter
import rdx.works.core.domain.assets.ValidatorWithStakes

data class AssetsViewData(
    val epoch: Long?,
    val xrd: Token?,
    val nonXrdTokens: List<Token>,
    val nonFungibleCollections: List<NonFungibleCollection>,
    val poolUnits: List<PoolUnit>,
    val validatorsWithStakes: List<ValidatorWithStakes>,
    val prices: Map<Asset, AssetPrice?>?
) {

    val isTokensEmpty: Boolean
        get() = xrd == null && nonXrdTokens.isEmpty()

    val isNonFungibleCollectionsEmpty: Boolean
        get() = nonFungibleCollections.isEmpty()

    val isPoolUnitsEmpty: Boolean
        get() = poolUnits.isEmpty()

    val isValidatorWithStakesEmpty: Boolean
        get() = validatorsWithStakes.isEmpty()

    val oneXrdPrice: FiatPrice?
        get() = (
            prices?.values?.find {
                (it as? AssetPrice.LSUPrice)?.oneXrdPrice != null
            } as? AssetPrice.LSUPrice
            )?.oneXrdPrice ?: (
            prices?.values?.find {
                (it as? AssetPrice.StakeClaimPrice)?.oneXrdPrice != null
            } as? AssetPrice.StakeClaimPrice
            )?.oneXrdPrice

    val stakeSummary: StakeSummary? by lazy {
        if (epoch == null || validatorsWithStakes.any { !it.isDetailsAvailable }) return@lazy null

        StakeSummary(
            staked = validatorsWithStakes.sumOf { it.stakeValue().orZero() },
            unstaking = validatorsWithStakes.sumOf { validator ->
                validator.stakeClaimNft?.unstakingNFTs(epoch)?.sumOf { item -> item.claimAmountXrd.orZero() }.orZero()
            },
            readyToClaim = validatorsWithStakes.sumOf { validator ->
                validator.stakeClaimNft?.readyToClaimNFTs(epoch)?.sumOf { item -> item.claimAmountXrd.orZero() }.orZero()
            }
        )
    }

    companion object {

        fun from(
            assets: Assets?,
            prices: Map<Asset, AssetPrice?>?,
            epoch: Long?
        ): AssetsViewData? {
            if (assets == null) return null

            val validators =
                (assets.ownedLiquidStakeUnits.map { it.validator } + assets.ownedStakeClaims.map { it.validator }).toSet()
            val validatorsWithStakes = validators.mapNotNull { validator ->
                val lsu = assets.ownedLiquidStakeUnits.find { it.validator == validator }
                val claimCollection = assets.ownedStakeClaims.find { it.validator == validator }
                if (lsu == null && claimCollection == null) return@mapNotNull null

                ValidatorWithStakes(
                    validator = validator,
                    liquidStakeUnit = lsu,
                    stakeClaimNft = claimCollection
                )
            }

            return AssetsViewData(
                epoch = epoch,
                xrd = assets.ownedXrd,
                nonXrdTokens = assets.ownedNonXrdTokens.sortedWith(TokensPriceSorter(prices)),
                nonFungibleCollections = assets.ownedNonFungibles,
                poolUnits = assets.ownedPoolUnits,
                validatorsWithStakes = validatorsWithStakes,
                prices = prices
            )
        }
    }
}
