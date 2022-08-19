package com.babylon.wallet.android.presentation.helpers

import com.babylon.wallet.android.data.AccountDto.Companion.toUiModel
import com.babylon.wallet.android.data.mockdata.mockAccountDtoList
import com.babylon.wallet.android.domain.MainViewRepository
import com.babylon.wallet.android.presentation.model.AccountUi
import com.babylon.wallet.android.presentation.wallet.WalletData

class MockMainViewRepository : MainViewRepository {

    override suspend fun getWallet(): WalletData {
        return WalletData(
            currency = "$",
            amount = "320409"
        )
    }

    override suspend fun getAccounts(): List<AccountUi> {
        return mockAccountDtoList.map { accountDto ->
            accountDto.toUiModel()
        }
    }

    override suspend fun getAccountBasedOnId(id: String): AccountUi {
        return mockAccountDtoList[2].toUiModel()
    }
}
