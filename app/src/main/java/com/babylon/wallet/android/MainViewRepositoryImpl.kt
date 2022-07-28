package com.babylon.wallet.android

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class MainViewRepositoryImpl : MainViewRepository {
    override suspend fun getWalletData(): Flow<WalletData> {
        return flow {
            delay(500)
            emit(
                WalletData(
                    "$",
                    "1000"
                )
            )
        }
    }
    override suspend fun getAccountData(): Flow<AccountData> {
        return flow {
            delay(500)
            emit(
                AccountData(
                    "My main account",
                    "r32djrf920rh20dff",
                    "25",
                    "$"
                )
            )
        }
    }
}
