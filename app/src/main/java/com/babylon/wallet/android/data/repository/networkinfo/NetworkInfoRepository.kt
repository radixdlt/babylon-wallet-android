package com.babylon.wallet.android.data.repository.networkinfo

import com.babylon.wallet.android.data.gateway.DynamicUrlApi
import com.babylon.wallet.android.data.repository.performHttpRequest
import com.babylon.wallet.android.domain.common.Result
import javax.inject.Inject

interface NetworkInfoRepository {
    suspend fun getNetworkInfo(networkUrl: String): Result<String>
}

class NetworkInfoRepositoryImpl @Inject constructor(
    private val dynamicUrlApi: DynamicUrlApi
) : NetworkInfoRepository {

    override suspend fun getNetworkInfo(networkUrl: String): Result<String> {
        return performHttpRequest(
            call = {
                dynamicUrlApi.gatewayInfo("$networkUrl/gateway/information")
            },
            map = {
                it.ledgerState.network
            }
        )
    }
}
