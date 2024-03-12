package com.babylon.wallet.android.domain.model.assets

import com.babylon.wallet.android.domain.model.resources.Resource
import com.babylon.wallet.android.utils.toLocaleNumberFormat
import java.math.BigDecimal
import java.util.Currency

sealed class AssetPrice {
    abstract val asset: Asset
    abstract val price: BigDecimal?
    abstract val currencyCode: String?

    val currency: Currency?
        get() = currencyCode?.let { Currency.getInstance(currencyCode) }

    protected fun priceWithCurrency(price: BigDecimal?): String? {
        val symbol = currency?.symbol

        return price?.let { amount ->
            "${symbol.orEmpty()}${amount.toLocaleNumberFormat()}"
        }
    }

    data class TokenPrice(
        override val asset: Token,
        override val price: BigDecimal?,
        override val currencyCode: String?
    ) : AssetPrice() {
        val priceFormatted: String?
            get() = priceWithCurrency(price)
    }

    data class LSUPrice(
        override val asset: LiquidStakeUnit,
        override val price: BigDecimal?,
        override val currencyCode: String?
    ) : AssetPrice() {
        val priceFormatted: String?
            get() = priceWithCurrency(price)
    }

    data class StakeClaimPrice(
        override val asset: StakeClaim,
        override val price: BigDecimal?,
        override val currencyCode: String?
    ) : AssetPrice() {
        val priceFormatted: String?
            get() = priceWithCurrency(price)
    }

    data class PoolUnitPrice(
        override val asset: PoolUnit,
        val prices: Map<Resource.FungibleResource, BigDecimal?>,
        override val currencyCode: String?
    ) : AssetPrice() {
        override val price: BigDecimal?
            get() {
                val areAllPricesNull = prices.values.all { price -> price == null }
                if (areAllPricesNull) {
                    return null
                }
                return prices.values.sumOf { price ->
                    price ?: BigDecimal.ZERO
                }
            }

        fun priceFormatted(resource: Resource.FungibleResource): String? {
            val fiatPrice = prices[resource]
            return priceWithCurrency(fiatPrice)
        }
    }
}
