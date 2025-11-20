package rdx.works.core.domain.assets

import android.icu.number.NumberFormatter
import android.icu.number.Precision
import android.icu.text.NumberFormat
import android.icu.util.Currency
import android.os.Build
import com.radixdlt.sargon.Decimal192
import com.radixdlt.sargon.NonFungibleGlobalId
import com.radixdlt.sargon.extensions.isZero
import com.radixdlt.sargon.extensions.orZero
import com.radixdlt.sargon.extensions.plus
import com.radixdlt.sargon.extensions.string
import com.radixdlt.sargon.extensions.sumOf
import com.radixdlt.sargon.extensions.times
import com.radixdlt.sargon.extensions.toDecimal192
import rdx.works.core.domain.resources.Resource
import rdx.works.core.domain.toDouble
import java.math.RoundingMode
import java.util.Locale
import kotlin.math.max

data class FiatPrice(
    val price: Decimal192,
    val currency: SupportedCurrency
) {

    val isZero = price.isZero

    val defaultFormatted: String by lazy {
        formatted()
    }

    fun formatted(significantDigitsPrecision: Int? = null): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            var formatter = NumberFormatter.with()
                .unit(Currency.getInstance(currency.name))
                .locale(Locale.getDefault())
            if (price.isZero) {
                formatter = formatter.precision(Precision.fixedFraction(NO_PRECISION))
            } else if (significantDigitsPrecision != null) {
                formatter = formatter.precision(Precision.fixedSignificantDigits(significantDigitsPrecision))
            }
            formatter.format(price.toDouble()).toString()
        } else {
            var priceAsBigDecimal = price.string.toBigDecimal()
            if (significantDigitsPrecision != null) {
                val newScale = significantDigitsPrecision - priceAsBigDecimal.precision() + priceAsBigDecimal.scale()
                priceAsBigDecimal = priceAsBigDecimal.setScale(newScale, RoundingMode.HALF_UP)
            }
            val javaCurrency = Currency.getInstance(currency.name)
            NumberFormat.getCurrencyInstance().apply {
                currency = javaCurrency
                if (price.isZero) {
                    maximumFractionDigits = NO_PRECISION
                } else if (significantDigitsPrecision != null) {
                    maximumFractionDigits = max(0, priceAsBigDecimal.stripTrailingZeros().scale())
                }
            }.format(if (significantDigitsPrecision != null) priceAsBigDecimal else price.toDouble())
        }
    }

    operator fun times(value: Decimal192): FiatPrice = FiatPrice(
        price = price * value,
        currency = currency
    )

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
                        fiatPrice?.price.orZero()
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
                        fiatPrice?.price.orZero()
                    },
                    currency = currency
                )
            }

        fun xrdPrice(resource: Resource.FungibleResource): FiatPrice? = prices[resource]
    }

    data class NFTPrice(
        override val asset: Asset.NonFungible,
        private val nftPrices: Map<NonFungibleGlobalId, FiatPrice>,
        private val currency: SupportedCurrency
    ) : AssetPrice() {

        override val price: FiatPrice?
            get() {
                var totalPrice = 0.toDecimal192()
                asset.resource.items.forEach { item ->
                    totalPrice += nftPrices[item.globalId]?.price.orZero()
                }
                return FiatPrice(
                    price = totalPrice,
                    currency = currency
                )
            }
    }
}
