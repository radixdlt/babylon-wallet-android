@file:Suppress("LongParameterList")

package com.babylon.wallet.android.data.dapp

import com.babylon.wallet.android.data.dapp.model.Curve
import com.babylon.wallet.android.data.dapp.model.LedgerInteractionRequest
import com.babylon.wallet.android.domain.RadixWalletException
import com.babylon.wallet.android.domain.model.messages.LedgerResponse
import com.radixdlt.sargon.HierarchicalDeterministicPublicKey
import com.radixdlt.sargon.extensions.string
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

interface LedgerMessenger {

    val isAnyLinkedConnectorConnected: Flow<Boolean>

    suspend fun sendDeviceInfoRequest(interactionId: String): Result<LedgerResponse.GetDeviceInfoResponse>

    suspend fun signTransactionRequest(
        interactionId: String,
        hdPublicKeys: List<HierarchicalDeterministicPublicKey>,
        compiledTransactionIntent: String,
        ledgerDevice: LedgerInteractionRequest.LedgerDevice,
        displayHashOnLedgerDisplay: Boolean = true
    ): Result<LedgerResponse.SignTransactionResponse>

    suspend fun signSubintentHashRequest(
        interactionId: String,
        hdPublicKeys: List<HierarchicalDeterministicPublicKey>,
        subintentHash: String,
        ledgerDevice: LedgerInteractionRequest.LedgerDevice
    ): Result<LedgerResponse.SignSubintentHashResponse>

    suspend fun sendDerivePublicKeyRequest(
        interactionId: String,
        keyParameters: List<LedgerInteractionRequest.KeyParameters>,
        ledgerDevice: LedgerInteractionRequest.LedgerDevice
    ): Result<LedgerResponse.DerivePublicKeyResponse>

    suspend fun signChallengeRequest(
        interactionId: String,
        hdPublicKeys: List<HierarchicalDeterministicPublicKey>,
        ledgerDevice: LedgerInteractionRequest.LedgerDevice,
        challengeHex: String,
        origin: String,
        dAppDefinitionAddress: String
    ): Result<LedgerResponse.SignChallengeResponse>

    suspend fun deriveAndDisplayAddressRequest(
        interactionId: String,
        keyParameters: LedgerInteractionRequest.KeyParameters,
        ledgerDevice: LedgerInteractionRequest.LedgerDevice
    ): Result<LedgerResponse.DeriveAndDisplayAddressResponse>
}

