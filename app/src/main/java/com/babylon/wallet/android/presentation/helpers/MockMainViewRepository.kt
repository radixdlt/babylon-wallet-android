package com.babylon.wallet.android.presentation.helpers

import com.babylon.wallet.android.domain.MainViewRepository
import com.babylon.wallet.android.presentation.wallet.WalletData

class MockMainViewRepository : MainViewRepository {

    override suspend fun getWallet(): WalletData {
        return WalletData(
            currency = "$",
            amount = "320409"
        )
    }
}
