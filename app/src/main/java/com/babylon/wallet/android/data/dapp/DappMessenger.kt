@file:Suppress("LongParameterList")

package com.babylon.wallet.android.data.dapp

import com.babylon.wallet.android.data.dapp.model.WalletErrorType
import com.babylon.wallet.android.data.dapp.model.WalletInteractionFailureResponse
import com.babylon.wallet.android.data.dapp.model.WalletInteractionResponse
import com.babylon.wallet.android.data.dapp.model.WalletInteractionSuccessResponse
import com.babylon.wallet.android.data.dapp.model.WalletTransactionResponseItems
import com.babylon.wallet.android.data.dapp.model.WalletTransactionResponseItems.SendTransactionResponseItem
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
interface DappMessenger {

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

    suspend fun sendWalletInteractionSuccessResponse(
        dappId: String,
        response: WalletInteractionResponse
    ): Result<Unit>
}

class DappMessengerImpl @Inject constructor(
    private val peerdroidClient: PeerdroidClient
) : DappMessenger {

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

    override suspend fun sendWalletInteractionSuccessResponse(
        dappId: String,
        response: WalletInteractionResponse
    ): Result<Unit> {
        val messageJson = try {
            Json.encodeToString(response)
        } catch (e: Exception) {
            Timber.d(e)
            ""
        }
        return when (peerdroidClient.sendMessage(dappId, messageJson)) {
            is rdx.works.peerdroid.helpers.Result.Success -> Result.Success(Unit)
            is rdx.works.peerdroid.helpers.Result.Error -> Result.Error()
        }
    }
}
