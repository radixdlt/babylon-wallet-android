package com.babylon.wallet.android.domain.dapp

import com.babylon.wallet.android.data.dapp.DAppDetailsResponse
import com.babylon.wallet.android.data.dapp.RequestMethodWalletRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VerifyDAppUseCase @Inject constructor(
    private val dAppRepository: DAppRepository
) {

    /**
     * Get origin and dAppId from request payload metadata
     * Fetch well-known.json with origin(host)
     * Compare dAppIds to check if dApp is correct.
     * Later we will use definitionAddress, which is old DAppEntity
     */
    suspend operator fun invoke(
        connectionId: String = "Connection Id" // TODO this is to be provided when integrating with WebRtc
    ): DAppVerifyResult {
        val dAppPayloadRequest = dAppRepository.getDAppRequest(connectionId)
        val dAppId = dAppPayloadRequest.metadata.dAppId
        val origin = dAppPayloadRequest.metadata.origin

        var accountAddresses = 0
        dAppPayloadRequest.payload.forEach { payload ->
            when (payload) {
                is RequestMethodWalletRequest.AccountAddressesRequestMethodWalletRequest -> {
                    payload.numberOfAddresses?.let { numberOfAddresses ->
                        accountAddresses = numberOfAddresses
                    }
                }
                else -> {}
            }
        }

        // Fetch well-known.json
        val dAppWellKnown = dAppRepository.fetchWellKnown(host = origin)

        // Find dApp that we are attempting to connect to
        val wellKnownDApp = dAppWellKnown.dApps.find { dApp ->
            dApp.id == dAppId
        } ?: return DAppVerifyResult(verified = false)

        // Fetch dApp details i.e. url, dApp name etc
        val dAppDetails = dAppRepository.fetchDAppDetails(wellKnownDApp.id)

        return DAppVerifyResult(
            verified = true,
            dAppResult = DAppResult(
                dAppDetails = dAppDetails,
                accountAddresses = accountAddresses
            )
        )
    }
}

data class DAppVerifyResult(
    val verified: Boolean,
    val dAppResult: DAppResult? = null
)

data class DAppResult(
    val dAppDetails: DAppDetailsResponse,
    val accountAddresses: Int
)
