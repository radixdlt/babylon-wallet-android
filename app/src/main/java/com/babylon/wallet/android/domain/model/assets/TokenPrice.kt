package com.babylon.wallet.android.domain.model.assets

import java.math.BigDecimal

data class TokenPrice(
    val resourceAddress: String,
    val price: BigDecimal,
    val currency: String
) {

    companion object {
        const val CURRENCY_USD = "USD"
    }
}
