package com.babylon.wallet.android.presentation.sargonInteractors

import com.babylon.wallet.android.utils.AppEvent
import com.babylon.wallet.android.utils.AppEventBus
import com.radixdlt.sargon.BagOfBytes
import com.radixdlt.sargon.CommonException
import com.radixdlt.sargon.NfcTagDriver
import com.radixdlt.sargon.NfcTagDriverPurpose
import com.radixdlt.sargon.extensions.toBagOfBytes
import javax.inject.Inject

class NFCTagInteractor @Inject constructor(
    private val appEventBus: AppEventBus
) : NfcTagDriver {
    override suspend fun startSession(purpose: NfcTagDriverPurpose) {
        appEventBus.sendEvent(AppEvent.Nfc.StartSession(purpose))
    }

    override suspend fun endSession(withFailure: CommonException?) {
        appEventBus.sendEvent(AppEvent.Nfc.EndSession(withFailure))
    }

    override suspend fun sendReceive(command: BagOfBytes): BagOfBytes {
        // Scaffold only: echo back success SW
        return byteArrayOf(0x90.toByte(), 0x00.toByte()).toBagOfBytes()
    }

    override suspend fun sendReceiveCommandChain(commands: List<BagOfBytes>): BagOfBytes {
        // Scaffold only: return success SW
        return byteArrayOf(0x90.toByte(), 0x00.toByte()).toBagOfBytes()
    }

    override suspend fun setMessage(message: String) {
        appEventBus.sendEvent(AppEvent.Nfc.SetMessage(message))
    }
}