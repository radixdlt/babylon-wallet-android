package com.babylon.wallet.android.domain

import com.babylon.wallet.android.presentation.wallet.AccountData
import com.babylon.wallet.android.presentation.wallet.WalletData
import kotlinx.coroutines.flow.Flow

interface MainViewRepository {
    fun getWalletData(): Flow<WalletData>
    fun getAccountData(): Flow<AccountData>
}
