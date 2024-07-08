package com.babylon.wallet.android.data.repository.dapps

import com.babylon.wallet.android.data.gateway.apis.DAppDefinitionApi
import com.babylon.wallet.android.data.gateway.model.WellKnownDAppDefinitionResponse
import com.babylon.wallet.android.data.repository.toResult
import com.babylon.wallet.android.di.JsonConverterFactory
import com.babylon.wallet.android.di.GatewayHttpClient
import com.babylon.wallet.android.di.buildApi
import com.babylon.wallet.android.di.coroutines.IoDispatcher
import com.babylon.wallet.android.domain.DappDefinition
import com.babylon.wallet.android.domain.DappDefinitions
import com.babylon.wallet.android.domain.RadixWalletException
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.extensions.init
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Converter
import javax.inject.Inject

interface WellKnownDAppDefinitionRepository {

    suspend fun getWellKnownDappDefinitions(origin: String): Result<DappDefinitions>
    suspend fun getWellKnownDAppDefinitionAddresses(origin: String): Result<List<AccountAddress>>
}

class WellKnownDAppDefinitionRepositoryImpl @Inject constructor(
    @GatewayHttpClient private val okHttpClient: OkHttpClient,
    @JsonConverterFactory private val jsonConverterFactory: Converter.Factory,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : WellKnownDAppDefinitionRepository {
    override suspend fun getWellKnownDAppDefinitionAddresses(origin: String): Result<List<AccountAddress>> = withContext(ioDispatcher) {
        getWellKnownDAppDefinitions(origin).map { response ->
            response.dApps.map { AccountAddress.init(it.dAppDefinitionAddress) }
        }
    }

    override suspend fun getWellKnownDappDefinitions(origin: String): Result<DappDefinitions> {
        return withContext(ioDispatcher) {
            getWellKnownDAppDefinitions(origin).map {
                DappDefinitions(
                    dAppDefinitions = it.dApps.map { dApp ->
                        DappDefinition(AccountAddress.init(dApp.dAppDefinitionAddress))
                    },
                    callbackPath = it.callbackPath
                )
            }
        }
    }

    private suspend fun getWellKnownDAppDefinitions(origin: String): Result<WellKnownDAppDefinitionResponse> =
        withContext(ioDispatcher) {
            val api = buildApi<DAppDefinitionApi>(
                baseUrl = origin,
                okHttpClient = okHttpClient,
                jsonConverterFactory = jsonConverterFactory
            )

            api.wellKnownDefinition().toResult().map { wellKnownFile ->
                wellKnownFile
            }.onFailure {
                RadixWalletException.DappVerificationException.RadixJsonNotFound
            }
        }
}
