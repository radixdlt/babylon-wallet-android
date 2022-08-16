package com.babylon.wallet.android.data

import com.babylon.wallet.android.data.AccountDto.Companion.toUiModel
import com.babylon.wallet.android.data.mockdata.mockAccountDtoList
import com.babylon.wallet.android.domain.MainViewRepository
import com.babylon.wallet.android.presentation.model.AccountUi
import com.babylon.wallet.android.presentation.wallet.WalletData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlin.random.Random

class MainViewRepositoryImpl : MainViewRepository {

    override fun getWallet(): Flow<WalletData> {
        return flow {
            delay(Random.nextLong(500, 1500))
            emit(
                WalletData(
                    "$",
                    "320409"
                )
            )
        }.flowOn(Dispatchers.IO)
    }

    override fun getAccounts(): Flow<List<AccountUi>> {
        return flow {
            delay(Random.nextLong(500, 1500))
            emit(
                mockAccountDtoList.map { accountDto ->
                    accountDto.toUiModel()
                }
            )
        }.flowOn(Dispatchers.IO)
    }

    override suspend fun getAccountBasedOnId(id: String): AccountUi {
        return withContext(Dispatchers.IO) {
            delay(Random.nextLong(500, 1000))
            mockAccountDtoList
                .map { accountDto ->
                    accountDto.toUiModel()
                }
                .first { accountData ->
                    accountData.id == id
                }
        }
    }
}
