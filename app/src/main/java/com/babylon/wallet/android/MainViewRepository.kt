package com.babylon.wallet.android

import kotlinx.coroutines.flow.Flow

interface MainViewRepository {
    fun getWalletData(): Flow<WalletData>
    fun getAccountData(): Flow<AccountData>
}
