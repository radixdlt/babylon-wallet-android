@file:Suppress("SuspendFunWithFlowReturnType")

package com.babylon.wallet.android.data.dapp

import com.babylon.wallet.android.data.dapp.model.Curve
import com.babylon.wallet.android.data.dapp.model.DerivePublicKeyRequest
import com.babylon.wallet.android.data.dapp.model.GetDeviceInfoRequest
import com.babylon.wallet.android.data.dapp.model.ImportOlympiaDeviceRequest
import com.babylon.wallet.android.data.dapp.model.LedgerInteractionRequest
import com.babylon.wallet.android.data.dapp.model.peerdroidRequestJson
import com.babylon.wallet.android.domain.model.MessageFromDataChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.serialization.encodeToString
import javax.inject.Inject

interface LedgerMessenger {

    suspend fun sendDeviceInfoRequest(interactionId: String): Flow<MessageFromDataChannel.LedgerResponse.GetDeviceInfoResponse>
    suspend fun sendImportOlympiaDeviceRequest(
        interactionId: String,
        derivationPaths: List<String>
    ): Flow<MessageFromDataChannel.LedgerResponse.ImportOlympiaDeviceResponse>

    suspend fun sendDeriveCurve25519PublicKeyRequest(
        interactionId: String,
        derivationPath: String,
        ledgerDevice: DerivePublicKeyRequest.LedgerDevice
    ): Flow<MessageFromDataChannel.LedgerResponse.DerivePublicKeyResponse>
}

class LedgerMessengerImpl @Inject constructor(
    private val peerdroidClient: PeerdroidClient,
) : LedgerMessenger {

    override suspend fun sendDeviceInfoRequest(interactionId: String): Flow<MessageFromDataChannel.LedgerResponse.GetDeviceInfoResponse> {
        val ledgerRequest: LedgerInteractionRequest = GetDeviceInfoRequest(interactionId)
        return when (peerdroidClient.sendMessage(peerdroidRequestJson.encodeToString(ledgerRequest))) {
            is rdx.works.peerdroid.helpers.Result.Success -> {
                peerdroidClient.listenForLedgerResponses().filter { it.id == interactionId }.filterIsInstance()
            }
            is rdx.works.peerdroid.helpers.Result.Error -> emptyFlow()
        }
    }

    override suspend fun sendImportOlympiaDeviceRequest(
        interactionId: String,
        derivationPaths: List<String>
    ): Flow<MessageFromDataChannel.LedgerResponse.ImportOlympiaDeviceResponse> {
        val request: LedgerInteractionRequest = ImportOlympiaDeviceRequest(interactionId, derivationPaths)
        return when (peerdroidClient.sendMessage(peerdroidRequestJson.encodeToString(request))) {
            is rdx.works.peerdroid.helpers.Result.Success -> {
                peerdroidClient.listenForLedgerResponses().filter { it.id == interactionId }.filterIsInstance()
            }
            is rdx.works.peerdroid.helpers.Result.Error -> emptyFlow()
        }
    }

    override suspend fun sendDeriveCurve25519PublicKeyRequest(
        interactionId: String,
        derivationPath: String,
        ledgerDevice: DerivePublicKeyRequest.LedgerDevice
    ): Flow<MessageFromDataChannel.LedgerResponse.DerivePublicKeyResponse> {
        val ledgerRequest: LedgerInteractionRequest = DerivePublicKeyRequest(
            interactionId = interactionId,
            keyParameters = DerivePublicKeyRequest.KeyParameters(Curve.Curve25519, derivationPath),
            ledgerDevice = ledgerDevice
        )
        return when (peerdroidClient.sendMessage(peerdroidRequestJson.encodeToString(ledgerRequest))) {
            is rdx.works.peerdroid.helpers.Result.Success -> {
                peerdroidClient.listenForLedgerResponses().filter { it.id == interactionId }
                    .filterIsInstance()
            }
            is rdx.works.peerdroid.helpers.Result.Error -> emptyFlow()
        }
    }
}
