package com.babylon.wallet.android.presentation.dapp.data

data class DAppConnectionData(
    val labels: List<String> = emptyList(),
    val imageUrl: String,
    val dAppAccount: DAppAccount? = null
)
