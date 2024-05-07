package com.babylon.wallet.android.data.repository.networkinfo

import com.babylon.wallet.android.data.gateway.apis.StatusApi
import com.babylon.wallet.android.data.repository.toResult
import com.babylon.wallet.android.di.JsonConverterFactory
import com.babylon.wallet.android.di.SimpleHttpClient
import com.babylon.wallet.android.di.buildApi
import com.babylon.wallet.android.domain.model.NetworkInfo
import com.radixdlt.sargon.NetworkId
import okhttp3.OkHttpClient
import rdx.works.core.sargon.init
import retrofit2.Converter
import javax.inject.Inject

interface NetworkInfoRepository {
    suspend fun getNetworkInfo(networkUrl: String): Result<NetworkInfo>
}

class NetworkInfoRepositoryImpl @Inject constructor(
    @SimpleHttpClient private val okHttpClient: OkHttpClient,
    @JsonConverterFactory private val jsonConverterFactory: Converter.Factory,
) : NetworkInfoRepository {

    override suspend fun getNetworkInfo(networkUrl: String): Result<NetworkInfo> = buildApi<StatusApi>(
        baseUrl = networkUrl,
        okHttpClient = okHttpClient,
        jsonConverterFactory = jsonConverterFactory
    ).gatewayStatus().toResult().mapCatching { response ->
        val networkId = NetworkId.init(response.ledgerState.network)
        NetworkInfo(
            id = networkId,
            epoch = response.ledgerState.epoch
        )
    }
}
