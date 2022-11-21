package com.babylon.wallet.android.data.dapp

import com.babylon.wallet.android.data.dapp.model.RequestMethodWalletRequest
import com.babylon.wallet.android.domain.Result
import com.babylon.wallet.android.domain.dapp.DAppRepository
import kotlinx.coroutines.delay
import javax.inject.Inject
import kotlin.random.Random

@Suppress("MagicNumber", "UnusedPrivateMember") // TODO this is temporarily here.
class DAppRepositoryImpl @Inject constructor(
    private val peerdroidClient: PeerdroidClient // will be used in the next PR
//    private val gatewayClient: GatewayClient
) : DAppRepository {

    /**
     * Get origin and dAppId from request payload metadata
     * Fetch well-known.json with origin(host)
     * Compare dAppIds to check if dApp is correct.
     * Later we will use definitionAddress, which is old DAppEntity
     */
    override suspend fun verifyDApp(): Result<DAppResult> {
        val dAppPayloadRequest = getDAppRequest()
        val dAppId = dAppPayloadRequest.metadata.dAppId
        val origin = dAppPayloadRequest.metadata.origin

        var accountAddresses = 0
        dAppPayloadRequest.payload.forEach { payload ->
            payload.numberOfAddresses?.let { numberOfAddresses ->
                accountAddresses = numberOfAddresses
            }
        }

        // Fetch well-known.json
        val dAppWellKnown = fetchWellKnown(host = origin)

        // Find dApp that we are attempting to connect to
        val wellKnownDApp = dAppWellKnown.dApps.find { dApp ->
            dApp.id == dAppId
        } ?: return Result.Error(
            "Failed to verify dApp"
        )

        // Fetch dApp details i.e. url, dApp name etc
        val dAppDetails = fetchDAppDetails(wellKnownDApp.id)

        return Result.Success(
            DAppResult(
                dAppDetails = dAppDetails,
                accountAddresses = accountAddresses
            )
        )
    }

    override suspend fun getDAppRequest(): RequestMethodWalletRequest {
        delay(Random.nextLong(500, 1500))
        return RequestMethodWalletRequest(
            "",
            "",
            payload = listOf(
                RequestMethodWalletRequest.AccountAddressesRequestMethodWalletRequest(
                    requestType = RequestMethodWalletRequest.RequestType.ACCOUNT_ADDRESSES.value,
                    numberOfAddresses = 1
                )
            ),
            RequestMethodWalletRequest.RequestMethodMetadata(
                "",
                "",
                dAppId = "DAppId007"
            )
        )
    }

    override suspend fun fetchWellKnown(host: String): DAppWellKnownResponse {
        delay(Random.nextLong(500, 1500))
        return DAppWellKnownResponse(
            listOf(
                DApp(
                    id = "DAppId007",
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

data class DAppResult(
    val dAppDetails: DAppDetailsResponse,
    val accountAddresses: Int
)
