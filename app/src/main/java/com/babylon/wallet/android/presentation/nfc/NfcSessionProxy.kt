package com.babylon.wallet.android.presentation.nfc

import com.radixdlt.sargon.BagOfBytes
import com.radixdlt.sargon.CommonException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Session-scoped bridge between Sargon NFC driver and the UI sheet.
 */
@Singleton
class NfcSessionProxy @Inject constructor() {

    data class TransceiveRequest(
        val command: BagOfBytes,
        val response: CompletableDeferred<BagOfBytes>
    )

    private val requestsChannel = Channel<TransceiveRequest>(capacity = Channel.RENDEZVOUS)
    private val _isActive = MutableStateFlow(false)
    val isActive: Flow<Boolean> = _isActive.asStateFlow()

    val transceiveRequests: Flow<TransceiveRequest> = requestsChannel.receiveAsFlow()

    fun onSessionStarted() {
        isActiveValue(true)
        synchronized(this) {
            if (sessionReady == null || sessionReady?.isCompleted == true) {
                sessionReady = CompletableDeferred()
            }
        }
    }

    fun onSessionEnded(withFailure: CommonException?) {
        isActiveValue(false)
        if (withFailure != null) {
            // Cancel any pending requests with the provided failure
            // Note: requests are only created via sendReceive; we cannot iterate the channel buffer here.
            // Pending deferreds will be cancelled by their callers timeout logic if any.
        }
        synchronized(this) {
            sessionReady?.cancel()
            sessionReady = null
        }
    }

    suspend fun awaitReady() {
        val deferred = synchronized(this) { sessionReady }
        deferred?.await()
    }

    fun markReady() {
        synchronized(this) {
            sessionReady?.complete(Unit)
        }
    }

    @Suppress("SwallowedException")
    suspend fun transceive(command: BagOfBytes): BagOfBytes {
        val deferred = CompletableDeferred<BagOfBytes>()
        requestsChannel.send(TransceiveRequest(command = command, response = deferred))
        return try {
            deferred.await()
        } catch (c: Exception) {
            throw CommonException.HostInteractionAborted()
        }
    }

    suspend fun transceiveChain(commands: List<BagOfBytes>): BagOfBytes {
        var last: BagOfBytes? = null
        for (cmd in commands) {
            last = transceive(cmd)
        }
        @Suppress("UNCHECKED_CAST")
        return last as BagOfBytes
    }

    private fun isActiveValue(value: Boolean) {
        // Avoid emitting duplicate values
        if ((_isActive.value) != value) {
            _isActive.value = value
        }
    }

    @Volatile
    private var sessionReady: CompletableDeferred<Unit>? = null
}
