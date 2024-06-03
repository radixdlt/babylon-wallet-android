package com.babylon.wallet.android.domain.usecases

import com.babylon.wallet.android.data.dapp.DappMessenger
import com.babylon.wallet.android.data.dapp.model.asJsonString
import com.babylon.wallet.android.data.repository.RcrRepository
import com.babylon.wallet.android.di.coroutines.IoDispatcher
import com.babylon.wallet.android.domain.model.IncomingMessage
import com.babylon.wallet.android.domain.model.IncomingRequestResponse
import com.radixdlt.sargon.DappWalletInteractionErrorType
import com.radixdlt.sargon.WalletToDappInteractionFailureResponse
import com.radixdlt.sargon.WalletToDappInteractionResponse
import com.radixdlt.sargon.WalletToDappInteractionResponseItems
import com.radixdlt.sargon.WalletToDappInteractionSendTransactionResponseItem
import com.radixdlt.sargon.WalletToDappInteractionSuccessResponse
import com.radixdlt.sargon.WalletToDappInteractionTransactionResponseItems
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject

class RespondToIncomingRequestUseCase @Inject constructor(
    private val dAppMessenger: DappMessenger,
    private val rcrRepository: RcrRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {

    suspend fun respondWithFailure(
        request: IncomingMessage.IncomingRequest,
        error: DappWalletInteractionErrorType,
        message: String? = null
    ) =
        withContext(ioDispatcher) {
            WalletToDappInteractionResponse.Failure(
                v1 = WalletToDappInteractionFailureResponse(
                    interactionId = request.interactionId,
                    error = error,
                    message = message
                )
            ).asJsonString().mapCatching { payload ->
                when (request.remoteEntityId) {
                    is IncomingMessage.RemoteEntityID.ConnectorId -> {
                        dAppMessenger.sendWalletInteractionResponseFailure(
                            remoteConnectorId = request.remoteEntityId.value,
                            payload = payload
                        ).mapCatching { IncomingRequestResponse.SuccessCE }.getOrThrow()
                    }

                    is IncomingMessage.RemoteEntityID.RadixMobileConnectRemoteSession -> {
                        rcrRepository.sendResponse(
                            sessionId = request.remoteEntityId.value,
                            data = payload
                        ).mapCatching {
                            IncomingRequestResponse.SuccessRadixMobileConnect
                        }.getOrThrow()
                    }
                }
            }
        }

    suspend fun respondWithSuccess(
        request: IncomingMessage.IncomingRequest,
        response: WalletToDappInteractionResponse
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
                    rcrRepository.sendResponse(
                        sessionId = request.remoteEntityId.value,
                        data = response.asJsonString().getOrThrow()
                    ).fold(onSuccess = {
                        Result.success(
                            IncomingRequestResponse.SuccessRadixMobileConnect
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
            WalletToDappInteractionResponse.Success(
                v1 = WalletToDappInteractionSuccessResponse(
                    interactionId = request.interactionId,
                    items = WalletToDappInteractionResponseItems.Transaction(
                        v1 = WalletToDappInteractionTransactionResponseItems(
                            send = WalletToDappInteractionSendTransactionResponseItem(
                                bech32EncodedTxId = txId
                            )
                        )
                    )
                )
            ).asJsonString().mapCatching { payload ->
                when (request.remoteEntityId) {
                    is IncomingMessage.RemoteEntityID.ConnectorId -> {
                        dAppMessenger.sendTransactionWriteResponseSuccess(
                            remoteConnectorId = request.remoteEntityId.value,
                            payload = payload
                        ).mapCatching { IncomingRequestResponse.SuccessCE }.getOrThrow()
                    }

                    is IncomingMessage.RemoteEntityID.RadixMobileConnectRemoteSession -> {
                        rcrRepository.sendResponse(
                            sessionId = request.remoteEntityId.value,
                            data = payload
                        ).mapCatching {
                            IncomingRequestResponse.SuccessRadixMobileConnect
                        }.getOrThrow()
                    }
                }
            }
        }
}
