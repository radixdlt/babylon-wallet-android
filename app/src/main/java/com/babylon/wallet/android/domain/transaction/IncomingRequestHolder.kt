package com.babylon.wallet.android.domain.transaction

import com.babylon.wallet.android.domain.model.MessageFromDataChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import timber.log.Timber
import javax.inject.Inject

class IncomingRequestHolder @Inject constructor() {

    private val _incomingRequests = MutableSharedFlow<Map<String, MessageFromDataChannel.IncomingRequest>>(replay = 1)

    suspend fun emit(request: MessageFromDataChannel.IncomingRequest) {
        if (request.id != null) {
            val updatedMap = _incomingRequests.replayCache.firstOrNull().orEmpty().toMutableMap().apply {
                this[request.id] = request
            }
            _incomingRequests.emit(updatedMap)
        }
    }

    private suspend fun removeRequest(request: MessageFromDataChannel.IncomingRequest) {
        if (request.id != null) {
            Timber.d("Removing request ${request.id}")
            val updatedMap = _incomingRequests.replayCache.first().toMutableMap().apply {
                this.remove(request.id)
            }
            _incomingRequests.emit(updatedMap)
        }
    }

    suspend fun receive(
        scope: CoroutineScope,
        requestId: String,
        action: suspend (MessageFromDataChannel.IncomingRequest) -> Unit,
    ) {
        _incomingRequests.filter { it.contains(requestId) }.onEach {
            Timber.d("Received request $requestId")
            it[requestId]?.let { request ->
                removeRequest(request)
                action(request)
            }
        }.launchIn(scope)
    }
}
