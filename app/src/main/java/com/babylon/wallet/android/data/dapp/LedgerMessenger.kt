package com.babylon.wallet.android.data.dapp

import com.babylon.wallet.android.data.dapp.model.Curve
import com.babylon.wallet.android.data.dapp.model.DerivePublicKeyRequest
import com.babylon.wallet.android.data.dapp.model.GetDeviceInfoRequest
import com.babylon.wallet.android.data.dapp.model.ImportOlympiaDeviceRequest
import com.babylon.wallet.android.data.dapp.model.LedgerInteractionRequest
import com.babylon.wallet.android.data.dapp.model.SignTransactionRequest
import com.babylon.wallet.android.data.dapp.model.peerdroidRequestJson
import com.babylon.wallet.android.domain.model.MessageFromDataChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.encodeToString
import rdx.works.peerdroid.helpers.Result.Error
import rdx.works.peerdroid.helpers.Result.Success
import javax.inject.Inject

interface LedgerMessenger {

    fun sendDeviceInfoRequest(interactionId: String): Flow<MessageFromDataChannel.LedgerResponse.GetDeviceInfoResponse>
    fun sendImportOlympiaDeviceRequest(
        interactionId: String,
        derivationPaths: List<String>
    ): Flow<MessageFromDataChannel.LedgerResponse.ImportOlympiaDeviceResponse>

    fun sendDeriveCurve25519PublicKeyRequest(
        interactionId: String,
        derivationPath: String,
        ledgerDevice: DerivePublicKeyRequest.LedgerDevice
    ): Flow<MessageFromDataChannel.LedgerResponse.DerivePublicKeyResponse>

    fun signTransactionRequest(
        interactionId: String,
        signersDerivationPathToCurve: List<Pair<String, Curve>>,
        compiledTransactionIntent: String,
        ledgerDevice: DerivePublicKeyRequest.LedgerDevice,
        displayHashOnLedgerDisplay: Boolean = true
    ): Flow<MessageFromDataChannel.LedgerResponse.SignTransactionResponse>
}

class LedgerMessengerImpl @Inject constructor(
    private val peerdroidClient: PeerdroidClient,
) : LedgerMessenger {

    override fun sendDeviceInfoRequest(interactionId: String): Flow<MessageFromDataChannel.LedgerResponse.GetDeviceInfoResponse> {
        val ledgerRequest: LedgerInteractionRequest = GetDeviceInfoRequest(interactionId)
        return flow {
            when (peerdroidClient.sendMessage(peerdroidRequestJson.encodeToString(ledgerRequest))) {
                is Success -> {
                    peerdroidClient.listenForLedgerResponses().filter {
                        it.id == interactionId
                    }.filterIsInstance<MessageFromDataChannel.LedgerResponse.GetDeviceInfoResponse>().collect {
                        emit(it)
                    }
                }
                is Error -> {}
            }
        }
    }

    override fun sendImportOlympiaDeviceRequest(
        interactionId: String,
        derivationPaths: List<String>
    ): Flow<MessageFromDataChannel.LedgerResponse.ImportOlympiaDeviceResponse> {
        val request: LedgerInteractionRequest = ImportOlympiaDeviceRequest(interactionId, derivationPaths)
        return flow {
            when (peerdroidClient.sendMessage(peerdroidRequestJson.encodeToString(request))) {
                is Success -> {
                    peerdroidClient.listenForLedgerResponses().filter {
                        it.id == interactionId
                    }.filterIsInstance<MessageFromDataChannel.LedgerResponse.ImportOlympiaDeviceResponse>().collect {
                        emit(it)
                    }
                }
                is Error -> {}
            }
        }
    }

    override fun sendDeriveCurve25519PublicKeyRequest(
        interactionId: String,
        derivationPath: String,
        ledgerDevice: DerivePublicKeyRequest.LedgerDevice
    ): Flow<MessageFromDataChannel.LedgerResponse.DerivePublicKeyResponse> {
        val ledgerRequest: LedgerInteractionRequest = DerivePublicKeyRequest(
            interactionId = interactionId,
            keyParameters = DerivePublicKeyRequest.KeyParameters(Curve.Curve25519, derivationPath),
            ledgerDevice = ledgerDevice
        )
        return flow {
            when (peerdroidClient.sendMessage(peerdroidRequestJson.encodeToString(ledgerRequest))) {
                is Success -> {
                    peerdroidClient.listenForLedgerResponses().filter {
                        it.id == interactionId
                    }.filterIsInstance<MessageFromDataChannel.LedgerResponse.DerivePublicKeyResponse>().collect {
                        emit(it)
                    }
                }
                is Error -> {}
            }
        }
    }

    override fun signTransactionRequest(
        interactionId: String,
        signersDerivationPathToCurve: List<Pair<String, Curve>>,
        compiledTransactionIntent: String,
        ledgerDevice: DerivePublicKeyRequest.LedgerDevice,
        displayHashOnLedgerDisplay: Boolean
    ): Flow<MessageFromDataChannel.LedgerResponse.SignTransactionResponse> {
        val ledgerRequest: LedgerInteractionRequest = SignTransactionRequest(
            interactionId = interactionId,
            signers = signersDerivationPathToCurve.map { DerivePublicKeyRequest.KeyParameters(it.second, it.first) },
            ledgerDevice = ledgerDevice,
            displayHash = displayHashOnLedgerDisplay,
            compiledTransactionIntent = compiledTransactionIntent,
            mode = SignTransactionRequest.Mode.Summary
        )
        return flow {
            when (peerdroidClient.sendMessage(peerdroidRequestJson.encodeToString(ledgerRequest))) {
                is Success -> {
                    peerdroidClient.listenForLedgerResponses().filter {
                        it.id == interactionId
                    }.filterIsInstance<MessageFromDataChannel.LedgerResponse.SignTransactionResponse>().collect {
                        emit(it)
                    }
                }
                is Error -> {}
            }
        }
    }
}
