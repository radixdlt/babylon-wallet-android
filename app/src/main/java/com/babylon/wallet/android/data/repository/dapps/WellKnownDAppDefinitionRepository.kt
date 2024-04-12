package com.babylon.wallet.android.data.repository.dapps

import com.babylon.wallet.android.data.gateway.apis.DAppDefinitionApi
import com.babylon.wallet.android.data.repository.toResult
import com.babylon.wallet.android.di.JsonConverterFactory
import com.babylon.wallet.android.di.SimpleHttpClient
import com.babylon.wallet.android.di.buildApi
import com.babylon.wallet.android.di.coroutines.IoDispatcher
import com.babylon.wallet.android.domain.RadixWalletException
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.extensions.init
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Converter
import javax.inject.Inject

interface WellKnownDAppDefinitionRepository {

    suspend fun getWellKnownDAppDefinitions(origin: String): Result<List<AccountAddress>>
}

class WellKnownDAppDefinitionRepositoryImpl @Inject constructor(
    @SimpleHttpClient private val okHttpClient: OkHttpClient,
    @JsonConverterFactory private val jsonConverterFactory: Converter.Factory,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : WellKnownDAppDefinitionRepository {
    override suspend fun getWellKnownDAppDefinitions(origin: String): Result<List<AccountAddress>> = withContext(ioDispatcher) {
        val api = buildApi<DAppDefinitionApi>(
            baseUrl = origin,
            okHttpClient = okHttpClient,
            jsonConverterFactory = jsonConverterFactory
        )

        api.wellKnownDAppDefinition().toResult().mapCatching { wellKnownFile ->
            wellKnownFile.dApps.map { AccountAddress.init(it.dAppDefinitionAddress) }
        }.onFailure {
            RadixWalletException.DappVerificationException.RadixJsonNotFound
        }
    }
}