class LedgerMessengerImpl @Inject constructor(
    private val peerdroidClient: PeerdroidClient,
    private val jsonSerializer: Json
) : LedgerMessenger {

    override val isAnyLinkedConnectorConnected: Flow<Boolean>
        get() = peerdroidClient.hasAtLeastOneConnection

    override suspend fun sendDeviceInfoRequest(interactionId: String): Result<LedgerResponse.GetDeviceInfoResponse> {
        val ledgerRequest: LedgerInteractionRequest = LedgerInteractionRequest.GetDeviceInfo(interactionId)
        return makeLedgerRequest(request = ledgerRequest, onError = {
            RadixWalletException.LedgerCommunicationException.FailedToGetDeviceId
        })
    }

    override suspend fun sendDerivePublicKeyRequest(
        interactionId: String,
        keyParameters: List<LedgerInteractionRequest.KeyParameters>,
        ledgerDevice: LedgerInteractionRequest.LedgerDevice
    ): Result<LedgerResponse.DerivePublicKeyResponse> {
        val ledgerRequest: LedgerInteractionRequest = LedgerInteractionRequest.DerivePublicKeys(
            interactionId = interactionId,
            keysParameters = keyParameters,
            ledgerDevice = ledgerDevice
        )
        return makeLedgerRequest(request = ledgerRequest, onError = {
            RadixWalletException.LedgerCommunicationException.FailedToDerivePublicKeys
        })
    }

    override suspend fun signTransactionRequest(
        interactionId: String,
        hdPublicKeys: List<HierarchicalDeterministicPublicKey>,
        compiledTransactionIntent: String,
        ledgerDevice: LedgerInteractionRequest.LedgerDevice,
        displayHashOnLedgerDisplay: Boolean
    ): Result<LedgerResponse.SignTransactionResponse> {
        val ledgerRequest: LedgerInteractionRequest = LedgerInteractionRequest.SignTransaction(
            interactionId = interactionId,
            signers = hdPublicKeys.map {
                LedgerInteractionRequest.KeyParameters(
                    Curve.from(it.publicKey),
                    it.derivationPath.string
                )
            },
            ledgerDevice = ledgerDevice,
            displayHash = displayHashOnLedgerDisplay,
            compiledTransactionIntent = compiledTransactionIntent,
            mode = LedgerInteractionRequest.SignTransaction.Mode.Summary
        )
        return makeLedgerRequest(request = ledgerRequest, onError = {
            RadixWalletException.LedgerCommunicationException.FailedToSignTransaction(it.code)
        })
    }

    override suspend fun signSubintentHashRequest(
        interactionId: String,
        hdPublicKeys: List<HierarchicalDeterministicPublicKey>,
        subintentHash: String,
        ledgerDevice: LedgerInteractionRequest.LedgerDevice,
    ): Result<LedgerResponse.SignSubintentHashResponse> {
        val ledgerRequest: LedgerInteractionRequest = LedgerInteractionRequest.SignSubintentHash(
            interactionId = interactionId,
            signers = hdPublicKeys.map {
                LedgerInteractionRequest.KeyParameters(
                    Curve.from(it.publicKey),
                    it.derivationPath.string
                )
            },
            ledgerDevice = ledgerDevice,
            subintentHash = subintentHash
        )
        return makeLedgerRequest(request = ledgerRequest, onError = {
            RadixWalletException.LedgerCommunicationException.FailedToSignTransaction(it.code)
        })
    }

    override suspend fun signChallengeRequest(
        interactionId: String,
        hdPublicKeys: List<HierarchicalDeterministicPublicKey>,
        ledgerDevice: LedgerInteractionRequest.LedgerDevice,
        challengeHex: String,
        origin: String,
        dAppDefinitionAddress: String
    ): Result<LedgerResponse.SignChallengeResponse> {
        val ledgerRequest: LedgerInteractionRequest = LedgerInteractionRequest.SignChallenge(
            interactionId = interactionId,
            signers = hdPublicKeys.map {
                LedgerInteractionRequest.KeyParameters(
                    Curve.from(it.publicKey),
                    it.derivationPath.string
                )
            },
            ledgerDevice = ledgerDevice,
            challengeHex = challengeHex,
            origin = origin,
            dAppDefinitionAddress = dAppDefinitionAddress
        )
        return makeLedgerRequest(request = ledgerRequest, onError = {
            RadixWalletException.LedgerCommunicationException.FailedToSignAuthChallenge
        })
    }

    override suspend fun deriveAndDisplayAddressRequest(
        interactionId: String,
        keyParameters: LedgerInteractionRequest.KeyParameters,
        ledgerDevice: LedgerInteractionRequest.LedgerDevice
    ): Result<LedgerResponse.DeriveAndDisplayAddressResponse> {
        val ledgerRequest = LedgerInteractionRequest.DeriveAndDisplayAddress(
            interactionId = interactionId,
            keyParameters = keyParameters,
            ledgerDevice = ledgerDevice
        )
        return makeLedgerRequest(request = ledgerRequest, onError = {
            RadixWalletException.LedgerCommunicationException.FailedToDeriveAndDisplayAddress
        })
    }

    private suspend inline fun <reified R : LedgerResponse> makeLedgerRequest(
        request: LedgerInteractionRequest,
        crossinline onError: (LedgerResponse.LedgerErrorResponse) -> RadixWalletException.LedgerCommunicationException
    ): Result<R> = flow<Result<R>> {
        peerdroidClient.sendMessage(jsonSerializer.encodeToString(request))
            .onSuccess {
                peerdroidClient.listenForLedgerResponses().filter { ledgerResponse ->
                    ledgerResponse.id == request.interactionId
                }.catch { e ->
                    emit(Result.failure(e))
                }.collect { response ->
                    when (response) {
                        is R -> emit(Result.success(response))
                        is LedgerResponse.LedgerErrorResponse -> {
                            emit(Result.failure(onError(response)))
                        }
                        else -> {}
                    }
                }
            }
            .onFailure { throwable ->
                emit(Result.failure(RadixWalletException.LedgerCommunicationException.FailedToConnect))
            }
    }.first()
}
