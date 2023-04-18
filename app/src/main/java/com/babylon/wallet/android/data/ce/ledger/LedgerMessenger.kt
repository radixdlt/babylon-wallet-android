@file:Suppress("LongParameterList")

package com.babylon.wallet.android.data.ce.ledger

import com.babylon.wallet.android.data.ce.DerivePublicKeyRequest
import com.babylon.wallet.android.data.ce.GetDeviceInfoRequest
import com.babylon.wallet.android.data.ce.ImportOlympiaDeviceRequest
import com.babylon.wallet.android.data.ce.LedgerInteraction
import com.babylon.wallet.android.data.ce.PeerdroidClient
import com.babylon.wallet.android.data.ce.peerdroidRequestJson
import com.babylon.wallet.android.domain.common.Result
import kotlinx.serialization.encodeToString
import rdx.works.core.UUIDGenerator
import javax.inject.Inject

interface LedgerMessenger {

    suspend fun sendDeviceInfoRequest(): Result<String>
    suspend fun sendImportOlympiaDeviceRequest(derivationPaths: List<String>): Result<String>
    suspend fun sendDerivePublicKeyRequest(
        keyParameters: DerivePublicKeyRequest.KeyParameters,
        ledgerDevice: DerivePublicKeyRequest.LedgerDevice
    ): Result<String>
}

class LedgerMessengerImpl @Inject constructor(
    private val peerdroidClient: PeerdroidClient,
) : LedgerMessenger {

    override suspend fun sendDeviceInfoRequest(): Result<String> {
        val interactionId = UUIDGenerator.uuid().toString()
        val ledgerRequest: LedgerInteraction = GetDeviceInfoRequest(interactionId)
        return when (peerdroidClient.sendMessage(interactionId, peerdroidRequestJson.encodeToString(ledgerRequest))) {
            is rdx.works.peerdroid.helpers.Result.Success -> Result.Success(interactionId)
            is rdx.works.peerdroid.helpers.Result.Error -> Result.Error()
        }
    }

    override suspend fun sendImportOlympiaDeviceRequest(derivationPaths: List<String>): Result<String> {
        val interactionId = UUIDGenerator.uuid().toString()
        val request: LedgerInteraction = ImportOlympiaDeviceRequest(interactionId, derivationPaths)
        return when (peerdroidClient.sendMessage(interactionId, peerdroidRequestJson.encodeToString(request))) {
            is rdx.works.peerdroid.helpers.Result.Success -> Result.Success(interactionId)
            is rdx.works.peerdroid.helpers.Result.Error -> Result.Error()
        }
    }

    override suspend fun sendDerivePublicKeyRequest(
        keyParameters: DerivePublicKeyRequest.KeyParameters,
        ledgerDevice: DerivePublicKeyRequest.LedgerDevice
    ): Result<String> {
        val interactionId = UUIDGenerator.uuid().toString()
        val ledgerRequest: LedgerInteraction = DerivePublicKeyRequest(interactionId, keyParameters, ledgerDevice)
        return when (peerdroidClient.sendMessage(interactionId, peerdroidRequestJson.encodeToString(ledgerRequest))) {
            is rdx.works.peerdroid.helpers.Result.Success -> Result.Success(interactionId)
            is rdx.works.peerdroid.helpers.Result.Error -> Result.Error()
        }
    }

}
