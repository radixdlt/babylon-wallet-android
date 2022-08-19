package com.babylon.wallet.android.domain

import com.babylon.wallet.android.presentation.model.AccountUi
import com.babylon.wallet.android.presentation.wallet.WalletData

interface MainViewRepository {

    suspend fun getWallet(): WalletData

    suspend fun getAccounts(): List<AccountUi>

    suspend fun getAccountBasedOnId(id: String): AccountUi
}
