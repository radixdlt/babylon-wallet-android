package com.babylon.wallet.android.domain.usecases

import android.net.Uri
import com.babylon.wallet.android.data.dapp.DappMessenger
import com.babylon.wallet.android.data.dapp.model.WalletErrorType
import com.babylon.wallet.android.data.dapp.model.WalletInteractionFailureResponse
import com.babylon.wallet.android.data.dapp.model.WalletInteractionResponse
import com.babylon.wallet.android.data.dapp.model.WalletInteractionSuccessResponse
import com.babylon.wallet.android.data.dapp.model.WalletTransactionResponseItems
import com.babylon.wallet.android.data.dapp.model.peerdroidRequestJson
import com.babylon.wallet.android.data.repository.DappLinkRepository
import com.babylon.wallet.android.data.repository.RcrRepository
import com.babylon.wallet.android.di.coroutines.IoDispatcher
import com.babylon.wallet.android.domain.model.IncomingMessage
import com.babylon.wallet.android.domain.model.IncomingRequestResponse
import com.babylon.wallet.android.presentation.mobileconnect.DappLink
import com.babylon.wallet.android.utils.Constants
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

class RespondToIncomingRequestUseCase @Inject constructor(
    private val dAppMessenger: DappMessenger,
    private val rcrRepository: RcrRepository,
    private val dAppLinkRepository: DappLinkRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {

    suspend fun respondWithFailure(
        request: IncomingMessage.IncomingRequest,
        error: WalletErrorType,
        message: String? = null
    ) =
        withContext(ioDispatcher) {
            val payload = Json.encodeToString(
                WalletInteractionFailureResponse(
                    interactionId = request.interactionId,
                    error = error,
                    message = message
                )
            )
            when (request.remoteEntityId) {
                is IncomingMessage.RemoteEntityID.ConnectorId -> {
                    dAppMessenger.sendWalletInteractionResponseFailure(
                        remoteConnectorId = request.remoteEntityId.value,
                        payload = payload
                    ).fold(onSuccess = {
                        Result.success(IncomingRequestResponse.SuccessCE)
                    }, onFailure = {
                        Result.failure(it)
                    })
                }

                is IncomingMessage.RemoteEntityID.RadixMobileConnectRemoteSession -> {
                    val link =
                        dAppLinkRepository.getDappLink(request.remoteEntityId.value).getOrNull() ?: return@withContext Result.failure(
                            IllegalStateException("No dapp link found for session id ${request.remoteEntityId.value}")
                        )
                    rcrRepository.sendResponse(
                        sessionId = request.remoteEntityId.value,
                        data = payload
                    ).fold(onSuccess = {
                        Result.success(
                            IncomingRequestResponse.SuccessRadixMobileConnect(
                                buildResponseRedirectUrl(request, link)
                            )
                        )
                    }, onFailure = {
                        Result.failure(it)
                    })
                }
            }
        }

    suspend fun respondWithSuccess(
        request: IncomingMessage.IncomingRequest,
        response: WalletInteractionResponse
    ) =
        withContext(ioDispatcher) {
            when (request.remoteEntityId) {
                is IncomingMessage.RemoteEntityID.ConnectorId -> {
                    dAppMessenger.sendWalletInteractionSuccessResponse(
                        remoteConnectorId = request.remoteEntityId.value,
                        response = response
                    ).fold(onSuccess = {
                        Result.success(IncomingRequestResponse.SuccessCE)
                    }, onFailure = {
                        Result.failure(it)
                    })
                }

                is IncomingMessage.RemoteEntityID.RadixMobileConnectRemoteSession -> {
                    val link =
                        dAppLinkRepository.getDappLink(request.remoteEntityId.value).getOrNull() ?: return@withContext Result.failure(
                            IllegalStateException("No dapp link found for session id ${request.remoteEntityId.value}")
                        )
                    rcrRepository.sendResponse(
                        sessionId = request.remoteEntityId.value,
                        data = peerdroidRequestJson.encodeToString(response)
                    ).fold(onSuccess = {
                        Result.success(
                            IncomingRequestResponse.SuccessRadixMobileConnect(
                                buildResponseRedirectUrl(request, link)
                            )
                        )
                    }, onFailure = {
                        Result.failure(it)
                    })
                }
            }
        }

    suspend fun respondWithSuccess(
        request: IncomingMessage.IncomingRequest,
        txId: String
    ) =
        withContext(ioDispatcher) {
            val response: WalletInteractionResponse = WalletInteractionSuccessResponse(
                interactionId = request.interactionId,
                items = WalletTransactionResponseItems(WalletTransactionResponseItems.SendTransactionResponseItem(txId))
            )
            val payload = peerdroidRequestJson.encodeToString(response)
            when (request.remoteEntityId) {
                is IncomingMessage.RemoteEntityID.ConnectorId -> {
                    dAppMessenger.sendTransactionWriteResponseSuccess(
                        remoteConnectorId = request.remoteEntityId.value,
                        payload = payload
                    )
                }

                is IncomingMessage.RemoteEntityID.RadixMobileConnectRemoteSession -> {
                    val link =
                        dAppLinkRepository.getDappLink(request.remoteEntityId.value).getOrNull() ?: return@withContext Result.failure(
                            IllegalStateException("No dapp link found for session id ${request.remoteEntityId.value}")
                        )
                    rcrRepository.sendResponse(
                        sessionId = request.remoteEntityId.value,
                        data = payload
                    ).fold(onSuccess = {
                        Result.success(
                            IncomingRequestResponse.SuccessRadixMobileConnect(
                                buildResponseRedirectUrl(request, link)
                            )
                        )
                    }, onFailure = {
                        Result.failure(it)
                    })
                }
            }
        }

    private fun buildResponseRedirectUrl(
        request: IncomingMessage.IncomingRequest,
        link: DappLink
    ) = Uri.parse(request.metadata.origin).buildUpon().apply {
        appendQueryParameter(Constants.RadixMobileConnect.CONNECT_URL_PARAM_SESSION_ID, request.remoteEntityId.value)
        appendQueryParameter(Constants.RadixMobileConnect.CONNECT_URL_PARAM_INTERACTION_ID, request.interactionId)
        fragment(link.callbackPath?.replace("#", ""))
    }.build().toString()
}
