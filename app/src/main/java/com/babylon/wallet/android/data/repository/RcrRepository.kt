package com.babylon.wallet.android.data.repository

import com.babylon.wallet.android.data.gateway.apis.RcrApi
import com.babylon.wallet.android.di.coroutines.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject

interface RcrRepository {
}

class RcrRepositoryImpl @Inject constructor(
    private val rcrApi: RcrApi,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : RcrRepository {

    suspend fun sendRequest(origin: String) = withContext(ioDispatcher) {
//        api.wellKnownDAppDefinition().toResult().map { wellKnownFile ->
//            wellKnownFile.dApps.map { it.dAppDefinitionAddress }
//        }.onFailure {
//            RadixWalletException.DappVerificationException.RadixJsonNotFound
//        }
    }
}
