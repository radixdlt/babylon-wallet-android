package com.babylon.wallet.android.data.repository.networkinfo

import com.babylon.wallet.android.domain.common.Result

interface NetworkInfoRepository {
    suspend fun getNetworkInfo(networkUrl: String): Result<String>
}
