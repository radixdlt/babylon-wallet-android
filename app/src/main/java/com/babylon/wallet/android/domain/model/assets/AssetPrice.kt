package com.babylon.wallet.android.domain.model.assets

import android.icu.number.NumberFormatter
import android.icu.text.NumberFormat
import android.icu.util.Currency
import android.os.Build
import com.babylon.wallet.android.domain.model.resources.Resource
import java.math.BigDecimal
import java.util.Locale

data class FiatPrice(
    val price: Double,
    val currency: SupportedCurrency
) {

    val formatted: String
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            NumberFormatter.with()
                .unit(Currency.getInstance(currency.name))
                .locale(Locale.getDefault())
                .format(price)
                .toString()
        } else {
            val javaCurrency = Currency.getInstance(currency.name)
            NumberFormat.getCurrencyInstance().apply {
                currency = javaCurrency
            }.format(price)
        }

}

sealed class AssetPrice {
    abstract val asset: Asset
    abstract val price: FiatPrice?

    data class TokenPrice(
        override val asset: Token,
        override val price: FiatPrice?,
    ) : AssetPrice() {
        val priceFormatted: String?
            get() = price?.formatted
    }

    data class LSUPrice(
        override val asset: LiquidStakeUnit,
        override val price: FiatPrice?,
        val oneXrdPrice: FiatPrice?
    ) : AssetPrice() {
        val lsuPriceFormatted: String?
            get() = price?.formatted

        fun xrdPriceFormatted(xrdBalance: BigDecimal): String? = oneXrdPrice?.let {
            it.copy(price = (it.price.toBigDecimal() * xrdBalance).toDouble()).formatted
        }
    }

    data class StakeClaimPrice(
        override val asset: StakeClaim,
        val prices: Map<Resource.NonFungibleResource.Item, FiatPrice?>,
        private val currency: SupportedCurrency
    ) : AssetPrice() {
        override val price: FiatPrice?
            get() {
                val areAllPricesNull = prices.values.all { price -> price == null }
                if (areAllPricesNull) {
                    return null
                }

                return FiatPrice(
                    price = prices.values.sumOf { fiatPrice ->
                        fiatPrice?.price ?: 0.0
                    },
                    currency = currency
                )
            }

        fun xrdPriceFormatted(item: Resource.NonFungibleResource.Item): String? = prices[item]?.formatted
    }

    data class PoolUnitPrice(
        override val asset: PoolUnit,
        val prices: Map<Resource.FungibleResource, FiatPrice?>,
        private val currency: SupportedCurrency
    ) : AssetPrice() {
        override val price: FiatPrice?
            get() {
                val areAllPricesNull = prices.values.all { price -> price == null }
                if (areAllPricesNull) {
                    return null
                }

                return FiatPrice(
                    price = prices.values.sumOf { fiatPrice ->
                        fiatPrice?.price ?: 0.0
                    },
                    currency = currency
                )
            }

        fun xrdPriceFormatted(resource: Resource.FungibleResource): String? = prices[resource]?.formatted
    }
}
