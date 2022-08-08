package com.babylon.wallet.android.data

import com.babylon.wallet.android.presentation.model.AccountUi
import java.text.NumberFormat
import java.util.Locale

data class AccountDto(
    val id: String,
    val name: String,
    val hash: String,
    val value: Float,
    val currency: String,
    val assets: List<String>
) {

    companion object {
        fun AccountDto.toUiModel() = AccountUi(
            id = id,
            name = name,
            hash = hash,
            amount = amountToUiFormat(value, currency),
            currencySymbol = currency,
            assets = assets
        )
    }

    // TODO the format api returns the symbol alongside the amount,
    //  so we can drop later the "currencySymbol" from the AccountUi model
    private fun amountToUiFormat(amount: Float, currencySymbol: String): String {
        val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "US"))
        var formattedAmount = currencyFormat.format(amount)
        formattedAmount = formattedAmount.removePrefix(currencySymbol)
        return formattedAmount
    }
}
