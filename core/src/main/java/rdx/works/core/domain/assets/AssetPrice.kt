package rdx.works.core.domain.assets

import android.icu.number.NumberFormatter
import android.icu.number.Precision
import android.icu.text.NumberFormat
import android.icu.util.Currency
import android.os.Build
import com.radixdlt.sargon.Decimal192
import rdx.works.core.domain.resources.Resource
import rdx.works.core.domain.toDouble
import java.util.Locale

data class FiatPrice(
    val price: Double,
    val currency: SupportedCurrency
) {

    operator fun times(value: Decimal192): FiatPrice = times(value.toDouble())

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
                    .let { localizedNumberFormatter ->
                        if (price == 0.0) {
                            localizedNumberFormatter.precision(Precision.fixedFraction(NO_PRECISION))
                        } else {
                            localizedNumberFormatter
                        }
                    }
                    .format(price)
                    .toString()
            } else {
                val javaCurrency = Currency.getInstance(currency.name)
                NumberFormat.getCurrencyInstance().apply {
                    currency = javaCurrency
                    if (price == 0.0) {
                        maximumFractionDigits = NO_PRECISION
                    }
                }.format(price)
            }
        }

    companion object {
        private const val NO_PRECISION = 0
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
        fun xrdPrice(xrdBalance: Decimal192): FiatPrice? = oneXrdPrice?.let { it * xrdBalance }
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
