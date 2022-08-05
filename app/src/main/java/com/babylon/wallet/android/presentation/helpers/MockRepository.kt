package com.babylon.wallet.android.presentation.helpers

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
            listOf(
                AccountUi(
                    id = "account id",
                    name = "My main account",
                    hash = "0x589e5cb09935F67c441AEe6AF46A365274a932e3",
                    amount = "19195",
                    currencySymbol = "$"
                )
            )
        )
    }

    override suspend fun getAccountBasedOnId(id: String): AccountUi {
        return AccountUi(
            id = "account id",
            name = "My main account",
            hash = "0x589e5cb09935F67c441AEe6AF46A365274a932e3",
            amount = "19195",
            currencySymbol = "$"
        )
    }
}
