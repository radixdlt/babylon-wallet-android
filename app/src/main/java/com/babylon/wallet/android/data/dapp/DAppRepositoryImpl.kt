package com.babylon.wallet.android.data.dapp

import com.babylon.wallet.android.domain.dapp.DAppRepository
import kotlinx.coroutines.delay
import javax.inject.Inject
import kotlin.random.Random

class DAppRepositoryImpl @Inject constructor(
//    private val webRtcManager: WebRtcManager // TODO
//    private val gatewayClient: GatewayClient
) : DAppRepository {

    override suspend fun getDAppRequest(connectionId: String): RequestMethodWalletRequest {
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
                dAppId = ""
            )
        )
    }

    override suspend fun fetchWellKnown(host: String): DAppWellKnownResponse {
        delay(Random.nextLong(500, 1500))
        return DAppWellKnownResponse(
            listOf(
                DApp(
                    id = "",
                    definitionAddress = ""
                )
            )
        )
    }

    override suspend fun fetchDAppDetails(dAppId: String): DAppDetailsResponse {
        delay(Random.nextLong(500, 1500))
        return DAppDetailsResponse(
            imageUrl = "https://cdn-icons-png.flaticon.com/512/738/738680.png",
            dAppName = "Radaswap"
        )
    }
}