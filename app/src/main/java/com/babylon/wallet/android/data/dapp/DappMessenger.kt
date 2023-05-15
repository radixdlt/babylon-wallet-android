@file:Suppress("LongParameterList")

package com.babylon.wallet.android.data.dapp

import com.babylon.wallet.android.data.dapp.model.AuthLoginWithoutChallengeRequestResponseItem
import com.babylon.wallet.android.data.dapp.model.AuthUsePersonaRequestResponseItem
import com.babylon.wallet.android.data.dapp.model.Persona
import com.babylon.wallet.android.data.dapp.model.WalletAuthorizedRequestResponseItems
import com.babylon.wallet.android.data.dapp.model.WalletErrorType
import com.babylon.wallet.android.data.dapp.model.WalletInteractionFailureResponse
import com.babylon.wallet.android.data.dapp.model.WalletInteractionResponse
import com.babylon.wallet.android.data.dapp.model.WalletInteractionSuccessResponse
import com.babylon.wallet.android.data.dapp.model.WalletTransactionResponseItems
import com.babylon.wallet.android.data.dapp.model.WalletTransactionResponseItems.SendTransactionResponseItem
import com.babylon.wallet.android.data.dapp.model.WalletUnauthorizedRequestResponseItems
import com.babylon.wallet.android.data.dapp.model.toDataModel
import com.babylon.wallet.android.domain.common.Result
import com.babylon.wallet.android.presentation.dapp.authorized.account.AccountItemUiModel
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import rdx.works.profile.data.model.pernetwork.Network
import timber.log.Timber
import javax.inject.Inject

/**
 * The main responsibility of this class is to
 * - build the (dapp) responses
 * - to send responses to dapp
 *
 */
interface DappMessenger {

    suspend fun sendWalletInteractionUnauthorizedSuccessResponse(
        dappId: String,
        requestId: String,
        oneTimeAccounts: List<AccountItemUiModel> = emptyList(),
        onetimeDataFields: List<Network.Persona.Field> = emptyList()
    ): Result<Unit>

    suspend fun sendWalletInteractionResponseFailure(
        dappId: String,
        requestId: String,
        error: WalletErrorType,
        message: String? = null
    ): Result<Unit>

    suspend fun sendTransactionWriteResponseSuccess(
        dappId: String,
        requestId: String,
        txId: String
    ): Result<Unit>

    suspend fun sendWalletInteractionAuthorizedSuccessResponse(
        dappId: String,
        interactionId: String,
        persona: Network.Persona,
        usePersona: Boolean,
        oneTimeAccounts: List<AccountItemUiModel> = emptyList(),
        ongoingAccounts: List<AccountItemUiModel> = emptyList(),
        ongoingDataFields: List<Network.Persona.Field> = emptyList(),
        onetimeDataFields: List<Network.Persona.Field> = emptyList()
    ): Result<Unit>
}

class DappMessengerImpl @Inject constructor(
    private val peerdroidClient: PeerdroidClient
) : DappMessenger {

    override suspend fun sendWalletInteractionUnauthorizedSuccessResponse(
        dappId: String,
        requestId: String,
        oneTimeAccounts: List<AccountItemUiModel>,
        onetimeDataFields: List<Network.Persona.Field>
    ): Result<Unit> {
        val walletResponse: WalletInteractionResponse = WalletInteractionSuccessResponse(
            interactionId = requestId,
            items = WalletUnauthorizedRequestResponseItems(
                oneTimeAccounts = oneTimeAccounts.toDataModel(),
                oneTimePersonaData = onetimeDataFields.toDataModel()
            )
        )
        val json = Json.encodeToString(walletResponse)

        return when (peerdroidClient.sendMessage(dappId, json)) {
            is rdx.works.peerdroid.helpers.Result.Success -> Result.Success(Unit)
            is rdx.works.peerdroid.helpers.Result.Error -> Result.Error()
        }
    }

    override suspend fun sendTransactionWriteResponseSuccess(
        dappId: String,
        requestId: String,
        txId: String
    ): Result<Unit> {
        val response: WalletInteractionResponse = WalletInteractionSuccessResponse(
            interactionId = requestId,
            items = WalletTransactionResponseItems(SendTransactionResponseItem(txId))
        )
        val message = Json.encodeToString(response)
        return when (peerdroidClient.sendMessage(dappId, message)) {
            is rdx.works.peerdroid.helpers.Result.Success -> Result.Success(Unit)
            is rdx.works.peerdroid.helpers.Result.Error -> Result.Error()
        }
    }

    override suspend fun sendWalletInteractionResponseFailure(
        dappId: String,
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
        return when (peerdroidClient.sendMessage(dappId, messageJson)) {
            is rdx.works.peerdroid.helpers.Result.Success -> Result.Success(Unit)
            is rdx.works.peerdroid.helpers.Result.Error -> Result.Error()
        }
    }

    override suspend fun sendWalletInteractionAuthorizedSuccessResponse(
        dappId: String,
        interactionId: String,
        persona: Network.Persona,
        usePersona: Boolean,
        oneTimeAccounts: List<AccountItemUiModel>,
        ongoingAccounts: List<AccountItemUiModel>,
        ongoingDataFields: List<Network.Persona.Field>,
        onetimeDataFields: List<Network.Persona.Field>
    ): Result<Unit> {
        val walletSuccessResponse: WalletInteractionResponse = buildSuccessResponse(
            interactionId = interactionId,
            usePersona = usePersona,
            persona = persona,
            oneTimeAccounts = oneTimeAccounts,
            ongoingAccounts = ongoingAccounts,
            ongoingDataFields = ongoingDataFields,
            onetimeDataFields = onetimeDataFields
        )
        val messageJson = try {
            Json.encodeToString(walletSuccessResponse)
        } catch (e: Exception) {
            Timber.d(e)
            ""
        }
        return when (peerdroidClient.sendMessage(dappId, messageJson)) {
            is rdx.works.peerdroid.helpers.Result.Success -> Result.Success(Unit)
            is rdx.works.peerdroid.helpers.Result.Error -> Result.Error()
        }
    }

    private fun buildSuccessResponse(
        interactionId: String,
        usePersona: Boolean,
        persona: Network.Persona,
        oneTimeAccounts: List<AccountItemUiModel>,
        ongoingAccounts: List<AccountItemUiModel>,
        ongoingDataFields: List<Network.Persona.Field>,
        onetimeDataFields: List<Network.Persona.Field>
    ): WalletInteractionResponse {
        val walletSuccessResponse: WalletInteractionResponse = WalletInteractionSuccessResponse(
            interactionId = interactionId,
            items = WalletAuthorizedRequestResponseItems(
                auth = if (usePersona) {
                    AuthUsePersonaRequestResponseItem(
                        Persona(
                            persona.address,
                            persona.displayName
                        )
                    )
                } else {
                    AuthLoginWithoutChallengeRequestResponseItem(
                        Persona(
                            persona.address,
                            persona.displayName
                        )
                    )
                },
                oneTimeAccounts = oneTimeAccounts.toDataModel(),
                ongoingAccounts = ongoingAccounts.toDataModel(),
                ongoingPersonaData = ongoingDataFields.toDataModel(),
                oneTimePersonaData = onetimeDataFields.toDataModel()
            )
        )
        return walletSuccessResponse
    }
}
