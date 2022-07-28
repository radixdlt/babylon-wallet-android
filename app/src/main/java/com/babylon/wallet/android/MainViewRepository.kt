package com.babylon.wallet.android

import kotlinx.coroutines.flow.Flow

interface MainViewRepository {
    suspend fun getWalletData(): Flow<WalletData>
    suspend fun getAccountData(): Flow<AccountData>
}
