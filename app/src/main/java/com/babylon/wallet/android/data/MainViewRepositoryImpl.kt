package com.babylon.wallet.android.data

import com.babylon.wallet.android.data.AccountDto.Companion.toUiModel
import com.babylon.wallet.android.domain.MainViewRepository
import com.babylon.wallet.android.presentation.model.AccountUi
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
                mockAccountData.map { accountDto ->
                    accountDto.toUiModel()
                }
            )
        }
    }

    override suspend fun getAccountBasedOnId(id: String): AccountUi {
        delay(Random.nextLong(500, 1000))
        return mockAccountData
            .map { accountDto ->
                accountDto.toUiModel()
            }
            .first { accountData ->
                accountData.id == id
            }
    }
}

val mockAccountData = listOf(
    AccountDto(
        id = "a1",
        name = "My main account",
        hash = "0x589e5cb09935F67c441AEe6AF46A365274a932e3",
        value = 19195.0F,
        currency = "$"
    ),
    AccountDto(
        id = "a2",
        name = "My fun account",
        hash = "0x589e5cb06635F67c441EAe6AF46A365278a932e1",
        value = 214945.5F,
        currency = "$"
    ),
    AccountDto(
        id = "a2",
        name = "Only NFTs",
        hash = "0x559e5cb66035F67c441EAe6AF46A474278a932e1",
        value = 12149455.0F,
        currency = "$"
    )
).shuffled()
