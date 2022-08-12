package com.babylon.wallet.android.presentation.model

data class TokenUi(
    val id: String,
    val name: String?,
    val symbol: String?, // short name
    val tokenQuantity: String, // the amount of the token held
    val tokenValue: String?, // the current value in currency the user has selected for the wallet
    val iconUrl: String?,
)
