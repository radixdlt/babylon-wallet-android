@file:Suppress("LongParameterList")

package com.babylon.wallet.android.data.dapp

import com.radixdlt.sargon.WalletToDappInteractionResponse
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
        remoteConnectorId: String,
        payload: String
    ): Result<Unit>

    suspend fun sendTransactionWriteResponseSuccess(
        remoteConnectorId: String,
        payload: String
    ): Result<Unit>

    suspend fun sendWalletInteractionSuccessResponse(
        remoteConnectorId: String,
        response: WalletToDappInteractionResponse
    ): Result<Unit>
}

class DappMessengerImpl @Inject constructor(
    private val peerdroidClient: PeerdroidClient
) : DappMessenger {

    override suspend fun sendTransactionWriteResponseSuccess(
        remoteConnectorId: String,
        payload: String
    ): Result<Unit> {
        return peerdroidClient.sendMessage(remoteConnectorId, payload)
    }

    override suspend fun sendWalletInteractionResponseFailure(
        remoteConnectorId: String,
        payload: String
    ): Result<Unit> {
        return peerdroidClient.sendMessage(remoteConnectorId, payload)
    }

    override suspend fun sendWalletInteractionSuccessResponse(
        remoteConnectorId: String,
        response: WalletToDappInteractionResponse
    ): Result<Unit> {
        val messageJson = try {
            Json.encodeToString(response)
        } catch (e: Exception) {
            Timber.d(e)
            ""
        }
        return peerdroidClient.sendMessage(remoteConnectorId, messageJson)
    }
}
