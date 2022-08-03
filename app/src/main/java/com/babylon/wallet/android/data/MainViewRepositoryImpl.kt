package com.babylon.wallet.android.data

import com.babylon.wallet.android.domain.MainViewRepository
import com.babylon.wallet.android.presentation.wallet.AccountData
import com.babylon.wallet.android.presentation.wallet.WalletData
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.random.Random

class MainViewRepositoryImpl : MainViewRepository {
    override fun getWalletData(): Flow<WalletData> {
        return flow {
            delay(Random.nextLong(500, 1500))
            emit(
                WalletData(
                    "$",
                    "320409"
                )
            )
        }
    }
    override fun getAccountData(): Flow<AccountData> {
        return flow {
            delay(Random.nextLong(500, 1500))
            emit(
                AccountData(
                    "My main account",
                    "0x589e5cb09935F67c441AEe6AF46A365274a932e3",
                    "19195",
                    "$"
                )
            )
        }
    }
}
