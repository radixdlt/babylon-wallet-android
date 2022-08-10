package com.babylon.wallet.android.presentation.model

data class AccountUi(
    val id: String,
    val name: String,
    val hash: String,
    val amount: String,
    val currencySymbol: String,
    val tokens: List<TokenUi> = emptyList(),
    val nfts: List<NftClassUi> = emptyList()
)
