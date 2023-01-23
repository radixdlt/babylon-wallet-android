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
            val id = incomingRequest.id
            if (id != null) {
                if (listOfIncomingRequests.isEmpty()) {
                    _currentRequestToHandle.emit(incomingRequest)
                }
                listOfIncomingRequests.putIfAbsent(id, incomingRequest)
            }
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

    fun getAccountsRequest(requestId: String): IncomingRequest.AccountsRequest {
        require(listOfIncomingRequests.containsKey(requestId)) {
            "IncomingRequestRepository does not contain this request"
        }

        return (listOfIncomingRequests[requestId] as IncomingRequest.AccountsRequest)
    }

    fun getTransactionWriteRequest(requestId: String): IncomingRequest.TransactionItem {
        require(listOfIncomingRequests.containsKey(requestId)) {
            "IncomingRequestRepository does not contain this request"
        }

        return (listOfIncomingRequests[requestId] as IncomingRequest.TransactionItem)
    }

    fun getOneTimePersonaRequest(requestId: String): IncomingRequest.PersonaRequest {
        require(listOfIncomingRequests.containsKey(requestId)) {
            "IncomingRequestRepository does not contain this request"
        }

        return (listOfIncomingRequests[requestId] as IncomingRequest.PersonaRequest)
    }

    fun getAmountOfRequests() = listOfIncomingRequests.size
}
