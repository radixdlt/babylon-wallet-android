package com.babylon.wallet.android.presentation.sargonInteractors

import com.babylon.wallet.android.utils.AppEvent
import com.babylon.wallet.android.utils.AppEventBus
import com.radixdlt.sargon.BagOfBytes
import com.radixdlt.sargon.CommonException
import com.radixdlt.sargon.NfcTagDriver
import com.radixdlt.sargon.NfcTagDriverPurpose
import com.babylon.wallet.android.presentation.nfc.NfcSessionProxy
import javax.inject.Inject

class NFCTagInteractor @Inject constructor(
    private val appEventBus: AppEventBus,
    private val sessionProxy: NfcSessionProxy
) : NfcTagDriver {
    override suspend fun startSession(purpose: NfcTagDriverPurpose) {
        appEventBus.sendEvent(AppEvent.Nfc.StartSession(purpose))
        sessionProxy.onSessionStarted()
    }

    override suspend fun endSession(withFailure: CommonException?) {
        appEventBus.sendEvent(AppEvent.Nfc.EndSession(withFailure))
        sessionProxy.onSessionEnded(withFailure)
    }

    override suspend fun sendReceive(command: BagOfBytes): BagOfBytes {
        return sessionProxy.transceive(command)
    }

    override suspend fun sendReceiveCommandChain(commands: List<BagOfBytes>): BagOfBytes {
        return sessionProxy.transceiveChain(commands)
    }

    override suspend fun setMessage(message: String) {
        appEventBus.sendEvent(AppEvent.Nfc.SetMessage(message))
    }
}