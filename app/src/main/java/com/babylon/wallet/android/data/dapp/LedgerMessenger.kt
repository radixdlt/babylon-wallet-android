package com.babylon.wallet.android.data.dapp

import com.babylon.wallet.android.data.dapp.model.Curve
import com.babylon.wallet.android.data.dapp.model.DerivePublicKeyRequest
import com.babylon.wallet.android.data.dapp.model.GetDeviceInfoRequest
import com.babylon.wallet.android.data.dapp.model.ImportOlympiaDeviceRequest
import com.babylon.wallet.android.data.dapp.model.LedgerInteractionRequest
import com.babylon.wallet.android.data.dapp.model.SignTransactionRequest
import com.babylon.wallet.android.data.dapp.model.peerdroidRequestJson
import com.babylon.wallet.android.domain.model.MessageFromDataChannel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.encodeToString
import rdx.works.peerdroid.helpers.Result.Error
import rdx.works.peerdroid.helpers.Result.Success
import rdx.works.profile.data.model.factorsources.Slip10Curve
import javax.inject.Inject

interface LedgerMessenger {

    suspend fun sendDeviceInfoRequest(interactionId: String): Result<MessageFromDataChannel.LedgerResponse.GetDeviceInfoResponse>
    suspend fun sendImportOlympiaDeviceRequest(
        interactionId: String,
        derivationPaths: List<String>
    ): Result<MessageFromDataChannel.LedgerResponse.ImportOlympiaDeviceResponse>

    suspend fun sendDeriveCurve25519PublicKeyRequest(
        interactionId: String,
        derivationPath: String,
        ledgerDevice: DerivePublicKeyRequest.LedgerDevice
    ): Result<MessageFromDataChannel.LedgerResponse.DerivePublicKeyResponse>

    suspend fun signTransactionRequest(
        interactionId: String,
        signersDerivationPathToCurve: List<Pair<String, Slip10Curve>>,
        compiledTransactionIntent: String,
        ledgerDevice: DerivePublicKeyRequest.LedgerDevice,
        displayHashOnLedgerDisplay: Boolean = true
    ): Result<MessageFromDataChannel.LedgerResponse.SignTransactionResponse>
}

class LedgerMessengerImpl @Inject constructor(
    private val peerdroidClient: PeerdroidClient,
) : LedgerMessenger {

    override suspend fun sendDeviceInfoRequest(interactionId: String): Result<MessageFromDataChannel.LedgerResponse.GetDeviceInfoResponse> {
        val ledgerRequest: LedgerInteractionRequest = GetDeviceInfoRequest(interactionId)
        return flow<Result<MessageFromDataChannel.LedgerResponse.GetDeviceInfoResponse>> {
            when (peerdroidClient.sendMessage(peerdroidRequestJson.encodeToString(ledgerRequest))) {
                is Success -> {
                    peerdroidClient.listenForLedgerResponses().filter {
                        it.id == interactionId
                    }.catch {
                        emit(Result.failure(it))
                    }.filterIsInstance<MessageFromDataChannel.LedgerResponse.GetDeviceInfoResponse>().collect {
                        emit(Result.success(it))
                    }
                }
                is Error -> {
                    emit(Result.failure(Exception("Failed to get ledger device info")))
                }
            }
        }.first()
    }

    override suspend fun sendImportOlympiaDeviceRequest(
        interactionId: String,
        derivationPaths: List<String>
    ): Result<MessageFromDataChannel.LedgerResponse.ImportOlympiaDeviceResponse> {
        val request: LedgerInteractionRequest = ImportOlympiaDeviceRequest(interactionId, derivationPaths)
        return flow<Result<MessageFromDataChannel.LedgerResponse.ImportOlympiaDeviceResponse>> {
            when (peerdroidClient.sendMessage(peerdroidRequestJson.encodeToString(request))) {
                is Success -> {
                    peerdroidClient.listenForLedgerResponses().filter {
                        it.id == interactionId
                    }.catch {
                        emit(Result.failure(exception = it))
                    }.filterIsInstance<MessageFromDataChannel.LedgerResponse.ImportOlympiaDeviceResponse>().collect {
                        emit(Result.success(it))
                    }
                }
                is Error -> {
                    emit(Result.failure(Exception("Failed to receive Olympia Import response")))
                }
            }
        }.first()
    }

    override suspend fun sendDeriveCurve25519PublicKeyRequest(
        interactionId: String,
        derivationPath: String,
        ledgerDevice: DerivePublicKeyRequest.LedgerDevice
    ): Result<MessageFromDataChannel.LedgerResponse.DerivePublicKeyResponse> {
        val ledgerRequest: LedgerInteractionRequest = DerivePublicKeyRequest(
            interactionId = interactionId,
            keysParameters = listOf(DerivePublicKeyRequest.KeyParameters(Curve.Curve25519, derivationPath)),
            ledgerDevice = ledgerDevice
        )
        return flow<Result<MessageFromDataChannel.LedgerResponse.DerivePublicKeyResponse>> {
            when (peerdroidClient.sendMessage(peerdroidRequestJson.encodeToString(ledgerRequest))) {
                is Success -> {
                    peerdroidClient.listenForLedgerResponses().filter {
                        it.id == interactionId
                    }.catch {
                        emit(Result.failure(it))
                    }.filterIsInstance<MessageFromDataChannel.LedgerResponse.DerivePublicKeyResponse>().collect {
                        emit(Result.success(it))
                    }
                }
                is Error -> {
                    emit(Result.failure(Exception("Failed to derive public key with Ledger")))
                }
            }
        }.first()
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
            signers = signersDerivationPathToCurve.map { DerivePublicKeyRequest.KeyParameters(Curve.from(it.second), it.first) },
            ledgerDevice = ledgerDevice,
            displayHash = displayHashOnLedgerDisplay,
            compiledTransactionIntent = compiledTransactionIntent,
            mode = SignTransactionRequest.Mode.Verbose
        )
        return flow<Result<MessageFromDataChannel.LedgerResponse.SignTransactionResponse>> {
            when (peerdroidClient.sendMessage(peerdroidRequestJson.encodeToString(ledgerRequest))) {
                is Success -> {
                    peerdroidClient.listenForLedgerResponses().filter {
                        it.id == interactionId
                    }.catch { e ->
                        emit(Result.failure(e))
                    }.filterIsInstance<MessageFromDataChannel.LedgerResponse.SignTransactionResponse>().collect {
                        emit(Result.success(it))
                    }
                }
                is Error -> {
                    emit(Result.failure(Exception("Failed to sign transaction with Ledger")))
                }
            }
        }.first()
    }
}
