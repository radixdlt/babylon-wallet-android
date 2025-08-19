package com.babylon.wallet.android.presentation.sargonInteractors

import com.babylon.wallet.android.presentation.nfc.common.NfcSessionProxy
import com.babylon.wallet.android.utils.AppEvent
import com.babylon.wallet.android.utils.AppEventBus
import com.radixdlt.sargon.BagOfBytes
import com.radixdlt.sargon.CommonException
import com.radixdlt.sargon.NfcTagDriver
import com.radixdlt.sargon.NfcTagDriverPurpose
import javax.inject.Inject

class NFCTagInteractor @Inject constructor(
    private val appEventBus: AppEventBus,
    private val sessionProxy: NfcSessionProxy
) : NfcTagDriver {

    override suspend fun startSession(purpose: NfcTagDriverPurpose) {
        appEventBus.sendEvent(AppEvent.Nfc.StartSession(purpose))
        // Wait until UI reports tag discovered and ready
        sessionProxy.awaitSessionReady()
    }

    override suspend fun endSession(withFailure: CommonException?) {
        sessionProxy.sendEvent(NfcSessionProxy.Event.EndSession(withFailure))
    }

    override suspend fun sendReceive(command: BagOfBytes): BagOfBytes {
        return sessionProxy.transceive(command)
    }

    override suspend fun sendReceiveCommandChain(commands: List<BagOfBytes>): BagOfBytes {
        return sessionProxy.transceiveChain(commands)
    }

    override suspend fun setMessage(message: String) {
        sessionProxy.sendEvent(NfcSessionProxy.Event.SetMessage(message))
    }
}
