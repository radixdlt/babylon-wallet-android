package com.babylon.wallet.android.data.dapp

import com.babylon.wallet.android.domain.model.MessageFromDataChannel.IncomingRequest
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IncomingRequestRepository @Inject constructor() {

    private val listOfIncomingRequests = mutableMapOf<String, IncomingRequest>()

    private val _currentRequestToHandle = MutableSharedFlow<IncomingRequest>()
    val currentRequestToHandle = _currentRequestToHandle.asSharedFlow()

    private val mutex = Mutex()

    suspend fun add(incomingRequest: IncomingRequest) {
        mutex.withLock {
            if (listOfIncomingRequests.isEmpty()) {
                _currentRequestToHandle.emit(incomingRequest)
            }
            listOfIncomingRequests.putIfAbsent(incomingRequest.id, incomingRequest)
        }
    }

    suspend fun requestHandled(requestId: String) {
        mutex.withLock {
            listOfIncomingRequests.remove(requestId)
            listOfIncomingRequests.values.firstOrNull()?.let { nextRequest ->
                _currentRequestToHandle.emit(nextRequest)
            }
        }
    }

    fun getUnauthorizedRequest(requestId: String): IncomingRequest.UnauthorizedRequest {
        require(listOfIncomingRequests.containsKey(requestId)) {
            "IncomingRequestRepository does not contain this request"
        }

        return (listOfIncomingRequests[requestId] as IncomingRequest.UnauthorizedRequest)
    }

    fun getTransactionWriteRequest(requestId: String): IncomingRequest.TransactionRequest {
        require(listOfIncomingRequests.containsKey(requestId)) {
            "IncomingRequestRepository does not contain this request"
        }

        return (listOfIncomingRequests[requestId] as IncomingRequest.TransactionRequest)
    }

    fun getAuthorizedRequest(requestId: String): IncomingRequest.AuthorizedRequest {
        require(listOfIncomingRequests.containsKey(requestId)) {
            "IncomingRequestRepository does not contain this request"
        }

        return (listOfIncomingRequests[requestId] as IncomingRequest.AuthorizedRequest)
    }

    fun getAmountOfRequests() = listOfIncomingRequests.size
}
