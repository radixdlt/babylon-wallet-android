package com.babylon.wallet.android.presentation.helpers

import com.babylon.wallet.android.data.AccountDto.Companion.toUiModel
import com.babylon.wallet.android.data.mockdata.mockAccountDtoList
import com.babylon.wallet.android.domain.MainViewRepository
import com.babylon.wallet.android.presentation.model.AccountUi
import com.babylon.wallet.android.presentation.wallet.WalletData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class MockMainViewRepository : MainViewRepository {

    override fun getWallet(): Flow<WalletData> {
        return flowOf(
            WalletData(
                currency = "$",
                amount = "320409"
            )
        )
    }

    override fun getAccounts(): Flow<List<AccountUi>> {
        return flowOf(
            mockAccountDtoList.map { accountDto ->
                accountDto.toUiModel()
            }
        )
    }

    override suspend fun getAccountBasedOnId(id: String): AccountUi {
        return mockAccountDtoList[2].toUiModel()
    }
}
