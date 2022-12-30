package com.babylon.wallet.android.data.dapp

import com.babylon.wallet.android.domain.common.Result
import com.babylon.wallet.android.domain.dapp.DAppRepository
import javax.inject.Inject

// TODO temp class to mock the dapp verification

@Suppress("MagicNumber", "UnusedPrivateMember")
class DAppRepositoryImpl @Inject constructor(
//    private val gatewayClient: GatewayClient
) : DAppRepository {

    /**
     * Get origin and dAppId from request payload metadata
     * Fetch well-known.json with origin(host)
     * Compare dAppIds to check if dApp is correct.
     * Later we will use definitionAddress, which is old DAppEntity
     */
    override suspend fun verifyDApp(): Result<DAppResult> {
        return Result.Success(
            DAppResult(
                dAppDetails = DAppDetailsResponse(
                    imageUrl = "https://cdn-icons-png.flaticon.com/512/738/738680.png",
                    dAppName = "Radaswap"
                ),
                accountAddresses = 0
            )
        )
    }
}

data class DAppDetailsResponse(
    val imageUrl: String,
    val dAppName: String
)

data class DAppResult(
    val dAppDetails: DAppDetailsResponse,
    val accountAddresses: Int
)
