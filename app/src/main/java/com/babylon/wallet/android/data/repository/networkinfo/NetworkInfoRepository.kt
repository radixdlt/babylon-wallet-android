package com.babylon.wallet.android.data.repository.networkinfo

import com.babylon.wallet.android.data.gateway.apis.StatusApi
import com.babylon.wallet.android.data.repository.execute
import com.babylon.wallet.android.di.JsonConverterFactory
import com.babylon.wallet.android.di.SimpleHttpClient
import com.babylon.wallet.android.di.buildApi
import com.babylon.wallet.android.domain.common.Result
import com.babylon.wallet.android.domain.common.onValue
import kotlinx.coroutines.flow.MutableStateFlow
import okhttp3.OkHttpClient
import retrofit2.Converter
import timber.log.Timber
import javax.inject.Inject

interface NetworkInfoRepository {
    val isMainnetLive: MutableStateFlow<Boolean>
    suspend fun getNetworkInfo(networkUrl: String): Result<String>
    suspend fun getFaucetComponentAddress(networkUrl: String): Result<String>
    suspend fun getMainnetAvailability(): Result<Boolean>
}

class NetworkInfoRepositoryImpl @Inject constructor(
    @SimpleHttpClient private val okHttpClient: OkHttpClient,
    @JsonConverterFactory private val jsonConverterFactory: Converter.Factory,
) : NetworkInfoRepository {

    // TODO ONLY FOR TESTING PURPOSES
    override val isMainnetLive: MutableStateFlow<Boolean> = MutableStateFlow(false)

    override suspend fun getNetworkInfo(networkUrl: String): Result<String> = buildApi<StatusApi>(
        baseUrl = networkUrl,
        okHttpClient = okHttpClient,
        jsonConverterFactory = jsonConverterFactory
    ).gatewayStatus().execute(
        map = {
            it.ledgerState.network
        }
    )

    override suspend fun getFaucetComponentAddress(networkUrl: String): Result<String> {
        return buildApi<StatusApi>(
            baseUrl = networkUrl,
            okHttpClient = okHttpClient,
            jsonConverterFactory = jsonConverterFactory
        ).networkConfiguration().execute(
            map = {
                it.wellKnownAddresses.faucet
            }
        )
    }

    override suspend fun getMainnetAvailability(): Result<Boolean> {
        return buildApi<StatusApi>(
            baseUrl = MAINNET_STATUS_URL,
            okHttpClient = okHttpClient,
            jsonConverterFactory = jsonConverterFactory
        ).mainnetNetworkStatus().execute(
            // TODO ONLY FOR TESTING PURPOSES
            map = { /*it.isMainnetLive*/isMainnetLive.value  }
        ).onValue {
            Timber.tag("Bakos").d("Mainnet: $it")
        }
    }

    companion object {
        private const val MAINNET_STATUS_URL = "https://mainnet-status.extratools.works"
    }
}
