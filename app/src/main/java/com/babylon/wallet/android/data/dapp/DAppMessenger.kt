package com.babylon.wallet.android.data.dapp

import com.babylon.wallet.android.data.dapp.model.Account
import com.babylon.wallet.android.data.dapp.model.OneTimeAccountsRequestType
import com.babylon.wallet.android.data.dapp.model.OneTimeAccountsWithoutProofOfOwnershipResponseItem
import com.babylon.wallet.android.data.dapp.model.SendTransactionResponseItem
import com.babylon.wallet.android.data.dapp.model.WalletErrorResponse
import com.babylon.wallet.android.data.dapp.model.WalletErrorType
import com.babylon.wallet.android.data.dapp.model.WalletResponse
import com.babylon.wallet.android.domain.common.Result
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber
import javax.inject.Inject

/**
 * The main responsibility of this class is to
 * - build the (dapp) responses
 * - to send responses to dapp
 *
 */
interface DAppMessenger {

    suspend fun sendAccountsResponse(
        requestId: String,
        accounts: List<Account>,
    ): Result<Unit>

    suspend fun sendTransactionWriteResponseFailure(
        requestId: String,
        error: WalletErrorType,
        message: String? = null,
    ): Result<Unit>

    suspend fun sendTransactionWriteResponseSuccess(requestId: String, txId: String): Result<Unit>
}

class DAppMessengerImpl @Inject constructor(
    private val peerdroidClient: PeerdroidClient,
) : DAppMessenger {

    override suspend fun sendAccountsResponse(
        requestId: String,
        accounts: List<Account>,
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

    override suspend fun sendTransactionWriteResponseSuccess(
        requestId: String,
        txId: String,
    ): Result<Unit> {
        val message = Json.encodeToString(
            WalletResponse(
                requestId,
                listOf(SendTransactionResponseItem(requestType = SendTransactionResponseItem.REQUEST_TYPE,
                    transactionIntentHash = txId))
            )
        )
        return when (peerdroidClient.sendMessage(message)) {
            is rdx.works.peerdroid.helpers.Result.Error -> Result.Error()
            is rdx.works.peerdroid.helpers.Result.Success -> Result.Success(Unit)
        }
    }

    override suspend fun sendTransactionWriteResponseFailure(
        requestId: String,
        error: WalletErrorType,
        message: String?,
    ): Result<Unit> {
        val messageJson =
            Json.encodeToString(
                WalletErrorResponse(
                    requestId, error = error, message = message
                )
            )
        return when (peerdroidClient.sendMessage(messageJson)) {
            is rdx.works.peerdroid.helpers.Result.Error -> Result.Error()
            is rdx.works.peerdroid.helpers.Result.Success -> Result.Success(Unit)
        }
    }
}
