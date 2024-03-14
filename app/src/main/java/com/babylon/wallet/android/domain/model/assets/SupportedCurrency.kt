package com.babylon.wallet.android.domain.model.assets

import java.util.Currency

enum class SupportedCurrency(val code: String) {
    USD("USD");

    val hiddenBalance: String
        get() {
            val currency = Currency.getInstance(code)
            val hiddenPriceCharacters = "• • • •"

            val symbolEscaped = Regex.escape(currency.symbol)
            return FiatPrice(
                price = 1.0,
                currency = this
            ).formatted.replace("[^$symbolEscaped]+".toRegex(), hiddenPriceCharacters)
        }

    companion object {
        fun fromCode(code: String) = entries.find { it.code == code }
    }
}
