package com.babylon.wallet.android.data

import com.babylon.wallet.android.data.AccountDto.Companion.toUiModel
import com.babylon.wallet.android.data.mockdata.mockAccountDtoList
import com.babylon.wallet.android.di.coroutines.IoDispatcher
import com.babylon.wallet.android.domain.MainViewRepository
import com.babylon.wallet.android.presentation.model.AccountUi
import com.babylon.wallet.android.presentation.wallet.WalletData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlin.random.Random

@Suppress("MagicNumber") // TODO this is temporarily here.
class MainViewRepositoryImpl(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : MainViewRepository {

    override fun getWallet(): Flow<WalletData> {
        return flow {
            delay(Random.nextLong(500, 1500))
            emit(
                WalletData(
                    "$",
                    "320409"
                )
            )
        }.flowOn(ioDispatcher)
    }

    override fun getAccounts(): Flow<List<AccountUi>> {
        return flow {
            delay(Random.nextLong(500, 1500))
            emit(
                mockAccountDtoList.map { accountDto ->
                    accountDto.toUiModel()
                }
            )
        }.flowOn(ioDispatcher)
    }

    override suspend fun getAccountBasedOnId(id: String): AccountUi {
        return withContext(ioDispatcher) {
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
