package com.babylon.wallet.android.presentation.dapp.domain

import com.babylon.wallet.android.presentation.dapp.data.DAppConnectionData

interface DAppConnectionRepository {
    suspend fun getDAppConnectionData(): DAppConnectionData
    suspend fun getChooseDAppLoginData(): DAppConnectionData
}
