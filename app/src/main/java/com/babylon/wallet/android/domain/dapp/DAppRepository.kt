package com.babylon.wallet.android.domain.dapp

import com.babylon.wallet.android.data.dapp.RequestMethodWalletRequest

interface DAppRepository {
    suspend fun getDAppRequest(connectionId: String): RequestMethodWalletRequest
}