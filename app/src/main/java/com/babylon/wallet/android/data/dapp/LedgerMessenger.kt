@file:Suppress("LongParameterList")

package com.babylon.wallet.android.data.dapp

import com.babylon.wallet.android.data.dapp.model.Curve
import com.babylon.wallet.android.data.dapp.model.LedgerInteractionRequest
import com.babylon.wallet.android.data.dapp.model.peerdroidRequestJson
import com.babylon.wallet.android.data.transaction.DappRequestException
import com.babylon.wallet.android.data.transaction.DappRequestFailure
import com.babylon.wallet.android.domain.model.MessageFromDataChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.encodeToString
import rdx.works.profile.data.model.factorsources.Slip10Curve
import javax.inject.Inject

interface LedgerMessenger {

    val isConnected: Flow<Boolean>
    suspend fun sendDeviceInfoRequest(interactionId: String): Result<MessageFromDataChannel.LedgerResponse.GetDeviceInfoResponse>

    suspend fun signTransactionRequest(
        interactionId: String,
        signersDerivationPathToCurve: List<Pair<String, Slip10Curve>>,
        compiledTransactionIntent: String,
        ledgerDevice: LedgerInteractionRequest.LedgerDevice,
        displayHashOnLedgerDisplay: Boolean = true
    ): Result<MessageFromDataChannel.LedgerResponse.SignTransactionResponse>

    suspend fun sendDerivePublicKeyRequest(
        interactionId: String,
        keyParameters: List<LedgerInteractionRequest.KeyParameters>,
        ledgerDevice: LedgerInteractionRequest.LedgerDevice
    ): Result<MessageFromDataChannel.LedgerResponse.DerivePublicKeyResponse>

    suspend fun signChallengeRequest(
        interactionId: String,
        signersDerivationPathToCurve: List<Pair<String, Slip10Curve>>,
        ledgerDevice: LedgerInteractionRequest.LedgerDevice,
        challengeHex: String,
        origin: String,
        dAppDefinitionAddress: String
    ): Result<MessageFromDataChannel.LedgerResponse.SignChallengeResponse>

    suspend fun deriveAndDisplayAddressRequest(
        interactionId: String,
        keyParameters: LedgerInteractionRequest.KeyParameters,
        ledgerDevice: LedgerInteractionRequest.LedgerDevice
    ): Result<MessageFromDataChannel.LedgerResponse.DeriveAndDisplayAddressResponse>
}

