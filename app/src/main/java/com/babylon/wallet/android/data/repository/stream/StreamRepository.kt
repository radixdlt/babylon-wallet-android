package com.babylon.wallet.android.data.repository.stream

import com.babylon.wallet.android.data.gateway.apis.StreamApi
import com.babylon.wallet.android.data.gateway.generated.models.StreamTransactionsRequest
import com.babylon.wallet.android.data.gateway.generated.models.StreamTransactionsResponse
import com.babylon.wallet.android.data.repository.toResult
import com.babylon.wallet.android.di.coroutines.DefaultDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import rdx.works.profile.data.model.pernetwork.Network
import javax.inject.Inject

interface StreamRepository {

    suspend fun getAccountHistory(account: Network.Account): Result<StreamTransactionsResponse>
}

class StreamRepositoryImpl @Inject constructor(
    private val streamApi: StreamApi,
    @DefaultDispatcher private val dispatcher: CoroutineDispatcher,
) : StreamRepository {
    override suspend fun getAccountHistory(account: Network.Account): Result<StreamTransactionsResponse> {
        // TODO just temporary to validate endpoint is working
        return withContext(dispatcher) {
            streamApi.streamTransactions(StreamTransactionsRequest(accountsWithManifestOwnerMethodCalls = listOf(account.address)))
                .toResult()
        }
    }

}