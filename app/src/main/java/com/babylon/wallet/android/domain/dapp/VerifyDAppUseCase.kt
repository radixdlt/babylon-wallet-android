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
        connectionId: String = "Connection Id"//TODO this is to be provided when integrating with WebRtc
    ): DAppVerifyResult {
        val dAppPayloadRequest = dAppRepository.getDAppRequest(connectionId)
        val dAppId = dAppPayloadRequest.metadata.dAppId
        val origin = dAppPayloadRequest.metadata.origin

        val dAppWellKnown = dAppRepository.fetchWellKnown(host = origin)
        dAppWellKnown.dApps.forEach { dApp ->
            if (dApp.id == dAppId) {
                var accountAddresses = 0
                val dAppDetails = dAppRepository.fetchDAppDetails(dAppId)
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
                return DAppVerifyResult(
                    verified = true,
                    dAppResult = DAppResult(
                        dAppDetails = dAppDetails,
                        accountAddresses = accountAddresses
                    )
                )
            }
        }
        return DAppVerifyResult(verified = false)
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