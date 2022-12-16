package com.babylon.wallet.android.data.repository.networkinfo

import com.babylon.wallet.android.data.gateway.GatewayInfoApi
import com.babylon.wallet.android.data.repository.performHttpRequest
import com.babylon.wallet.android.domain.common.Result
import javax.inject.Inject

class NetworkInfoRepositoryImpl @Inject constructor(private val gatewayInfoApi: GatewayInfoApi) :
    NetworkInfoRepository {

    override suspend fun getNetworkInfo(networkUrl: String): Result<String> {
        return performHttpRequest(
            call = {
                gatewayInfoApi.gatewayInfo("$networkUrl/gateway/information")
            },
            map = {
                it.ledgerState.network
            }
        )
    }
}
