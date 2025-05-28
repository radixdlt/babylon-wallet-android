package com.babylon.wallet.android.data.repository.rns

import com.babylon.wallet.android.presentation.transfer.accounts.RnsDomain
import com.radixdlt.sargon.RadixNameService
import com.radixdlt.sargon.os.driver.AndroidNetworkingDriver
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
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
    suspend fun resolveReceiver(domain: String): Result<RnsDomain>
}

class RNSRepositoryImpl @Inject constructor(
    @GatewayHttpClient httpClient: OkHttpClient,
    profileRepository: ProfileRepository,
    @ApplicationScope applicationScope: CoroutineScope,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
): RNSRepository {

    private val networkingDriver: AndroidNetworkingDriver = AndroidNetworkingDriver(client = httpClient)
    private val nameServiceState: MutableStateFlow<RadixNameService?> = MutableStateFlow(null)

    init {
        applicationScope.launch {
            profileRepository
                .profile
                .map { it.currentGateway.network.id }
                .distinctUntilChanged()
                .collect { networkId ->
                    nameServiceState.update {
                        RadixNameService(
                            networkingDriver,
                            networkId
                        )
                    }
                }
        }
    }

    override suspend fun resolveReceiver(domain: String): Result<RnsDomain> = withContext(ioDispatcher){
        val nameService = nameServiceState
            .filterNotNull()
            .first()

        runCatching {
            nameService.resolveReceiverAccountForDomain(domain)
        }.map {
            RnsDomain(
                accountAddress = it.account,
                name = it.domain,
                imageUrl = "https://qr.rns.foundation/${it.domain}"
            )
        }
    }
}