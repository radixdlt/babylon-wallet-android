package com.babylon.wallet.android.domain.model.assets

import android.icu.number.NumberFormatter
import android.icu.number.Precision
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

    operator fun times(value: BigDecimal): FiatPrice = times(value.toDouble())

    operator fun times(value: Double): FiatPrice = FiatPrice(
        price = price * value,
        currency = currency
    )

    val formatted: String
        get() {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                NumberFormatter.with()
                    .unit(Currency.getInstance(currency.name))
                    .locale(Locale.getDefault())
                    .let {
                        if (price < 1.0 && price != 0.0) {
                            it.precision(Precision.fixedFraction(FRACTION_PLACES))
                        } else {
                            it
                        }
                    }
                    .format(price)
                    .toString()
            } else {
                val javaCurrency = Currency.getInstance(currency.name)
                NumberFormat.getCurrencyInstance().apply {
                    currency = javaCurrency
                    if (price < 1.0 && price != 0.0) {
                        maximumFractionDigits = MAX_FRACTION_DIGITS
                    }
                }.format(price)
            }
        }

    val formattedWithoutCurrency: String
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            NumberFormatter.with()
                .locale(Locale.getDefault())
                .format(price)
                .toString()
        } else {
            NumberFormat.getNumberInstance(Locale.getDefault()).format(price)
        }

    companion object {
        private const val MAX_FRACTION_DIGITS = 5
        private const val FRACTION_PLACES = 5
    }
}

sealed class AssetPrice {
    abstract val asset: Asset
    abstract val price: FiatPrice?

    data class TokenPrice(
        override val asset: Token,
        override val price: FiatPrice?,
    ) : AssetPrice()

    data class LSUPrice(
        override val asset: LiquidStakeUnit,
        override val price: FiatPrice?,
        val oneXrdPrice: FiatPrice?
    ) : AssetPrice() {
        fun xrdPrice(xrdBalance: BigDecimal): FiatPrice? = oneXrdPrice?.let { it * xrdBalance }
    }

    data class StakeClaimPrice(
        override val asset: StakeClaim,
        val prices: Map<Resource.NonFungibleResource.Item, FiatPrice?>,
        private val currency: SupportedCurrency,
        val oneXrdPrice: FiatPrice?
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

        fun xrdPrice(item: Resource.NonFungibleResource.Item): FiatPrice? = prices[item]
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

        fun xrdPrice(resource: Resource.FungibleResource): FiatPrice? = prices[resource]
    }
}
