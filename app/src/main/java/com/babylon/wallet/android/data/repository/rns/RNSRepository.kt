package com.babylon.wallet.android.data.repository.rns

import com.radixdlt.sargon.RadixNameService
import com.radixdlt.sargon.RnsDomainConfiguredReceiver
import com.radixdlt.sargon.os.driver.AndroidNetworkingDriver
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import rdx.works.core.di.ApplicationScope
import rdx.works.core.di.GatewayHttpClient
import rdx.works.core.di.IoDispatcher
import rdx.works.core.sargon.currentGateway
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.data.repository.profile
import javax.inject.Inject

interface RNSRepository {
    suspend fun resolveReceiver(domain: String): Result<RnsDomainConfiguredReceiver>
}

class RNSRepositoryImpl @Inject constructor(
    @GatewayHttpClient httpClient: OkHttpClient,
    profileRepository: ProfileRepository,
    @ApplicationScope applicationScope: CoroutineScope,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : RNSRepository {

    private val networkingDriver: AndroidNetworkingDriver =
        AndroidNetworkingDriver(client = httpClient)
    private val nameServiceState: MutableStateFlow<RNSState> = MutableStateFlow(RNSState.Idle)

    init {
        applicationScope.launch {
            profileRepository
                .profile
                .map { it.currentGateway.network.id }
                .distinctUntilChanged()
                .collect { networkId ->
                    runCatching {
                        RadixNameService(networkingDriver, networkId)
                    }.onSuccess { service ->
                        nameServiceState.update { RNSState.Instantiated(service) }
                    }.onFailure { error ->
                        nameServiceState.update { RNSState.Error(error) }
                    }
                }
        }
    }

    override suspend fun resolveReceiver(domain: String): Result<RnsDomainConfiguredReceiver> =
        withContext(ioDispatcher) {
            val serviceState = nameServiceState
                .filterNot { it is RNSState.Idle }
                .first()

            when (serviceState) {
                is RNSState.Idle -> error("Impossible state. Such state is filtered out.")
                is RNSState.Error -> Result.failure(serviceState.error)
                is RNSState.Instantiated -> runCatching {
                    serviceState.service.resolveReceiverAccountForDomain(domain)
                }
            }
        }

    sealed interface RNSState {
        data object Idle : RNSState
        data class Instantiated(val service: RadixNameService) : RNSState
        data class Error(val error: Throwable) : RNSState
    }
}
