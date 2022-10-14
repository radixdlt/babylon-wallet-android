package com.babylon.wallet.android.data.dapp

data class DAppConnectionData(
    val labels: List<String> = emptyList(),
    val imageUrl: String,
    val dAppAccount: DAppAccount? = null
)
