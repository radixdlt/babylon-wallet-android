package com.babylon.wallet.android.data.dapp

import com.babylon.wallet.android.data.repository.p2plink.P2PLinksRepository
import com.radixdlt.sargon.DappWalletInteractionErrorType
import com.radixdlt.sargon.P2pLink
import com.radixdlt.sargon.WalletToDappInteractionFailureResponse
import com.radixdlt.sargon.WalletToDappInteractionResponse
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.extensions.P2pLinks
import com.radixdlt.sargon.extensions.asIdentifiable
import com.radixdlt.sargon.samples.sample
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import rdx.works.peerdroid.domain.ConnectionIdHolder
import rdx.works.peerdroid.domain.MessageNotSentException

@OptIn(ExperimentalCoroutinesApi::class, UsesSampleValues::class)
class DappMessengerImplTest {

    private val link = P2pLink.sample()
    private val connectorId = ConnectionIdHolder(link.connectionPassword).id
    private val links = MutableStateFlow<P2pLinks>(listOf(link).asIdentifiable())
    private val p2PLinksRepository = mockk<P2PLinksRepository> {
        every { observeP2PLinks() } returns links
    }

    private val open = MutableStateFlow(false)
    private val sendResults = ArrayDeque<Result<Unit>>()
    private val sent = mutableListOf<String>()
    private var attempts = 0

    // A send takes a moment (the data channel may still be opening), then fails or succeeds as told.
    private val peerdroidClient = mockk<PeerdroidClient> {
        every { isConnectionOpen(connectorId) } returns open
        coEvery { sendMessage(connectorId, any<String>()) } coAnswers {
            delay(SEND_MS)
            attempts++
            val result = sendResults.removeFirstOrNull() ?: Result.success(Unit)
            if (result.isSuccess) sent += secondArg<String>()
            result
        }
    }

    private fun TestScope.messenger() = DappMessengerImpl(peerdroidClient, p2PLinksRepository, backgroundScope)

    private fun notSent() = Result.failure<Unit>(MessageNotSentException("no open data channel"))

    @Test
    fun `the answer goes out even when the screen that asked closes while it is being sent`() = runTest {
        val sut = messenger()
        val screen = launch { sut.sendWalletInteractionResponseFailure(remoteConnectorId = connectorId, payload = "rejected") }
        runCurrent()

        // The person rejected: the request screen closes, and its work is cancelled.
        screen.cancel()
        advanceTimeBy(SEND_MS + 1)
        runCurrent()

        assertEquals(listOf("rejected"), sent)
    }

    @Test
    fun `a first send that did not go out returns at once, and the answer is sent again once the link opens`() = runTest {
        val sut = messenger()
        sendResults += notSent()

        val result = sut.sendWalletInteractionResponseFailure(connectorId, "signed")

        assertTrue(result.exceptionOrNull() is MessageNotSentException)
        assertEquals("the caller is not held by the attempts that follow", SEND_MS, currentTime)
        advanceTimeBy(5_000)
        assertEquals("waits for the link", emptyList<String>(), sent)

        open.value = true
        advanceTimeBy(2_000)

        assertEquals(listOf("signed"), sent)
        assertEquals(2, attempts)
    }

    @Test
    fun `an answer that may have been delivered is not sent again`() = runTest {
        val sut = messenger()
        open.value = true
        sendResults += Result.failure(IllegalStateException("the data channel did not take a package after 1 packages"))

        assertTrue(sut.sendWalletInteractionResponseFailure(connectorId, "signed").isFailure)
        advanceTimeBy(61_000)

        assertEquals(1, attempts)
        assertEquals(emptyList<String>(), sent)
    }

    @Test
    fun `an answer is not sent again once its link is removed`() = runTest {
        val sut = messenger()
        sendResults += notSent()

        sut.sendWalletInteractionResponseFailure(connectorId, "signed")
        advanceTimeBy(2_000)
        links.value = emptyList<P2pLink>().asIdentifiable()
        runCurrent()
        open.value = true
        advanceTimeBy(5_000)

        assertEquals(1, attempts)
    }

    @Test
    fun `an answer is not sent again for ever`() = runTest {
        val sut = messenger()
        sendResults += notSent()

        sut.sendWalletInteractionResponseFailure(connectorId, "signed")
        advanceTimeBy(61_000)
        open.value = true
        advanceTimeBy(5_000)

        assertEquals(1, attempts)
    }

    @Test
    fun `the answer to a submitted transaction is sent again when it did not go out, if the caller asks`() = runTest {
        val sut = messenger()
        sendResults += notSent()
        open.value = true

        sut.sendTransactionWriteResponseSuccess(connectorId, "submitted", resendIfNotSent = true)
        advanceTimeBy(2_000)

        assertEquals(listOf("submitted"), sent)
    }

    @Test
    fun `an answer whose failure the caller acts on is not sent again`() = runTest {
        val sut = messenger()
        open.value = true
        sendResults += notSent()
        sendResults += notSent()

        // A pre-authorization: its failure is shown, and the person may sign again.
        assertTrue(sut.sendTransactionWriteResponseSuccess(connectorId, "signed subintent").isFailure)
        // A login or data request: the wallet does not record what it shared when the answer fails.
        val response = WalletToDappInteractionResponse.Failure(
            v1 = WalletToDappInteractionFailureResponse(
                interactionId = "1",
                error = DappWalletInteractionErrorType.REJECTED_BY_USER,
                message = null
            )
        )
        assertTrue(sut.sendWalletInteractionSuccessResponse(connectorId, response).isFailure)
        advanceTimeBy(61_000)

        assertEquals(2, attempts)
        assertEquals(emptyList<String>(), sent)
    }

    private companion object {
        const val SEND_MS = 300L
    }
}
