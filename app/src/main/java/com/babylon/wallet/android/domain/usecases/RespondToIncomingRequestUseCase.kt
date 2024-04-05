package com.babylon.wallet.android.domain.usecases

import com.babylon.wallet.android.data.dapp.DappMessenger
import com.babylon.wallet.android.data.dapp.model.WalletErrorType
import com.babylon.wallet.android.data.dapp.model.WalletInteractionResponse
import com.babylon.wallet.android.data.dapp.model.peerdroidRequestJson
import com.babylon.wallet.android.data.repository.DappLinkRepository
import com.babylon.wallet.android.data.repository.RcrRepository
import com.babylon.wallet.android.di.coroutines.IoDispatcher
import com.babylon.wallet.android.domain.model.IncomingRequestResponse
import com.babylon.wallet.android.domain.model.MessageFromDataChannel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import rdx.works.core.decodeHex
import rdx.works.core.encrypt
import rdx.works.core.toHexString
import javax.inject.Inject

class RespondToIncomingRequestUseCase @Inject constructor(
    private val dAppMessenger: DappMessenger,
    private val rcrRepository: RcrRepository,
    private val dAppLinkRepository: DappLinkRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {

    suspend fun respondWithFailure(
        request: MessageFromDataChannel.IncomingRequest,
        error: WalletErrorType,
        message: String? = null
    ) =
        withContext(ioDispatcher) {
            when (request.remoteEntityId) {
                is MessageFromDataChannel.RemoteEntityID.ConnectorId -> {
                    dAppMessenger.sendWalletInteractionResponseFailure(
                        remoteConnectorId = request.remoteEntityId.value,
                        requestId = request.interactionId,
                        error = error,
                        message = message
                    ).fold(onSuccess = {
                        Result.success(IncomingRequestResponse.SuccessCE)
                    }, onFailure = {
                        Result.failure(it)
                    })
                }

                is MessageFromDataChannel.RemoteEntityID.RadixMobileConnectRemoteEntityId -> {
                    rcrRepository.sendResponse(
                        sessionId = request.remoteEntityId.value,
                        data = error.name
                    ).fold(onSuccess = {
                        Result.success(
                            IncomingRequestResponse.SuccessRadixMobileConnect(
                                "${request.metadata.origin}?sessionId=${request.remoteEntityId.value}&interactionId=${request.interactionId}"
                            )
                        )
                    }, onFailure = {
                        Result.failure(it)
                    })
                }
            }
        }

    suspend fun respondWithSuccess(
        request: MessageFromDataChannel.IncomingRequest,
        response: WalletInteractionResponse
    ) =
        withContext(ioDispatcher) {
            when (request.remoteEntityId) {
                is MessageFromDataChannel.RemoteEntityID.ConnectorId -> {
                    dAppMessenger.sendWalletInteractionSuccessResponse(
                        remoteConnectorId = request.remoteEntityId.value,
                        response = response
                    ).fold(onSuccess = {
                        Result.success(IncomingRequestResponse.SuccessCE)
                    }, onFailure = {
                        Result.failure(it)
                    })
                }

                is MessageFromDataChannel.RemoteEntityID.RadixMobileConnectRemoteEntityId -> {
                    val link =
                        dAppLinkRepository.getDappLink(request.remoteEntityId.value).getOrNull() ?: return@withContext Result.failure(
                            IllegalStateException("No dapp link found for session id ${request.remoteEntityId.value}")
                        )
                    val encryptedData =
                        peerdroidRequestJson.encodeToString(response).toByteArray().encrypt(link.secret.value.decodeHex()).getOrThrow()
                            .toHexString()
                    rcrRepository.sendResponse(
                        sessionId = request.remoteEntityId.value,
                        data = encryptedData
                    ).fold(onSuccess = {
                        Result.success(
                            IncomingRequestResponse.SuccessRadixMobileConnect(
                                "${link.origin}?sessionId=${request.remoteEntityId.value}${link.callbackPath}"
                            )
                        )
                    }, onFailure = {
                        Result.failure(it)
                    })
                }
            }
        }

    suspend fun respondWithSuccess(
        request: MessageFromDataChannel.IncomingRequest,
        txId: String
    ) =
        withContext(ioDispatcher) {
            when (request.remoteEntityId) {
                is MessageFromDataChannel.RemoteEntityID.ConnectorId -> {
                    dAppMessenger.sendTransactionWriteResponseSuccess(
                        remoteConnectorId = request.remoteEntityId.value,
                        requestId = request.interactionId,
                        txId = txId
                    )
                }

                is MessageFromDataChannel.RemoteEntityID.RadixMobileConnectRemoteEntityId -> {
//                    dAppLinkRepository.getDappLink(request.remoteEntityId.value).mapCatching { link ->
//                        val encryptedData =
//                            peerdroidRequestJson.encodeToString(response).toByteArray().encrypt(link.secret.value.decodeHex()).getOrThrow()
//                                .toHexString()
//                        rcrRepository.sendResponse(
//                            sessionId = request.remoteEntityId.value,
//                            data = encryptedData
//                        ).getOrThrow()
//                    }
                }
            }
        }

}