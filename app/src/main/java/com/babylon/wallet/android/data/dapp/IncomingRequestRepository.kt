package com.babylon.wallet.android.data.dapp

import com.babylon.wallet.android.domain.model.messages.DappToWalletInteraction
import com.babylon.wallet.android.utils.AppEvent
import com.babylon.wallet.android.utils.AppEventBus
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

    val currentRequestToHandle: Flow<DappToWalletInteraction>

    suspend fun add(dappToWalletInteraction: DappToWalletInteraction)

    suspend fun addPriorityRequest(dappToWalletInteraction: DappToWalletInteraction)

    suspend fun requestHandled(requestId: String)

    suspend fun pauseIncomingRequests()

    suspend fun resumeIncomingRequests()

    fun getRequest(requestId: String): DappToWalletInteraction?

    fun removeAll()

    fun getAmountOfRequests(): Int

    suspend fun requestDeferred(requestId: String)

    fun consumeBufferedRequest(): DappToWalletInteraction?

    fun setBufferedRequest(request: DappToWalletInteraction)
}

@Suppress("TooManyFunctions")
class IncomingRequestRepositoryImpl @Inject constructor(
    private val appEventBus: AppEventBus
) : IncomingRequestRepository {

    private val logger = Timber.tag("IncomingRequestRepository")

    private val requestQueue = LinkedList<QueueItem>()

    /**
     * Current request is saved in a state flow. This is needed in order to know if the topmost
     * request in [requestQueue] is being handled right now.
     */
    private val _currentRequestToHandle = MutableStateFlow<DappToWalletInteraction?>(null)

    /**
     * The exposed request to handled is a shared flow, there is no need for the client to know which
     * is the current state of the flow. The client just reacts to incoming requests.
     */
    override val currentRequestToHandle = _currentRequestToHandle.asSharedFlow().filterNotNull()

    private val mutex = Mutex()

    /**
     * Request that can come in via deep link before wallet is setup.
     */
    private var bufferedRequest: DappToWalletInteraction? = null

    override fun setBufferedRequest(request: DappToWalletInteraction) {
        bufferedRequest = request
    }

    override fun consumeBufferedRequest(): DappToWalletInteraction? {
        return bufferedRequest?.also { bufferedRequest = null }
    }

    override suspend fun add(dappToWalletInteraction: DappToWalletInteraction) {
        mutex.withLock {
            val requestItem = QueueItem.RequestItem(dappToWalletInteraction)

            if (dappToWalletInteraction.isInternal) {
                requestQueue.addFirst(requestItem)
            } else {
                requestQueue.add(requestItem)
            }
            handleNextRequest()
            logger.d(
                "🗂 new incoming request with id ${dappToWalletInteraction.interactionId} added in list, " +
                    "so size now is ${getAmountOfRequests()}"
            )
        }
    }

    /**
     * There are two path of execution when using this method:
     * - there is high priority screen in the queue, so the incoming request is added below it,
     * taking priority over requests currently in the queue
     * - there is no high priority screen: request is added at the top of queue and if there other request currently handled,
     * we send a defer event for it so that UI can react and defer handling, without removing it from the queue.
     * Deferred request will be handled again when top priority one handling completes
     */
    override suspend fun addPriorityRequest(dappToWalletInteraction: DappToWalletInteraction) {
        mutex.withLock {
            requestQueue.addFirst(QueueItem.RequestItem(dappToWalletInteraction))
            val currentRequest = _currentRequestToHandle.value
            val handlingPaused = requestQueue.contains(QueueItem.HighPriorityScreen)
            when {
                currentRequest != null -> {
                    logger.d("🗂 Deferring request with id ${currentRequest.interactionId}")
                    appEventBus.sendEvent(AppEvent.DeferRequestHandling(currentRequest.interactionId))
                }

                else -> {
                    if (!handlingPaused) {
                        handleNextRequest()
                    }
                }
            }
        }
    }

    override suspend fun requestHandled(requestId: String) {
        mutex.withLock {
            requestQueue.removeIf { it is QueueItem.RequestItem && it.dappToWalletInteraction.interactionId == requestId }
            clearCurrent(requestId)
            handleNextRequest()
            logger.d("🗂 request $requestId handled so size of list is now: ${getAmountOfRequests()}")
        }
    }

    override suspend fun requestDeferred(requestId: String) {
        mutex.withLock {
            clearCurrent(requestId)
            handleNextRequest()
            logger.d("🗂 request $requestId handled so size of list is now: ${getAmountOfRequests()}")
        }
    }

    private suspend fun clearCurrent(requestId: String) {
        if (_currentRequestToHandle.value?.interactionId == requestId) {
            _currentRequestToHandle.emit(null)
        }
    }

    override suspend fun pauseIncomingRequests() {
        mutex.withLock {
            // If the queue knows about any high priority item, no need to add it again
            if (requestQueue.contains(QueueItem.HighPriorityScreen)) {
                return
            }

            // Put high priority item below any internal request and mobile connect requests
            val index = requestQueue.indexOfFirst {
                val item = it as? QueueItem.RequestItem
                item != null && !item.dappToWalletInteraction.isInternal
            }
            if (index != -1) {
                requestQueue.add(index, QueueItem.HighPriorityScreen)
            } else {
                requestQueue.addFirst(QueueItem.HighPriorityScreen)
            }
            logger.d("🗂 Temporarily pausing incoming message queue")
        }
    }

    override suspend fun resumeIncomingRequests() {
        mutex.withLock {
            val removed = requestQueue.removeIf { it is QueueItem.HighPriorityScreen }
            if (removed) {
                handleNextRequest()
                logger.d("🗂 Resuming incoming message queue")
            }
        }
    }

    override fun getRequest(requestId: String): DappToWalletInteraction? {
        val queueItem = requestQueue.find {
            it is QueueItem.RequestItem && it.dappToWalletInteraction.interactionId == requestId
        }
        if (queueItem == null) {
            logger.w("Request with id $requestId is null")
        }
        logger.d("\uD83D\uDDC2 get request $requestId")
        return (queueItem as? QueueItem.RequestItem)?.dappToWalletInteraction
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
        if (nextRequest is QueueItem.RequestItem && _currentRequestToHandle.value != nextRequest.dappToWalletInteraction) {
            _currentRequestToHandle.emit(nextRequest.dappToWalletInteraction)
        }
    }

    private sealed interface QueueItem {
        data object HighPriorityScreen : QueueItem
        data class RequestItem(val dappToWalletInteraction: DappToWalletInteraction) : QueueItem
    }
}
