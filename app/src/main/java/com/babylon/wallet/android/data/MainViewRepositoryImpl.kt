package com.babylon.wallet.android.data

import com.babylon.wallet.android.data.AccountDto.Companion.toUiModel
import com.babylon.wallet.android.data.mockdata.mockAccountDtoList
import com.babylon.wallet.android.domain.MainViewRepository
import com.babylon.wallet.android.presentation.model.AccountUi
import com.babylon.wallet.android.presentation.model.NftClassUi
import com.babylon.wallet.android.presentation.wallet.WalletData
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
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
        }
    }

    override fun getAccounts(): Flow<List<AccountUi>> {
        return flow {
            delay(Random.nextLong(500, 1500))
            emit(
                mockAccountDtoList.map { accountDto ->
                    accountDto.toUiModel()
                }
            )
        }
    }

    override suspend fun getAccountBasedOnId(id: String): AccountUi {
        delay(Random.nextLong(500, 1000))
        return mockAccountDtoList
            .map { accountDto ->
                accountDto.toUiModel()
            }
            .first { accountData ->
                accountData.id == id
            }
    }

    override fun getNftList(): Flow<List<NftClassUi>> {
        return flow {
            delay(Random.nextLong(500, 1500))
            emit(
                mockAccountDtoList.first().toUiModel().nfts
            )
        }
    }
}
