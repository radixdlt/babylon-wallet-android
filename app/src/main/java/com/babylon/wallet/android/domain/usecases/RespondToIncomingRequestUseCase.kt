package com.babylon.wallet.android.domain.usecases

import com.babylon.wallet.android.data.dapp.DappMessenger
import com.babylon.wallet.android.di.coroutines.IoDispatcher
import com.babylon.wallet.android.domain.model.messages.DappToWalletInteraction
import com.babylon.wallet.android.domain.model.messages.IncomingRequestResponse
import com.babylon.wallet.android.domain.model.messages.RemoteEntityID
import com.radixdlt.sargon.DappWalletInteractionErrorType
import com.radixdlt.sargon.Instant
import com.radixdlt.sargon.RadixConnectMobile
import com.radixdlt.sargon.RadixConnectMobileWalletResponse
import com.radixdlt.sargon.SessionId
import com.radixdlt.sargon.SignedSubintent
import com.radixdlt.sargon.TransactionIntentHash
import com.radixdlt.sargon.WalletToDappInteractionFailureResponse
import com.radixdlt.sargon.WalletToDappInteractionResponse
import com.radixdlt.sargon.WalletToDappInteractionResponseItems
import com.radixdlt.sargon.WalletToDappInteractionSendTransactionResponseItem
import com.radixdlt.sargon.WalletToDappInteractionSuccessResponse
import com.radixdlt.sargon.WalletToDappInteractionTransactionResponseItems
import com.radixdlt.sargon.extensions.toJson
import com.radixdlt.sargon.newWalletToDappInteractionPreAuthorizationResponseItems
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

class RespondToIncomingRequestUseCase @Inject constructor(
    private val dAppMessenger: DappMessenger,
    private val radixConnectMobile: RadixConnectMobile,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {

    suspend fun respondWithFailure(
        request: DappToWalletInteraction,
        dappWalletInteractionErrorType: DappWalletInteractionErrorType,
        message: String? = null
    ) = withContext(ioDispatcher) {
        val payload = WalletToDappInteractionResponse.Failure(
            v1 = WalletToDappInteractionFailureResponse(
                interactionId = request.interactionId,
                error = dappWalletInteractionErrorType,
                message = message
            )
        )
        when (request.remoteEntityId) {
            is RemoteEntityID.ConnectorId -> {
                dAppMessenger.sendWalletInteractionResponseFailure(
                    remoteConnectorId = request.remoteEntityId.id,
                    payload = payload.toJson()
                ).mapCatching { IncomingRequestResponse.SuccessCE }
            }

            is RemoteEntityID.RadixMobileConnectRemoteSession -> {
                runCatching {
                    radixConnectMobile.sendDappInteractionResponse(
                        RadixConnectMobileWalletResponse(SessionId.fromString(request.remoteEntityId.id), payload)
                    )
                }.onFailure {
                    Timber.d(it, "Failed to send failure response to Radix Mobile Connect")
                }.mapCatching {
                    IncomingRequestResponse.SuccessRadixMobileConnect
                }
            }
        }
    }

    suspend fun respondWithSuccess(
        request: DappToWalletInteraction,
        response: WalletToDappInteractionResponse
    ) = withContext(ioDispatcher) {
        when (request.remoteEntityId) {
            is RemoteEntityID.ConnectorId -> {
                dAppMessenger.sendWalletInteractionSuccessResponse(
                    remoteConnectorId = request.remoteEntityId.id,
                    response = response
                ).fold(onSuccess = {
                    Result.success(IncomingRequestResponse.SuccessCE)
                }, onFailure = {
                    Result.failure(it)
                })
            }

            is RemoteEntityID.RadixMobileConnectRemoteSession -> {
                runCatching {
                    radixConnectMobile.sendDappInteractionResponse(
                        RadixConnectMobileWalletResponse(
                            sessionId = SessionId.fromString(request.remoteEntityId.id),
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

    suspend fun respondWithSuccessTransactionIntent(
        request: DappToWalletInteraction,
        intentHash: TransactionIntentHash
    ) = withContext(ioDispatcher) {
        val payload = WalletToDappInteractionResponse.Success(
            v1 = WalletToDappInteractionSuccessResponse(
                interactionId = request.interactionId,
                items = WalletToDappInteractionResponseItems.Transaction(
                    v1 = WalletToDappInteractionTransactionResponseItems(
                        send = WalletToDappInteractionSendTransactionResponseItem(
                            transactionIntentHash = intentHash,
                        )
                    )
                )
            )
        )

        respondWithForAnyTransaction(request = request, payload = payload)
    }

    suspend fun respondWithSuccessSubintent(
        request: DappToWalletInteraction,
        signedSubintent: SignedSubintent
    ): Result<Instant> = withContext(ioDispatcher) {
        val responseItems = newWalletToDappInteractionPreAuthorizationResponseItems(
            signedSubintent = signedSubintent
        )
        val payload = WalletToDappInteractionResponse.Success(
            v1 = WalletToDappInteractionSuccessResponse(
                interactionId = request.interactionId,
                items = WalletToDappInteractionResponseItems.PreAuthorization(
                    v1 = responseItems
                )
            )
        )

        respondWithForAnyTransaction(request = request, payload = payload)
            .map { responseItems.response.expirationTimestamp }
    }

    private suspend fun respondWithForAnyTransaction(
        request: DappToWalletInteraction,
        payload: WalletToDappInteractionResponse.Success
    ) = when (request.remoteEntityId) {
        is RemoteEntityID.ConnectorId -> {
            dAppMessenger.sendTransactionWriteResponseSuccess(
                remoteConnectorId = request.remoteEntityId.id,
                payload = payload.toJson()
            ).mapCatching { IncomingRequestResponse.SuccessCE }
        }

        is RemoteEntityID.RadixMobileConnectRemoteSession -> {
            runCatching {
                radixConnectMobile.sendDappInteractionResponse(
                    RadixConnectMobileWalletResponse(
                        sessionId = SessionId.fromString(request.remoteEntityId.id),
                        response = payload
                    )
                )
            }.mapCatching {
                IncomingRequestResponse.SuccessRadixMobileConnect
            }
        }
    }
}
