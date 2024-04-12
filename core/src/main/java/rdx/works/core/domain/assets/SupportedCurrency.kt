package rdx.works.core.domain.assets

import com.radixdlt.sargon.extensions.toDecimal192
import java.util.Currency

enum class SupportedCurrency(val code: String) {
    USD("USD");

    private val nativeCurrency: Currency
        get() = Currency.getInstance(code)

    val symbol: String
        get() = nativeCurrency.symbol

    val hiddenBalance: String
        get() {
            val hiddenPriceCharacters = " • • • • "

            val symbolEscaped = Regex.escape(symbol)
            return FiatPrice(
                price = 1.0.toDecimal192(),
                currency = this
            ).formatted.replace("[^$symbolEscaped]+".toRegex(), hiddenPriceCharacters)
        }

    val errorBalance: String
        get() {
            val errorPriceCharacter = " - "

            val symbolEscaped = Regex.escape(symbol)
            return FiatPrice(
                price = 1.0.toDecimal192(),
                currency = this
            ).formatted.replace("[^$symbolEscaped]+".toRegex(), errorPriceCharacter)
        }

    companion object {
        fun fromCode(code: String) = entries.find { it.code == code }
    }
}
