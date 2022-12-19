package com.babylon.wallet.android.data.dapp

import com.babylon.wallet.android.data.dapp.model.Account
import com.babylon.wallet.android.data.dapp.model.OneTimeAccountsRequestType
import com.babylon.wallet.android.data.dapp.model.OneTimeAccountsWithoutProofOfOwnershipResponseItem
import com.babylon.wallet.android.data.dapp.model.WalletResponse
import com.babylon.wallet.android.domain.common.Result
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber
import javax.inject.Inject

interface DAppMessenger {

    suspend fun sendAccountsResponse(
        requestId: String,
        accounts: List<Account>
    ): Result<Unit>
}

class DAppMessengerImpl @Inject constructor(
    private val peerdroidClient: PeerdroidClient
) : DAppMessenger {

    override suspend fun sendAccountsResponse(
        requestId: String,
        accounts: List<Account>
    ): Result<Unit> {
        val responseItem = OneTimeAccountsWithoutProofOfOwnershipResponseItem(
            requestType = OneTimeAccountsRequestType.ONE_TIME_ACCOUNTS_READ.requestType,
            accounts = accounts
        )
        val walletResponse = WalletResponse(
            requestId = requestId,
            items = listOf(responseItem)
        )
        val json = Json.encodeToString(walletResponse)

        return when (peerdroidClient.sendMessage(json)) {
            is rdx.works.peerdroid.helpers.Result.Success -> {
                Timber.d("successfully sent response with accounts")
                Result.Success(Unit)
            }
            is rdx.works.peerdroid.helpers.Result.Error -> {
                Timber.d("failed to send response with accounts")
                Result.Error()
            }
        }
    }
}
