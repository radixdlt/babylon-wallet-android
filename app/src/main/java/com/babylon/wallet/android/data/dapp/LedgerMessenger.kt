package com.babylon.wallet.android.data.dapp

import com.babylon.wallet.android.data.dapp.model.DerivePublicKeyRequest
import com.babylon.wallet.android.data.dapp.model.GetDeviceInfoRequest
import com.babylon.wallet.android.data.dapp.model.ImportOlympiaDeviceRequest
import com.babylon.wallet.android.data.dapp.model.LedgerInteractionRequest
import com.babylon.wallet.android.data.dapp.model.peerdroidRequestJson
import com.babylon.wallet.android.domain.common.Result
import kotlinx.serialization.encodeToString
import javax.inject.Inject

interface LedgerMessenger {

    suspend fun sendDeviceInfoRequest(interactionId: String): Result<String>
    suspend fun sendImportOlympiaDeviceRequest(interactionId: String, derivationPaths: List<String>): Result<String>
    suspend fun sendDerivePublicKeyRequest(
        interactionId: String,
        keyParameters: DerivePublicKeyRequest.KeyParameters,
        ledgerDevice: DerivePublicKeyRequest.LedgerDevice
    ): Result<String>
}

class LedgerMessengerImpl @Inject constructor(
    private val peerdroidClient: PeerdroidClient,
) : LedgerMessenger {

    override suspend fun sendDeviceInfoRequest(interactionId: String): Result<String> {
        val ledgerRequest: LedgerInteractionRequest = GetDeviceInfoRequest(interactionId)
        return when (peerdroidClient.sendMessage(peerdroidRequestJson.encodeToString(ledgerRequest))) {
            is rdx.works.peerdroid.helpers.Result.Success -> Result.Success(interactionId)
            is rdx.works.peerdroid.helpers.Result.Error -> Result.Error()
        }
    }

    override suspend fun sendImportOlympiaDeviceRequest(interactionId: String, derivationPaths: List<String>): Result<String> {
        val request: LedgerInteractionRequest = ImportOlympiaDeviceRequest(interactionId, derivationPaths)
        return when (peerdroidClient.sendMessage(peerdroidRequestJson.encodeToString(request))) {
            is rdx.works.peerdroid.helpers.Result.Success -> Result.Success(interactionId)
            is rdx.works.peerdroid.helpers.Result.Error -> Result.Error()
        }
    }

    override suspend fun sendDerivePublicKeyRequest(
        interactionId: String,
        keyParameters: DerivePublicKeyRequest.KeyParameters,
        ledgerDevice: DerivePublicKeyRequest.LedgerDevice
    ): Result<String> {
        val ledgerRequest: LedgerInteractionRequest = DerivePublicKeyRequest(interactionId, keyParameters, ledgerDevice)
        return when (peerdroidClient.sendMessage(peerdroidRequestJson.encodeToString(ledgerRequest))) {
            is rdx.works.peerdroid.helpers.Result.Success -> Result.Success(interactionId)
            is rdx.works.peerdroid.helpers.Result.Error -> Result.Error()
        }
    }
}
