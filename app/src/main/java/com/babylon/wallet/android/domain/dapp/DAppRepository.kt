package com.babylon.wallet.android.domain.dapp

import com.babylon.wallet.android.data.dapp.DAppDetailsResponse
import com.babylon.wallet.android.data.dapp.DAppWellKnownResponse
import com.babylon.wallet.android.data.dapp.RequestMethodWalletRequest

interface DAppRepository {
    suspend fun getDAppRequest(connectionId: String): RequestMethodWalletRequest
    suspend fun fetchWellKnown(host: String): DAppWellKnownResponse
    suspend fun fetchDAppDetails(dAppId: String): DAppDetailsResponse
}