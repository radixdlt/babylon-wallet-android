package com.babylon.wallet.android.presentation.dapp

interface DAppConnectionRepository {
    suspend fun getDAppConnectionData(): DAppConnectionData
    suspend fun getChooseDAppLoginData(): DAppConnectionData
}
