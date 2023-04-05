package com.babylon.wallet.android.data.repository.networkinfo

import com.babylon.wallet.android.data.gateway.apis.StatusApi
import com.babylon.wallet.android.data.repository.execute
import com.babylon.wallet.android.domain.common.Result
import javax.inject.Inject

interface NetworkInfoRepository {
    suspend fun getNetworkInfo(networkUrl: String): Result<String>
}

class NetworkInfoRepositoryImpl @Inject constructor(
    private val statusApi: StatusApi
) : NetworkInfoRepository {

    override suspend fun getNetworkInfo(networkUrl: String): Result<String> {
        return statusApi.gatewayStatus(StatusApi.gatewayStatusUrl(networkUrl))
            .execute(
                map = { it.ledgerState.network }
            )
    }
}
