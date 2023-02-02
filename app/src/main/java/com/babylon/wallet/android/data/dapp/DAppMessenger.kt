package com.babylon.wallet.android.data.dapp

import com.babylon.wallet.android.data.dapp.model.AccountDto
import com.babylon.wallet.android.data.dapp.model.AuthLoginWithoutChallengeRequestResponseItem
import com.babylon.wallet.android.data.dapp.model.OneTimeAccountsWithoutProofOfOwnershipRequestResponseItem
import com.babylon.wallet.android.data.dapp.model.OngoingAccountsWithoutProofOfOwnershipRequestResponseItem
import com.babylon.wallet.android.data.dapp.model.PersonaDto
import com.babylon.wallet.android.data.dapp.model.SendTransactionResponseItem
import com.babylon.wallet.android.data.dapp.model.WalletAuthorizedRequestResponseItems
import com.babylon.wallet.android.data.dapp.model.WalletErrorType
import com.babylon.wallet.android.data.dapp.model.WalletInteractionFailureResponse
import com.babylon.wallet.android.data.dapp.model.WalletInteractionResponse
import com.babylon.wallet.android.data.dapp.model.WalletInteractionSuccessResponse
import com.babylon.wallet.android.data.dapp.model.WalletTransactionResponseItems
import com.babylon.wallet.android.data.dapp.model.WalletUnauthorizedRequestResponseItems
import com.babylon.wallet.android.data.dapp.model.toDataModel
import com.babylon.wallet.android.domain.common.Result
import com.babylon.wallet.android.presentation.dapp.account.AccountItemUiModel
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import rdx.works.profile.data.model.pernetwork.OnNetwork
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

    suspend fun sendWalletInteractionResponseFailure(
        requestId: String,
        error: WalletErrorType,
        message: String? = null
    ): Result<Unit>

    suspend fun sendTransactionWriteResponseSuccess(
        requestId: String,
        txId: String
    ): Result<Unit>

    suspend fun sendWalletInteractionSuccessResponse(
        interactionId: String,
        persona: OnNetwork.Persona,
        oneTimeAccounts: List<AccountItemUiModel>?,
        ongoingAccounts: List<AccountItemUiModel>?
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

    override suspend fun sendWalletInteractionResponseFailure(
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

    override suspend fun sendWalletInteractionSuccessResponse(
        interactionId: String,
        persona: OnNetwork.Persona,
        oneTimeAccounts: List<AccountItemUiModel>?,
        ongoingAccounts: List<AccountItemUiModel>?
    ): Result<Unit> {
        val walletSuccessResponse: WalletInteractionResponse = WalletInteractionSuccessResponse(
            interactionId = interactionId,
            items = WalletAuthorizedRequestResponseItems(
                auth = AuthLoginWithoutChallengeRequestResponseItem(
                    PersonaDto(
                        persona.address,
                        persona.displayName.orEmpty()
                    )
                ),
                oneTimeAccounts = oneTimeAccounts?.let { accounts ->
                    OneTimeAccountsWithoutProofOfOwnershipRequestResponseItem(
                        accounts.map {
                            AccountDto(it.address, it.displayName.orEmpty(), it.appearanceID)
                        }
                    )
                },
                ongoingAccounts = ongoingAccounts?.let { accounts ->
                    OngoingAccountsWithoutProofOfOwnershipRequestResponseItem(
                        accounts.map {
                            AccountDto(it.address, it.displayName.orEmpty(), it.appearanceID)
                        }
                    )
                }
            )
        )
        val messageJson = try {
            Json.encodeToString(walletSuccessResponse)
        } catch (e: Exception) {
            Timber.d(e)
            ""
        }
        return when (peerdroidClient.sendMessage(messageJson)) {
            is rdx.works.peerdroid.helpers.Result.Error -> Result.Error()
            is rdx.works.peerdroid.helpers.Result.Success -> {
                incomingRequestRepository.requestHandled(interactionId)
                Result.Success(Unit)
            }
        }
    }
}
