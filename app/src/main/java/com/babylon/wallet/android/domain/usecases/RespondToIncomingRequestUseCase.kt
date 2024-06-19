package com.babylon.wallet.android.domain.usecases

import com.babylon.wallet.android.data.dapp.DappMessenger
import com.babylon.wallet.android.di.coroutines.IoDispatcher
import com.babylon.wallet.android.domain.model.IncomingMessage
import com.babylon.wallet.android.domain.model.IncomingRequestResponse
import com.radixdlt.sargon.DappWalletInteractionErrorType
import com.radixdlt.sargon.IntentHash
import com.radixdlt.sargon.RadixConnectMobile
import com.radixdlt.sargon.RadixConnectMobileWalletResponse
import com.radixdlt.sargon.SessionId
import com.radixdlt.sargon.WalletToDappInteractionFailureResponse
import com.radixdlt.sargon.WalletToDappInteractionResponse
import com.radixdlt.sargon.WalletToDappInteractionResponseItems
import com.radixdlt.sargon.WalletToDappInteractionSendTransactionResponseItem
import com.radixdlt.sargon.WalletToDappInteractionSuccessResponse
import com.radixdlt.sargon.WalletToDappInteractionTransactionResponseItems
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.toJson
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject

class RespondToIncomingRequestUseCase @Inject constructor(
    private val dAppMessenger: DappMessenger,
    private val radixConnectMobile: RadixConnectMobile,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {

    suspend fun respondWithFailure(
        request: IncomingMessage.IncomingRequest,
        error: DappWalletInteractionErrorType,
        message: String? = null
    ) =
        withContext(ioDispatcher) {
            val payload = WalletToDappInteractionResponse.Failure(
                v1 = WalletToDappInteractionFailureResponse(
                    interactionId = request.interactionId,
                    error = error,
                    message = message
                )
            )
            when (request.remoteEntityId) {
                is IncomingMessage.RemoteEntityID.ConnectorId -> {
                    dAppMessenger.sendWalletInteractionResponseFailure(
                        remoteConnectorId = request.remoteEntityId.value,
                        payload = payload.toJson()
                    ).mapCatching { IncomingRequestResponse.SuccessCE }
                }

                is IncomingMessage.RemoteEntityID.RadixMobileConnectRemoteSession -> {
                    runCatching {
                        radixConnectMobile.sendDappInteractionResponse(
                            RadixConnectMobileWalletResponse(SessionId.fromString(request.remoteEntityId.value), payload)
                        )
                    }.mapCatching {
                        IncomingRequestResponse.SuccessRadixMobileConnect
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
                    runCatching {
                        radixConnectMobile.sendDappInteractionResponse(
                            RadixConnectMobileWalletResponse(
                                sessionId = SessionId.fromString(request.remoteEntityId.value),
                                response = response
                            )
                        )
                    }.fold(onSuccess = {
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
            val payload = WalletToDappInteractionResponse.Success(
                v1 = WalletToDappInteractionSuccessResponse(
                    interactionId = request.interactionId,
                    items = WalletToDappInteractionResponseItems.Transaction(
                        v1 = WalletToDappInteractionTransactionResponseItems(
                            send = WalletToDappInteractionSendTransactionResponseItem(
                                transactionIntentHash = IntentHash.init(txId),
                            )
                        )
                    )
                )
            )
            when (request.remoteEntityId) {
                is IncomingMessage.RemoteEntityID.ConnectorId -> {
                    dAppMessenger.sendTransactionWriteResponseSuccess(
                        remoteConnectorId = request.remoteEntityId.value,
                        payload = payload.toJson()
                    ).mapCatching { IncomingRequestResponse.SuccessCE }
                }

                is IncomingMessage.RemoteEntityID.RadixMobileConnectRemoteSession -> {
                    runCatching {
                        radixConnectMobile.sendDappInteractionResponse(
                            RadixConnectMobileWalletResponse(
                                sessionId = SessionId.fromString(request.remoteEntityId.value),
                                response = payload
                            )
                        )
                    }.mapCatching {
                        IncomingRequestResponse.SuccessRadixMobileConnect
                    }
                }
            }
        }
}
