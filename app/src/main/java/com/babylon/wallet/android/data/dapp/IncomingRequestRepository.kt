package com.babylon.wallet.android.data.dapp

import com.babylon.wallet.android.domain.model.MessageFromDataChannel.IncomingRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import javax.inject.Inject

interface IncomingRequestRepository {

    val currentRequestToHandle: Flow<IncomingRequest>

    suspend fun add(incomingRequest: IncomingRequest)

    suspend fun requestHandled(requestId: String)

    fun getUnauthorizedRequest(requestId: String): IncomingRequest.UnauthorizedRequest

    fun getTransactionWriteRequest(requestId: String): IncomingRequest.TransactionRequest

    fun getAuthorizedRequest(requestId: String): IncomingRequest.AuthorizedRequest

    fun removeAll()

    fun getAmountOfRequests(): Int
}

class IncomingRequestRepositoryImpl @Inject constructor() : IncomingRequestRepository {

    private val listOfIncomingRequests = mutableMapOf<String, IncomingRequest>()

    private val _currentRequestToHandle = MutableSharedFlow<IncomingRequest>()
    override val currentRequestToHandle = _currentRequestToHandle.asSharedFlow()

    private val mutex = Mutex()

    override suspend fun add(incomingRequest: IncomingRequest) {
        mutex.withLock {
            if (listOfIncomingRequests.isEmpty()) {
                _currentRequestToHandle.emit(incomingRequest)
            }
            listOfIncomingRequests.putIfAbsent(incomingRequest.id, incomingRequest)
            Timber.d("🗂 new incoming request with id ${incomingRequest.id} added in list, so size now is ${listOfIncomingRequests.size}")
        }
    }

    override suspend fun requestHandled(requestId: String) {
        mutex.withLock {
            listOfIncomingRequests.remove(requestId)
            listOfIncomingRequests.values.firstOrNull()?.let { nextRequest ->
                _currentRequestToHandle.emit(nextRequest)
            }
            Timber.d("🗂 request $requestId handled so size of list is now: ${listOfIncomingRequests.size}")
        }
    }

    override fun getUnauthorizedRequest(requestId: String): IncomingRequest.UnauthorizedRequest {
        require(listOfIncomingRequests.containsKey(requestId)) {
            "IncomingRequestRepository does not contain this request"
        }

        return (listOfIncomingRequests[requestId] as IncomingRequest.UnauthorizedRequest)
    }

    override fun getTransactionWriteRequest(requestId: String): IncomingRequest.TransactionRequest {
        require(listOfIncomingRequests.containsKey(requestId)) {
            "IncomingRequestRepository does not contain this request"
        }

        return (listOfIncomingRequests[requestId] as IncomingRequest.TransactionRequest)
    }

    override fun getAuthorizedRequest(requestId: String): IncomingRequest.AuthorizedRequest {
        require(listOfIncomingRequests.containsKey(requestId)) {
            "IncomingRequestRepository does not contain this request"
        }

        return (listOfIncomingRequests[requestId] as IncomingRequest.AuthorizedRequest)
    }

    override fun removeAll() {
        listOfIncomingRequests.clear()
    }

    override fun getAmountOfRequests() = listOfIncomingRequests.size
}
