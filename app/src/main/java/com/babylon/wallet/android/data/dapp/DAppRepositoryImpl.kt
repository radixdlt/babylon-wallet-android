package com.babylon.wallet.android.data.dapp

import com.babylon.wallet.android.domain.dapp.DAppRepository
import kotlinx.coroutines.delay
import javax.inject.Inject
import kotlin.random.Random

class DAppRepositoryImpl @Inject constructor(
//    private val webRtcManager: WebRtcManager
//    private val gatewayClient: GatewayClient
) : DAppRepository {

    override suspend fun getDAppRequest(connectionId: String): RequestMethodWalletRequest {
        // webRtcManager.init(connectionId)
        // webRtcManager.getDAppRequest()

        // gatewayClient.checkHappy() path for now, check passes
        delay(Random.nextLong(500, 1500))
        return RequestMethodWalletRequest(
            "",
            "",
            payload = listOf(
                RequestMethodWalletRequest.AccountAddressesRequestMethodWalletRequest(
                    requestType = "",
                    numberOfAddresses = 1,
                    ongoing = false,
                    reset = false
                ),
                RequestMethodWalletRequest.PersonaDataRequestMethodWalletRequest(
                    requestType = "",
                    fields = listOf(
                        "email address",
                        "name"
                    ),
                    ongoing = false,
                    reset = false,
                    revokeOngoingAccess = listOf()
                )
            ),
            RequestMethodWalletRequest.RequestMethodMetadata(
                "",
                "",
            )
        )
    }
}