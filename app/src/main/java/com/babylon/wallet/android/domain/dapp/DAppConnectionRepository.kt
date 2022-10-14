package com.babylon.wallet.android.domain.dapp

import com.babylon.wallet.android.data.dapp.DAppConnectionData

interface DAppConnectionRepository {
    suspend fun getDAppConnectionData(): DAppConnectionData
    suspend fun getChooseDAppLoginData(): DAppConnectionData
}
