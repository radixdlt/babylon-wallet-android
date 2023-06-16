package com.babylon.wallet.android.data.dapp

import com.babylon.wallet.android.domain.model.MessageFromDataChannel.IncomingRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import java.util.LinkedList
import javax.inject.Inject

interface IncomingRequestRepository {

    val currentRequestToHandle: Flow<IncomingRequest>

    suspend fun add(incomingRequest: IncomingRequest)

    suspend fun requestHandled(requestId: String)

    suspend fun pauseIncomingRequests()

    suspend fun resumeIncomingRequests()

    fun getUnauthorizedRequest(requestId: String): IncomingRequest.UnauthorizedRequest

    fun getTransactionWriteRequest(requestId: String): IncomingRequest.TransactionRequest

    fun getAuthorizedRequest(requestId: String): IncomingRequest.AuthorizedRequest

    fun removeAll()

    fun getAmountOfRequests(): Int
}

class IncomingRequestRepositoryImpl @Inject constructor() : IncomingRequestRepository {

    private val requestQueue = LinkedList<QueueItem>()

    /**
     * Current request is saved in a state flow. This is needed in order to know if the topmost
     * request in [requestQueue] is being handled right now.
     */
    private val _currentRequestToHandle = MutableStateFlow<IncomingRequest?>(null)

    /**
     * The exposed request to handled is a shared flow, there is no need for the client to know which
     * is the current state of the flow. The client just reacts to incoming requests.
     */
    override val currentRequestToHandle = _currentRequestToHandle.asSharedFlow().filterNotNull()

    private val mutex = Mutex()

    override suspend fun add(incomingRequest: IncomingRequest) {
        mutex.withLock {
            val requestItem = QueueItem.RequestItem(incomingRequest)

            if (incomingRequest.isInternal) {
                requestQueue.addFirst(requestItem)
            } else {
                requestQueue.add(requestItem)
            }
            handleNextRequest()
            Timber.d("🗂 new incoming request with id ${incomingRequest.id} added in list, so size now is ${getAmountOfRequests()}")
        }
    }

    override suspend fun requestHandled(requestId: String) {
        mutex.withLock {
            requestQueue.removeIf { it is QueueItem.RequestItem && it.incomingRequest.id == requestId }
            handleNextRequest()
            Timber.d("🗂 request $requestId handled so size of list is now: ${getAmountOfRequests()}")
        }
    }

    override suspend fun pauseIncomingRequests() {
        mutex.withLock {
            // If the queue knows about any high priority item, no need to add it again
            if (requestQueue.contains(QueueItem.HighPriorityScreen)) {
                return
            }

            // Put high priority item below any internal request
            val topQueueItem = requestQueue.peekFirst()
            if (topQueueItem is QueueItem.RequestItem && topQueueItem.incomingRequest.isInternal) {
                requestQueue.add(1, QueueItem.HighPriorityScreen)
            } else {
                requestQueue.addFirst(QueueItem.HighPriorityScreen)
            }
            Timber.d("🗂 Temporarily pausing incoming message queue")
        }
    }

    override suspend fun resumeIncomingRequests() {
        mutex.withLock {
            val removed = requestQueue.removeIf { it is QueueItem.HighPriorityScreen }
            if (removed) {
                handleNextRequest()
                Timber.d("🗂 Resuming incoming message queue")
            }
        }
    }

    override fun getUnauthorizedRequest(requestId: String): IncomingRequest.UnauthorizedRequest {
        val queueItem = requestQueue.find {
            it is QueueItem.RequestItem && it.incomingRequest.id == requestId && it.incomingRequest is IncomingRequest.UnauthorizedRequest
        }

        requireNotNull(queueItem) {
            "IncomingRequestRepository does not contain this request"
        }

        return (queueItem as QueueItem.RequestItem).incomingRequest as IncomingRequest.UnauthorizedRequest
    }

    override fun getTransactionWriteRequest(requestId: String): IncomingRequest.TransactionRequest {
        val queueItem = requestQueue.find {
            it is QueueItem.RequestItem && it.incomingRequest.id == requestId && it.incomingRequest is IncomingRequest.TransactionRequest
        }

        requireNotNull(queueItem) {
            "IncomingRequestRepository does not contain this request"
        }

        return (queueItem as QueueItem.RequestItem).incomingRequest as IncomingRequest.TransactionRequest
    }

    override fun getAuthorizedRequest(requestId: String): IncomingRequest.AuthorizedRequest {
        val queueItem = requestQueue.find {
            it is QueueItem.RequestItem && it.incomingRequest.id == requestId && it.incomingRequest is IncomingRequest.AuthorizedRequest
        }

        requireNotNull(queueItem) {
            "IncomingRequestRepository does not contain this request"
        }

        return (queueItem as QueueItem.RequestItem).incomingRequest as IncomingRequest.AuthorizedRequest
    }

    override fun removeAll() {
        // Remove all incoming requests only, high priority screen queue items are handled by backstack
        requestQueue.removeIf { it is QueueItem.RequestItem }
    }

    override fun getAmountOfRequests() = requestQueue.filterNot { it is QueueItem.HighPriorityScreen }.size

    private suspend fun handleNextRequest() {
        val nextRequest = requestQueue.peekFirst()
        // In order to emit an incoming request, the topmost item should be
        // a. An incoming request
        // b. It should not be the same as the one being handled already
        if (nextRequest is QueueItem.RequestItem && _currentRequestToHandle.value != nextRequest) {
            _currentRequestToHandle.emit(nextRequest.incomingRequest)
        }
    }

    private sealed interface QueueItem {
        object HighPriorityScreen: QueueItem

        data class RequestItem(val incomingRequest: IncomingRequest): QueueItem
    }
}
