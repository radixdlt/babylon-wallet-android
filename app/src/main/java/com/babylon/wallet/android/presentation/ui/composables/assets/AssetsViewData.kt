package com.babylon.wallet.android.presentation.ui.composables.assets

import com.babylon.wallet.android.domain.model.assets.Asset
import com.babylon.wallet.android.domain.model.assets.AssetPrice
import com.babylon.wallet.android.domain.model.assets.Assets
import com.babylon.wallet.android.domain.model.assets.NonFungibleCollection
import com.babylon.wallet.android.domain.model.assets.PoolUnit
import com.babylon.wallet.android.domain.model.assets.StakeSummary
import com.babylon.wallet.android.domain.model.assets.Token
import com.babylon.wallet.android.domain.model.assets.TokensPriceSorter
import com.babylon.wallet.android.domain.model.assets.ValidatorWithStakes
import java.math.BigDecimal

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

    val stakeSummary: StakeSummary? by lazy {
        if (epoch == null || validatorsWithStakes.any { !it.isDetailsAvailable }) return@lazy null

        StakeSummary(
            staked = validatorsWithStakes.sumOf { it.stakeValue() ?: BigDecimal.ZERO },
            unstaking = validatorsWithStakes.sumOf { validator ->
                validator.stakeClaimNft?.unstakingNFTs(epoch)?.sumOf { it.claimAmountXrd ?: BigDecimal.ZERO } ?: BigDecimal.ZERO
            },
            readyToClaim = validatorsWithStakes.sumOf { validator ->
                validator.stakeClaimNft?.readyToClaimNFTs(epoch)?.sumOf { it.claimAmountXrd ?: BigDecimal.ZERO } ?: BigDecimal.ZERO
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

            val validators = (assets.ownedLiquidStakeUnits.map { it.validator } + assets.ownedStakeClaims.map { it.validator }).toSet()
            val validatorsWithStakes = validators.mapNotNull { validator ->
                val lsu = assets.ownedLiquidStakeUnits.find { it.validator == validator }
                val claimCollection = assets.ownedStakeClaims.find { it.validator == validator }
                if (lsu == null && claimCollection == null) return@mapNotNull null

                ValidatorWithStakes(
                    validatorDetail = validator,
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
