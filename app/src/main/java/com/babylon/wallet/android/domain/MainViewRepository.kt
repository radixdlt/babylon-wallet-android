package com.babylon.wallet.android.domain

import com.babylon.wallet.android.data.AssetDto
import com.babylon.wallet.android.presentation.model.AccountUi
import com.babylon.wallet.android.presentation.model.NftClassUi
import com.babylon.wallet.android.presentation.wallet.WalletData
import kotlinx.coroutines.flow.Flow

interface MainViewRepository {

    fun getWallet(): Flow<WalletData>

    fun getAccounts(): Flow<List<AccountUi>>

    suspend fun getAccountBasedOnId(id: String): AccountUi

    fun getNftList(): Flow<List<NftClassUi>>
}
