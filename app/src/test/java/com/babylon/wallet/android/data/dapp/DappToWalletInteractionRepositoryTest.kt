package com.babylon.wallet.android.data.dapp

import com.babylon.wallet.android.domain.model.messages.DappToWalletInteraction
import com.babylon.wallet.android.domain.model.messages.WalletAuthorizedRequest
import com.babylon.wallet.android.domain.model.messages.RemoteEntityID
import com.babylon.wallet.android.utils.AppEvent
import com.babylon.wallet.android.utils.AppEventBusImpl
import com.radixdlt.sargon.NetworkId
import com.radixdlt.sargon.newWalletInteractionVersionCurrent
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.spyk
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
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
class DappToWalletInteractionRepositoryTest {

    private val eventBus = spyk<AppEventBusImpl>()
    private val incomingRequestRepository = IncomingRequestRepositoryImpl(eventBus)
    private val amountOfIncomingRequests = 100
    private val sampleIncomingRequest = WalletAuthorizedRequest(
        remoteEntityId = RemoteEntityID.ConnectorId("remoteConnectorId"),
        interactionId = UUID.randomUUID().toString(),
        requestMetadata = DappToWalletInteraction.RequestMetadata(
            version = newWalletInteractionVersionCurrent(),
            networkId = NetworkId.MAINNET,
            origin = "",
            dAppDefinitionAddress = "",
            isInternal = false
        ),
        authRequestItem = WalletAuthorizedRequest.AuthRequestItem.LoginRequest.WithoutChallenge,
        ongoingAccountsRequestItem = DappToWalletInteraction.AccountsRequestItem(
            isOngoing = true,
            numberOfValues = DappToWalletInteraction.NumberOfValues(
                1,
                DappToWalletInteraction.NumberOfValues.Quantifier.Exactly
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
                                dappToWalletInteraction = sampleIncomingRequest.copy(
                                    interactionId = UUID.randomUUID().toString()
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
        var currentRequest: DappToWalletInteraction? = null
        incomingRequestRepository.currentRequestToHandle
            .onEach { currentRequest = it }
            .launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        val interactionIds = (1..5).map { UUID.randomUUID().toString() }
        interactionIds.forEach { id -> // and in each of them, add an incoming request
            incomingRequestRepository.add(dappToWalletInteraction = sampleIncomingRequest.copy(interactionId = id))
        }
        advanceUntilIdle()
        assertTrue(incomingRequestRepository.getAmountOfRequests() == 5)
        assert(currentRequest?.interactionId.toString() == interactionIds[0])
        incomingRequestRepository.requestHandled(interactionIds[0])
        incomingRequestRepository.requestHandled(interactionIds[1])
        advanceUntilIdle()
        assert(currentRequest?.interactionId.toString() == interactionIds[2])
        assertTrue(incomingRequestRepository.getAmountOfRequests() == 3)
        incomingRequestRepository.requestHandled(interactionIds[2])
        incomingRequestRepository.requestHandled(interactionIds[3])
        advanceUntilIdle()
        assert(currentRequest?.interactionId.toString() == interactionIds[4])
        assertTrue(incomingRequestRepository.getAmountOfRequests() == 1)
        incomingRequestRepository.requestHandled(interactionIds[4])
        assertTrue(incomingRequestRepository.getAmountOfRequests() == 0)
    }

    @Test
    fun `adding mobile connect request and deferring current makes mobile request new current, while deferred event stays in queue`() =
        runTest {
            var currentRequest: DappToWalletInteraction? = null
            val interactionId1 = UUID.randomUUID().toString()
            val interactionId2 = UUID.randomUUID().toString()
            incomingRequestRepository.currentRequestToHandle
                .onEach { currentRequest = it }
                .launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
            incomingRequestRepository.add(dappToWalletInteraction = sampleIncomingRequest.copy(interactionId = interactionId1))
            advanceUntilIdle()
            assertTrue(incomingRequestRepository.getAmountOfRequests() == 1)
            assert(currentRequest?.interactionId == interactionId1)
            incomingRequestRepository.addPriorityRequest(sampleIncomingRequest.copy(interactionId = interactionId2))
            incomingRequestRepository.requestDeferred(interactionId1)
            advanceUntilIdle()
            assert(currentRequest?.interactionId == interactionId2)
            assertTrue(incomingRequestRepository.getAmountOfRequests() == 2)
            coVerify(exactly = 1) { eventBus.sendEvent(AppEvent.DeferRequestHandling(interactionId1)) }
        }

    @Test
    fun `addFirst inserts item at 2nd position when there is high priority screen`() = runTest {
        var currentRequest: DappToWalletInteraction? = null
        val interactionId1 = UUID.randomUUID().toString()
        val interactionId2 = UUID.randomUUID().toString()
        incomingRequestRepository.currentRequestToHandle
            .onEach { currentRequest = it }
            .launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        incomingRequestRepository.pauseIncomingRequests()
        incomingRequestRepository.add(dappToWalletInteraction = sampleIncomingRequest.copy(interactionId = interactionId1))
        advanceUntilIdle()
        assertTrue(incomingRequestRepository.getAmountOfRequests() == 1)
        assert(currentRequest?.interactionId == null)
        incomingRequestRepository.addPriorityRequest(sampleIncomingRequest.copy(interactionId = interactionId2))
        advanceUntilIdle()
        incomingRequestRepository.resumeIncomingRequests()
        advanceUntilIdle()
        assert(currentRequest?.interactionId == interactionId2)
        assertTrue(incomingRequestRepository.getAmountOfRequests() == 2)
    }

    @Test
    fun `reading buffered request clears it`() = runTest {
        val request = mockk<DappToWalletInteraction>()
        incomingRequestRepository.setBufferedRequest(request)
        assert(incomingRequestRepository.consumeBufferedRequest() != null)
        assert(incomingRequestRepository.consumeBufferedRequest() == null)
    }
}
