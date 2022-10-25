package com.babylon.wallet.android.data.dapp

import com.babylon.wallet.android.data.profile.Account

data class DAppAccountUiState(
    val account: Account,
    val selected: Boolean = false
)