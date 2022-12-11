package com.babylon.wallet.android.domain

import com.babylon.wallet.android.presentation.wallet.WalletData

interface MainViewRepository {
    suspend fun getWallet(): WalletData
}
