package com.babylon.wallet.android.data.dapp

import com.babylon.wallet.android.data.dapp.model.OneTimeAccountsWithoutProofOfOwnershipRequestResponseItem
import com.babylon.wallet.android.data.dapp.model.SendTransactionResponseItem
import com.babylon.wallet.android.data.dapp.model.WalletErrorType
import com.babylon.wallet.android.data.dapp.model.WalletInteractionFailureResponse
import com.babylon.wallet.android.data.dapp.model.WalletInteractionSuccessResponse
import com.babylon.wallet.android.data.dapp.model.WalletTransactionResponseItems
import com.babylon.wallet.android.data.dapp.model.WalletUnauthorizedRequestResponseItems
import com.babylon.wallet.android.data.dapp.model.toDataModel
import com.babylon.wallet.android.domain.common.Result
import com.babylon.wallet.android.presentation.dapp.account.AccountItemUiModel
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
        accounts: List<AccountItemUiModel>
    ): Result<Unit>

    suspend fun sendTransactionWriteResponseFailure(
        requestId: String,
        error: WalletErrorType,
        message: String? = null
    ): Result<Unit>

    suspend fun sendTransactionWriteResponseSuccess(
        requestId: String,
        txId: String
    ): Result<Unit>
}

class DAppMessengerImpl @Inject constructor(
    private val peerdroidClient: PeerdroidClient,
    private val incomingRequestRepository: IncomingRequestRepository
) : DAppMessenger {

    override suspend fun sendAccountsResponse(
        requestId: String,
        accounts: List<AccountItemUiModel>
    ): Result<Unit> {
        val responseItem = OneTimeAccountsWithoutProofOfOwnershipRequestResponseItem(
            accounts = accounts.toDataModel()
        )
        val walletResponse = WalletInteractionSuccessResponse(
            interactionId = requestId,
            items = WalletUnauthorizedRequestResponseItems(oneTimeAccounts = responseItem)
        )
        val json = Json.encodeToString(walletResponse)

        return when (peerdroidClient.sendMessage(json)) {
            is rdx.works.peerdroid.helpers.Result.Success -> {
                incomingRequestRepository.requestHandled(requestId)
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
        txId: String
    ): Result<Unit> {
        val message = Json.encodeToString(
            WalletInteractionSuccessResponse(
                interactionId = requestId,
                items = WalletTransactionResponseItems(SendTransactionResponseItem(txId))
            )
        )
        return when (peerdroidClient.sendMessage(message)) {
            is rdx.works.peerdroid.helpers.Result.Error -> Result.Error()
            is rdx.works.peerdroid.helpers.Result.Success -> {
                incomingRequestRepository.requestHandled(requestId)
                Result.Success(Unit)
            }
        }
    }

    override suspend fun sendTransactionWriteResponseFailure(
        requestId: String,
        error: WalletErrorType,
        message: String?
    ): Result<Unit> {
        val messageJson = Json.encodeToString(
            WalletInteractionFailureResponse(
                interactionId = requestId,
                error = error,
                message = message
            )
        )
        return when (peerdroidClient.sendMessage(messageJson)) {
            is rdx.works.peerdroid.helpers.Result.Error -> Result.Error()
            is rdx.works.peerdroid.helpers.Result.Success -> {
                incomingRequestRepository.requestHandled(requestId)
                Result.Success(Unit)
            }
        }
    }
}
