@file:Suppress("LongParameterList")

package com.babylon.wallet.android.data.dapp

import com.babylon.wallet.android.data.dapp.model.Curve
import com.babylon.wallet.android.data.dapp.model.DerivePublicKeyRequest
import com.babylon.wallet.android.data.dapp.model.GetDeviceInfoRequest
import com.babylon.wallet.android.data.dapp.model.LedgerInteractionRequest
import com.babylon.wallet.android.data.dapp.model.SignChallengeRequest
import com.babylon.wallet.android.data.dapp.model.SignTransactionRequest
import com.babylon.wallet.android.data.dapp.model.peerdroidRequestJson
import com.babylon.wallet.android.data.transaction.DappRequestException
import com.babylon.wallet.android.data.transaction.DappRequestFailure
import com.babylon.wallet.android.domain.model.MessageFromDataChannel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.encodeToString
import rdx.works.peerdroid.helpers.Result.Error
import rdx.works.peerdroid.helpers.Result.Success
import rdx.works.profile.data.model.factorsources.Slip10Curve
import javax.inject.Inject

interface LedgerMessenger {

    suspend fun sendDeviceInfoRequest(interactionId: String): Result<MessageFromDataChannel.LedgerResponse.GetDeviceInfoResponse>

    suspend fun signTransactionRequest(
        interactionId: String,
        signersDerivationPathToCurve: List<Pair<String, Slip10Curve>>,
        compiledTransactionIntent: String,
        ledgerDevice: DerivePublicKeyRequest.LedgerDevice,
        displayHashOnLedgerDisplay: Boolean = true
    ): Result<MessageFromDataChannel.LedgerResponse.SignTransactionResponse>

    suspend fun sendDerivePublicKeyRequest(
        interactionId: String,
        keyParameters: List<DerivePublicKeyRequest.KeyParameters>,
        ledgerDevice: DerivePublicKeyRequest.LedgerDevice
    ): Result<MessageFromDataChannel.LedgerResponse.DerivePublicKeyResponse>

    suspend fun signChallengeRequest(
        interactionId: String,
        signersDerivationPathToCurve: List<Pair<String, Slip10Curve>>,
        ledgerDevice: DerivePublicKeyRequest.LedgerDevice,
        challengeHex: String,
        origin: String,
        dAppDefinitionAddress: String
    ): Result<MessageFromDataChannel.LedgerResponse.SignChallengeResponse>
}

class LedgerMessengerImpl @Inject constructor(
    private val peerdroidClient: PeerdroidClient,
) : LedgerMessenger {

    override suspend fun sendDeviceInfoRequest(interactionId: String): Result<MessageFromDataChannel.LedgerResponse.GetDeviceInfoResponse> {
        val ledgerRequest: LedgerInteractionRequest = GetDeviceInfoRequest(interactionId)
        return makeLedgerRequest(request = ledgerRequest, onError = {
            DappRequestException(DappRequestFailure.LedgerCommunicationFailure.FailedToGetDeviceId)
        })
    }

    override suspend fun sendDerivePublicKeyRequest(
        interactionId: String,
        keyParameters: List<DerivePublicKeyRequest.KeyParameters>,
        ledgerDevice: DerivePublicKeyRequest.LedgerDevice
    ): Result<MessageFromDataChannel.LedgerResponse.DerivePublicKeyResponse> {
        val ledgerRequest: LedgerInteractionRequest = DerivePublicKeyRequest(
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
        ledgerDevice: DerivePublicKeyRequest.LedgerDevice,
        displayHashOnLedgerDisplay: Boolean
    ): Result<MessageFromDataChannel.LedgerResponse.SignTransactionResponse> {
        val ledgerRequest: LedgerInteractionRequest = SignTransactionRequest(
            interactionId = interactionId,
            signers = signersDerivationPathToCurve.map {
                DerivePublicKeyRequest.KeyParameters(
                    Curve.from(it.second),
                    it.first
                )
            },
            ledgerDevice = ledgerDevice,
            displayHash = displayHashOnLedgerDisplay,
            compiledTransactionIntent = compiledTransactionIntent,
            mode = SignTransactionRequest.Mode.Summary
        )
        return makeLedgerRequest(request = ledgerRequest, onError = {
            DappRequestException(DappRequestFailure.LedgerCommunicationFailure.FailedToSignTransaction(it.code))
        })
    }

    override suspend fun signChallengeRequest(
        interactionId: String,
        signersDerivationPathToCurve: List<Pair<String, Slip10Curve>>,
        ledgerDevice: DerivePublicKeyRequest.LedgerDevice,
        challengeHex: String,
        origin: String,
        dAppDefinitionAddress: String
    ): Result<MessageFromDataChannel.LedgerResponse.SignChallengeResponse> {
        val ledgerRequest: LedgerInteractionRequest = SignChallengeRequest(
            interactionId = interactionId,
            signers = signersDerivationPathToCurve.map {
                DerivePublicKeyRequest.KeyParameters(
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

    private suspend inline fun <reified R : MessageFromDataChannel.LedgerResponse> makeLedgerRequest(
        request: LedgerInteractionRequest,
        crossinline onError: (MessageFromDataChannel.LedgerResponse.LedgerErrorResponse) -> DappRequestException
    ): Result<R> = flow<Result<R>> {
        when (peerdroidClient.sendMessage(peerdroidRequestJson.encodeToString(request))) {
            is Success -> {
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
            is Error -> {
                emit(Result.failure(Exception("Failed to sign transaction with Ledger")))
            }
        }
    }.first()
}
