package com.babylon.wallet.android.domain.transaction

import com.babylon.wallet.android.domain.model.IncomingRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

class IncomingRequestHolder @Inject constructor() {

    private val _incomingRequests = MutableSharedFlow<IncomingRequest>(replay = 1)

    suspend fun emit(request: IncomingRequest) {
        _incomingRequests.emit(request)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun receive(scope: CoroutineScope, action: suspend (IncomingRequest) -> Unit) {
        _incomingRequests.onEach {
            _incomingRequests.resetReplayCache()
            action(it)
        }.launchIn(scope)
    }
}
