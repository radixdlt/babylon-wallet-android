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
import rdx.works.core.UUIDGenerator

@OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
class IncomingRequestRepositoryTest {

    private val incomingRequestRepository = IncomingRequestRepositoryImpl()
    private val amountOfIncomingRequests = 100
    private val sampleIncomingRequest = MessageFromDataChannel.IncomingRequest.AuthorizedRequest(
        dappId = "dappId",
        interactionId = UUIDGenerator.uuid().toString(),
        requestMetadata = MessageFromDataChannel.IncomingRequest.RequestMetadata(1, "", "", false),
        authRequest = MessageFromDataChannel.IncomingRequest.AuthorizedRequest.AuthRequest.LoginRequest.WithoutChallenge,
        ongoingAccountsRequestItem = MessageFromDataChannel.IncomingRequest.AccountsRequestItem(
            isOngoing = true,
            numberOfAccounts = 1,
            numberOfValues = MessageFromDataChannel.IncomingRequest.NumberOfValues.Exactly,
            challenge = null
        )
    )

    @Test
    fun `given 1000 consequent incoming requests, when adding all of them one by one, then the amount of the incoming requests is 1000`() =
        runTest {
            // run on 4 threads
            val coroutineScope = CoroutineScope(
                newFixedThreadPoolContext(4, "thread pool")
            )

            coroutineScope.launch {
                val coroutines = 1.rangeTo(10).map { // create 1000 coroutines
                    launch {
                        for (i in 1..amountOfIncomingRequests) { // and in each of them, add an incoming request
                            incomingRequestRepository.add(incomingRequest = sampleIncomingRequest.copy(interactionId = UUIDGenerator.uuid().toString()))
                        }
                    }
                }

                coroutines.forEach { coroutine ->
                    coroutine.join() // wait for all coroutines to finish their jobs.
                }
            }.join()

            assertTrue(incomingRequestRepository.getAmountOfRequests() == amountOfIncomingRequests * 10)
        }

    @Test
    fun `after being handled, next request is set as current`() = runTest {
        var currentRequest: MessageFromDataChannel.IncomingRequest? = null
        incomingRequestRepository.currentRequestToHandle
            .onEach { currentRequest = it }
            .launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        for (i in 1..5) { // and in each of them, add an incoming request
            incomingRequestRepository.add(incomingRequest = sampleIncomingRequest.copy(interactionId = i.toString()))
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
