package com.babylon.wallet.android.data.repository.networkinfo

import com.babylon.wallet.android.data.gateway.apis.StatusApi
import com.babylon.wallet.android.data.repository.execute
import com.babylon.wallet.android.di.JsonConverterFactory
import com.babylon.wallet.android.di.SimpleHttpClient
import com.babylon.wallet.android.di.buildApi
import com.babylon.wallet.android.domain.common.Result
import okhttp3.OkHttpClient
import retrofit2.Converter
import javax.inject.Inject

interface NetworkInfoRepository {
    suspend fun getNetworkInfo(networkUrl: String): Result<String>
}

class NetworkInfoRepositoryImpl @Inject constructor(
    @SimpleHttpClient private val okHttpClient: OkHttpClient,
    @JsonConverterFactory private val jsonConverterFactory: Converter.Factory,
) : NetworkInfoRepository {

    override suspend fun getNetworkInfo(networkUrl: String): Result<String> = buildApi<StatusApi>(
        baseUrl = networkUrl,
        okHttpClient = okHttpClient,
        jsonConverterFactory = jsonConverterFactory
    ).gatewayStatus().execute(
        map = { it.ledgerState.network }
    )
}
