package com.babylon.wallet.android.data.repository.networkinfo

import com.babylon.wallet.android.data.gateway.DynamicUrlApi
import com.babylon.wallet.android.data.repository.cache.CacheParameters
import com.babylon.wallet.android.data.repository.cache.HttpCache
import com.babylon.wallet.android.data.repository.cache.TimeoutDuration
import com.babylon.wallet.android.data.repository.execute
import com.babylon.wallet.android.domain.common.Result
import javax.inject.Inject

interface NetworkInfoRepository {
    suspend fun getNetworkInfo(networkUrl: String): Result<String>
}

class NetworkInfoRepositoryImpl @Inject constructor(
    private val dynamicUrlApi: DynamicUrlApi,
    private val cache: HttpCache
) : NetworkInfoRepository {

    override suspend fun getNetworkInfo(networkUrl: String): Result<String> {
        return dynamicUrlApi.gatewayStatus(DynamicUrlApi.gatewayStatusUrl(networkUrl)) // TODO 1181
            .execute(
                cacheParameters = CacheParameters(
                    httpCache = cache,
                    timeoutDuration = TimeoutDuration.FIVE_MINUTES
                ),
                map = { it.ledgerState.network }
            )
    }
}
