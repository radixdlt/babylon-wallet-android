package com.babylon.wallet.android.data.dapp

import com.babylon.wallet.android.domain.model.IncomingMessage
import com.radixdlt.sargon.NetworkId
import com.radixdlt.sargon.WalletInteractionId
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

    private val incomingRequestRepository = IncomingRequestRepositoryImpl()
    private val amountOfIncomingRequests = 100
    private val sampleIncomingRequest = IncomingMessage.IncomingRequest.AuthorizedRequest(
        remoteEntityId = IncomingMessage.RemoteEntityID.ConnectorId("remoteConnectorId"),
        interactionId = WalletInteractionId.randomUUID(),
        requestMetadata = IncomingMessage.IncomingRequest.RequestMetadata(NetworkId.MAINNET, "", "", false),
        authRequest = IncomingMessage.IncomingRequest.AuthorizedRequest.AuthRequest.LoginRequest.WithoutChallenge,
        ongoingAccountsRequestItem = IncomingMessage.IncomingRequest.AccountsRequestItem(
            isOngoing = true,
            numberOfValues = IncomingMessage.IncomingRequest.NumberOfValues(
                1,
                IncomingMessage.IncomingRequest.NumberOfValues.Quantifier.Exactly
            ),
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
                            incomingRequestRepository.add(
                                incomingRequest = sampleIncomingRequest.copy(
                                    interactionId = WalletInteractionId.randomUUID()
                                )
                            )
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
        var currentRequest: IncomingMessage.IncomingRequest? = null
        incomingRequestRepository.currentRequestToHandle
            .onEach { currentRequest = it }
            .launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        val interactionIds = (1..5).map { WalletInteractionId.randomUUID() }
        interactionIds.forEach { id -> // and in each of them, add an incoming request
            incomingRequestRepository.add(incomingRequest = sampleIncomingRequest.copy(interactionId = id))
        }
        advanceUntilIdle()
        assertTrue(incomingRequestRepository.getAmountOfRequests() == 5)
        assert(currentRequest?.interactionId.toString() == interactionIds[0].toString())
        incomingRequestRepository.requestHandled(interactionIds[0].toString())
        incomingRequestRepository.requestHandled(interactionIds[1].toString())
        advanceUntilIdle()
        assert(currentRequest?.interactionId.toString() == interactionIds[2].toString())
        assertTrue(incomingRequestRepository.getAmountOfRequests() == 3)
        incomingRequestRepository.requestHandled(interactionIds[2].toString())
        incomingRequestRepository.requestHandled(interactionIds[3].toString())
        advanceUntilIdle()
        assert(currentRequest?.interactionId.toString() == interactionIds[4].toString())
        assertTrue(incomingRequestRepository.getAmountOfRequests() == 1)
        incomingRequestRepository.requestHandled(interactionIds[4].toString())
        assertTrue(incomingRequestRepository.getAmountOfRequests() == 0)
    }
}
