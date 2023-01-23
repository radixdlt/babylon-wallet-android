package com.babylon.wallet.android.data.dapp

import com.babylon.wallet.android.domain.model.MessageFromDataChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
class IncomingRequestRepositoryTest {

    private val incomingRequestRepository = IncomingRequestRepository()
    private val amountOfIncomingRequests = 1000

    @Test
    fun `given 1000 consequent incoming requests, when adding all of them one by one, then the amount of the incoming requests is 1000`() =
        runTest {
            // run on 4 threads
            val coroutineScope = CoroutineScope(
                newFixedThreadPoolContext(4, "thread pool")
            )

            coroutineScope.launch {
                val coroutines = 1.rangeTo(1000).map { // create 1000 coroutines
                    launch {
                        for (i in 1..amountOfIncomingRequests) { // and in each of them, add an incoming request
                            incomingRequestRepository.add(
                                incomingRequest = MessageFromDataChannel.IncomingRequest.AccountsRequest(
                                    requestId = i.toString(),
                                    isOngoing = false,
                                    requiresProofOfOwnership = false,
                                    numberOfAccounts = i,
                                    quantifier = MessageFromDataChannel.AccountNumberQuantifier.Exactly
                                )
                            )
                        }
                    }
                }

                coroutines.forEach { coroutine ->
                    coroutine.join() // wait for all coroutines to finish their jobs.
                }
            }.join()

            assertTrue(incomingRequestRepository.getAmountOfRequests() == amountOfIncomingRequests)
        }

    @Test
    fun `after being handled, next request is set as current`() = runTest {
        var currentRequest: MessageFromDataChannel.IncomingRequest? = null
        incomingRequestRepository.currentRequestToHandle
            .onEach { currentRequest = it }
            .launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        for (i in 1..5) { // and in each of them, add an incoming request
            incomingRequestRepository.add(
                incomingRequest = MessageFromDataChannel.IncomingRequest.AccountsRequest(
                    requestId = i.toString(),
                    isOngoing = false,
                    requiresProofOfOwnership = false,
                    numberOfAccounts = i,
                    quantifier = MessageFromDataChannel.AccountNumberQuantifier.Exactly
                )
            )
        }
        advanceUntilIdle()
        assertTrue(incomingRequestRepository.getAmountOfRequests() == 5)
        assert(currentRequest?.id == "1")
        incomingRequestRepository.requestHandled("1")
        incomingRequestRepository.requestHandled("2")
        advanceUntilIdle()
        assert(currentRequest?.id == "3")
        assertTrue(incomingRequestRepository.getAmountOfRequests() == 3)
        incomingRequestRepository.requestHandled("3")
        incomingRequestRepository.requestHandled("4")
        advanceUntilIdle()
        assert(currentRequest?.id == "5")
        assertTrue(incomingRequestRepository.getAmountOfRequests() == 1)
        incomingRequestRepository.requestHandled("5")
        assertTrue(incomingRequestRepository.getAmountOfRequests() == 0)
    }
}
