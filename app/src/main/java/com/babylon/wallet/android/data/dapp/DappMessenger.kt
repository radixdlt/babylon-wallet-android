@file:Suppress("LongParameterList")

package com.babylon.wallet.android.data.dapp

import com.babylon.wallet.android.data.dapp.model.WalletErrorType
import com.babylon.wallet.android.data.dapp.model.WalletInteractionFailureResponse
import com.babylon.wallet.android.data.dapp.model.WalletInteractionResponse
import com.babylon.wallet.android.data.dapp.model.WalletInteractionSuccessResponse
import com.babylon.wallet.android.data.dapp.model.WalletTransactionResponseItems
import com.babylon.wallet.android.data.dapp.model.WalletTransactionResponseItems.SendTransactionResponseItem
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import rdx.works.core.blake2Hash
import rdx.works.core.encrypt
import rdx.works.core.toHexString
import timber.log.Timber
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
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
        requestId: String,
        error: WalletErrorType,
        message: String? = null,
        encryptionKey: ByteArray? = null
    ): Result<Unit>

    suspend fun sendTransactionWriteResponseSuccess(
        remoteConnectorId: String,
        requestId: String,
        txId: String,
        encryptionKey: ByteArray? = null
    ): Result<Unit>

    suspend fun sendWalletInteractionSuccessResponse(
        remoteConnectorId: String,
        response: WalletInteractionResponse,
        encryptionKey: ByteArray? = null
    ): Result<Unit>
}

class DappMessengerImpl @Inject constructor(
    private val peerdroidClient: PeerdroidClient
) : DappMessenger {

    override suspend fun sendTransactionWriteResponseSuccess(
        remoteConnectorId: String,
        requestId: String,
        txId: String,
        encryptionKey: ByteArray?
    ): Result<Unit> {
        val response: WalletInteractionResponse = WalletInteractionSuccessResponse(
            interactionId = requestId,
            items = WalletTransactionResponseItems(SendTransactionResponseItem(txId))
        )
        val message = Json.encodeToString(response)
        return peerdroidClient.sendMessage(remoteConnectorId, message)
    }

    override suspend fun sendWalletInteractionResponseFailure(
        remoteConnectorId: String,
        requestId: String,
        error: WalletErrorType,
        message: String?,
        encryptionKey: ByteArray?
    ): Result<Unit> {
        val messageJson = Json.encodeToString(
            WalletInteractionFailureResponse(
                interactionId = requestId,
                error = error,
                message = message
            )
        )
        return peerdroidClient.sendMessage(remoteConnectorId, messageJson)
    }

    override suspend fun sendWalletInteractionSuccessResponse(
        remoteConnectorId: String,
        response: WalletInteractionResponse,
        encryptionKey: ByteArray?
    ): Result<Unit> {
        val messageJson = try {
            Json.encodeToString(response)
        } catch (e: Exception) {
            Timber.d(e)
            ""
        }

        if (remoteConnectorId == "deepLink") {
            if(encryptionKey != null) {
                val connectionId = encryptionKey.blake2Hash().toHexString()
                val url = "https://ddjdmrlme9v4i.cloudfront.net/api/dapp-request/${connectionId}"
                val encryptedMessage = messageJson.toByteArray().encrypt(
                    withEncryptionKey = encryptionKey
                ).getOrNull()!!.toHexString()

                val response = RaMSDappResponse(response = encryptedMessage)
                val result = uploadData(url, response)
                val x = result
            }
            return Result.success(Unit)
        } else {
            return peerdroidClient.sendMessage(remoteConnectorId, messageJson)
        }
    }

    private suspend fun uploadData(urlString: String, apiResponse: RaMSDappResponse): String = withContext(
        Dispatchers.IO) {
        var urlConnection: HttpURLConnection? = null
        try {
            val jsonData = try {
                Json.encodeToString(apiResponse)
            } catch (e: Exception) {
                Timber.d(e)
                ""
            }
            val url = URL(urlString)
            urlConnection = url.openConnection() as HttpURLConnection
            urlConnection.requestMethod = "POST"
            urlConnection.setRequestProperty("Content-Type", "application/json; utf-8")
            urlConnection.setRequestProperty("Accept", "application/json")
            urlConnection.doOutput = true

            BufferedWriter(OutputStreamWriter(urlConnection.outputStream, "UTF-8")).use { writer ->
                writer.write(jsonData)
                writer.flush()
            }

            val responseCode = urlConnection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val inStream = urlConnection.inputStream
                inStream.bufferedReader().use { it.readText() } // Read response
            } else {
                "Error: Server returned response code: $responseCode"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            "Error: ${e.message}"
        } finally {
            urlConnection?.disconnect()
        }
    }
}

data class RaMSDappResponse(
    val response: String
)
