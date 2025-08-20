package com.babylon.wallet.android.presentation.nfc.common

import com.radixdlt.sargon.BagOfBytes
import com.radixdlt.sargon.CommonException
import com.radixdlt.sargon.NfcTagDriverPurpose
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import javax.inject.Inject
import javax.inject.Singleton

interface NfcSessionIOHandler {

    val purpose: NfcTagDriverPurpose?

    val transceiveRequests: Flow<BagOfBytes>
    val events: Flow<NfcSessionProxy.Event>

    suspend fun onSessionReady()

    suspend fun onTransceiveResult(result: Result<BagOfBytes>)

    suspend fun onSessionClosed()
}

interface NfcSessionProxy {

    suspend fun awaitSessionReady(purpose: NfcTagDriverPurpose)

    suspend fun transceive(command: BagOfBytes): BagOfBytes

    suspend fun transceiveChain(commands: List<BagOfBytes>): BagOfBytes

    suspend fun sendEvent(event: Event)

    sealed interface Event {

        data class SetMessage(
            val message: String
        ) : Event

        data class EndSession(
            val withFailure: Throwable?
        ) : Event
    }
}

/**
 * Session-scoped bridge between Sargon NFC driver and the UI sheet.
 */
@Singleton
class NfcSessionProxyImpl @Inject constructor() : NfcSessionProxy, NfcSessionIOHandler {

    private val sessionReadyChannel = Channel<Result<Unit>>()

    private val transceiveRequestsChannel = Channel<BagOfBytes>()
    private val transceiveResultsChannel = Channel<Result<BagOfBytes>>()

    private val eventsChannel = Channel<NfcSessionProxy.Event>()
    private var _purpose: NfcTagDriverPurpose? = null

    override val purpose: NfcTagDriverPurpose?
        get() = _purpose
    override val transceiveRequests: Flow<BagOfBytes> = transceiveRequestsChannel.receiveAsFlow()
    override val events: Flow<NfcSessionProxy.Event> = eventsChannel.receiveAsFlow()

    override suspend fun awaitSessionReady(purpose: NfcTagDriverPurpose) {
        _purpose = purpose
        sessionReadyChannel.receive().getOrThrow()
    }

    override suspend fun transceive(command: BagOfBytes): BagOfBytes {
        transceiveRequestsChannel.send(command)
        return transceiveResultsChannel.receive().getOrThrow()
    }

    override suspend fun transceiveChain(commands: List<BagOfBytes>): BagOfBytes {
        var last: BagOfBytes? = null
        for (cmd in commands) {
            last = transceive(cmd)
        }
        return checkNotNull(last)
    }

    override suspend fun sendEvent(event: NfcSessionProxy.Event) {
        eventsChannel.send(event)
    }

    override suspend fun onSessionReady() {
        sessionReadyChannel.send(Result.success(Unit))
    }

    override suspend fun onTransceiveResult(result: Result<BagOfBytes>) {
        transceiveResultsChannel.send(result)
    }

    override suspend fun onSessionClosed() {
        sessionReadyChannel.trySend(Result.failure(CommonException.HostInteractionAborted()))
        transceiveResultsChannel.trySend(Result.failure(CommonException.HostInteractionAborted()))
    }
}