class LedgerMessengerImpl @Inject constructor(
    private val peerdroidClient: PeerdroidClient,
) : LedgerMessenger {

    override val isConnected: Flow<Boolean>
        get() = peerdroidClient.hasAtLeastOneConnection

    override suspend fun sendDeviceInfoRequest(interactionId: String): Result<MessageFromDataChannel.LedgerResponse.GetDeviceInfoResponse> {
        val ledgerRequest: LedgerInteractionRequest = LedgerInteractionRequest.GetDeviceInfo(interactionId)
        return makeLedgerRequest(request = ledgerRequest, onError = {
            DappRequestException(DappRequestFailure.LedgerCommunicationFailure.FailedToGetDeviceId)
        })
    }

    override suspend fun sendDerivePublicKeyRequest(
        interactionId: String,
        keyParameters: List<LedgerInteractionRequest.KeyParameters>,
        ledgerDevice: LedgerInteractionRequest.LedgerDevice
    ): Result<MessageFromDataChannel.LedgerResponse.DerivePublicKeyResponse> {
        val ledgerRequest: LedgerInteractionRequest = LedgerInteractionRequest.DerivePublicKeys(
            interactionId = interactionId,
            keysParameters = keyParameters,
            ledgerDevice = ledgerDevice
        )
        return makeLedgerRequest(request = ledgerRequest, onError = {
            DappRequestException(DappRequestFailure.LedgerCommunicationFailure.FailedToDerivePublicKeys)
        })
    }

    override suspend fun signTransactionRequest(
        interactionId: String,
        signersDerivationPathToCurve: List<Pair<String, Slip10Curve>>,
        compiledTransactionIntent: String,
        ledgerDevice: LedgerInteractionRequest.LedgerDevice,
        displayHashOnLedgerDisplay: Boolean
    ): Result<MessageFromDataChannel.LedgerResponse.SignTransactionResponse> {
        val ledgerRequest: LedgerInteractionRequest = LedgerInteractionRequest.SignTransaction(
            interactionId = interactionId,
            signers = signersDerivationPathToCurve.map {
                LedgerInteractionRequest.KeyParameters(
                    Curve.from(it.second),
                    it.first
                )
            },
            ledgerDevice = ledgerDevice,
            displayHash = displayHashOnLedgerDisplay,
            compiledTransactionIntent = compiledTransactionIntent,
            mode = LedgerInteractionRequest.SignTransaction.Mode.Summary
        )
        return makeLedgerRequest(request = ledgerRequest, onError = {
            DappRequestException(DappRequestFailure.LedgerCommunicationFailure.FailedToSignTransaction(it.code))
        })
    }

    override suspend fun signChallengeRequest(
        interactionId: String,
        signersDerivationPathToCurve: List<Pair<String, Slip10Curve>>,
        ledgerDevice: LedgerInteractionRequest.LedgerDevice,
        challengeHex: String,
        origin: String,
        dAppDefinitionAddress: String
    ): Result<MessageFromDataChannel.LedgerResponse.SignChallengeResponse> {
        val ledgerRequest: LedgerInteractionRequest = LedgerInteractionRequest.SignChallenge(
            interactionId = interactionId,
            signers = signersDerivationPathToCurve.map {
                LedgerInteractionRequest.KeyParameters(
                    Curve.from(it.second),
                    it.first
                )
            },
            ledgerDevice = ledgerDevice,
            challengeHex = challengeHex,
            origin = origin,
            dAppDefinitionAddress = dAppDefinitionAddress
        )
        return makeLedgerRequest(request = ledgerRequest, onError = {
            DappRequestException(DappRequestFailure.LedgerCommunicationFailure.FailedToDerivePublicKeys)
        })
    }

    override suspend fun deriveAndDisplayAddressRequest(
        interactionId: String,
        keyParameters: LedgerInteractionRequest.KeyParameters,
        ledgerDevice: LedgerInteractionRequest.LedgerDevice
    ): Result<MessageFromDataChannel.LedgerResponse.DeriveAndDisplayAddressResponse> {
        val ledgerRequest = LedgerInteractionRequest.DeriveAndDisplayAddress(
            interactionId = interactionId,
            keyParameters = keyParameters,
            ledgerDevice = ledgerDevice
        )
        return makeLedgerRequest(request = ledgerRequest, onError = {
            DappRequestException(DappRequestFailure.LedgerCommunicationFailure.FailedToDeriveAndDisplayAddress)
        })
    }

    private suspend inline fun <reified R : MessageFromDataChannel.LedgerResponse> makeLedgerRequest(
        request: LedgerInteractionRequest,
        crossinline onError: (MessageFromDataChannel.LedgerResponse.LedgerErrorResponse) -> DappRequestException
    ): Result<R> = flow<Result<R>> {
        peerdroidClient.sendMessage(peerdroidRequestJson.encodeToString(request))
            .onSuccess {
                peerdroidClient.listenForLedgerResponses().filter {
                    it.id == request.interactionId
                }.catch { e ->
                    emit(Result.failure(e))
                }.collect { response ->
                    when (response) {
                        is R -> emit(Result.success(response))
                        is MessageFromDataChannel.LedgerResponse.LedgerErrorResponse -> {
                            emit(Result.failure(onError(response)))
                        }
                        else -> {}
                    }
                }
            }
            .onFailure { throwable ->
                emit(Result.failure(Exception("Failed to connect Ledger device ", throwable)))
            }
    }.first()
}
