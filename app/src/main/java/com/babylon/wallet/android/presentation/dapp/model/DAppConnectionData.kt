package com.babylon.wallet.android.presentation.dapp.model

import com.babylon.wallet.android.presentation.dapp.model.DAppAccount

data class DAppConnectionData(
    val labels: List<String> = emptyList(),
    val imageUrl: String,
    val dAppAccount: DAppAccount? = null
)
