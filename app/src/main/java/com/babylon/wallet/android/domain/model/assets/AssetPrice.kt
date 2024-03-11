package com.babylon.wallet.android.domain.model.assets

import com.babylon.wallet.android.domain.model.resources.Resource
import java.math.BigDecimal

sealed interface AssetPrice {
    val asset: Asset
    val price: BigDecimal?

    data class TokenPrice(
        override val asset: Token,
        override val price: BigDecimal?
    ) : AssetPrice

    data class LSUPrice(
        override val asset: LiquidStakeUnit,
        override val price: BigDecimal?
    ) : AssetPrice

    data class StakeClaimPrice(
        override val asset: StakeClaim,
        override val price: BigDecimal?
    ) : AssetPrice

    data class PoolUnitPrice(
        override val asset: PoolUnit,
        val prices: Map<Resource.FungibleResource, BigDecimal?>
    ) : AssetPrice {
        override val price: BigDecimal?
            get() {
                val areAllNull = prices.values.all {
                    it == null
                }
                if (areAllNull) {
                    return null
                }
                return prices.values.sumOf {
                    it ?: BigDecimal.ZERO
                }
            }
    }
}
