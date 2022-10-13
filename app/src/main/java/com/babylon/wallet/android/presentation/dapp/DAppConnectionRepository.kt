package com.babylon.wallet.android.presentation.dapp

import com.babylon.wallet.android.presentation.dapp.model.DAppConnectionData

interface DAppConnectionRepository {
    suspend fun getDAppConnectionData(): DAppConnectionData
    suspend fun getChooseDAppLoginData(): DAppConnectionData
}
