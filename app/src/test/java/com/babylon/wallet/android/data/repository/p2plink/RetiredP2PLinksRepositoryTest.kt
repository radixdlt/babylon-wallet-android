package com.babylon.wallet.android.data.repository.p2plink

import com.radixdlt.sargon.Exactly32Bytes
import com.radixdlt.sargon.RadixConnectPassword
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.toBagOfBytes
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import rdx.works.profile.datastore.EncryptedPreferencesManager
import kotlin.time.Duration.Companion.days

@OptIn(ExperimentalCoroutinesApi::class)
class RetiredP2PLinksRepositoryTest {

    private val stored = MutableStateFlow<Result<String>?>(null)
    private var reads = 0
    private var saveGate: CompletableDeferred<Unit>? = null
    private val preferences = mockk<EncryptedPreferencesManager> {
        every { getRetiredP2PLinksJson() } answers {
            reads++
            stored
        }
        coEvery { saveRetiredP2PLinksJson(any()) } coAnswers {
            saveGate?.await()
            stored.value = Result.success(firstArg())
        }
    }

    private fun password(seed: Int) =
        RadixConnectPassword.init(Exactly32Bytes.init(ByteArray(32) { (seed + it).toByte() }.toBagOfBytes()))

    @Test
    fun `keeps one notice per link, until it is forgotten`() = runTest {
        val sut = RetiredP2PLinksRepositoryImpl(preferences, UnconfinedTestDispatcher(testScheduler))

        sut.retire(password(1), nowMillis = 10)
        sut.retire(password(2), nowMillis = 20)
        sut.retire(password(1), nowMillis = 30)

        val links = sut.observeRetiredP2PLinks().first()
        assertEquals(listOf(password(2) to 20L, password(1) to 30L), links.map { it.connectionPassword to it.retiredAtMillis })
        assertTrue(sut.isRetired(links.last().connectorId))

        sut.forget(links.last().connectorId)
        assertFalse(sut.isRetired(links.last().connectorId))
        assertEquals(1, sut.observeRetiredP2PLinks().first().size)
    }

    @Test
    fun `what cannot be read is no notice`() = runTest {
        val sut = RetiredP2PLinksRepositoryImpl(preferences, UnconfinedTestDispatcher(testScheduler))

        stored.value = Result.success("not json")
        assertEquals(emptyList<RetiredP2PLink>(), sut.observeRetiredP2PLinks().first())
        stored.value = Result.failure(IllegalStateException("cannot decrypt"))
        assertEquals(emptyList<RetiredP2PLink>(), sut.observeRetiredP2PLinks().first())
    }

    @Test
    fun `a notice expires after its lifetime`() {
        val link = RetiredP2PLink(password(1), retiredAtMillis = 0)

        assertFalse(link.isExpired(7.days.inWholeMilliseconds - 1))
        assertTrue(link.isExpired(7.days.inWholeMilliseconds))
        assertEquals(1L, link.millisUntilExpiry(7.days.inWholeMilliseconds - 1))
        assertEquals(0L, link.millisUntilExpiry(8.days.inWholeMilliseconds))
    }

    @Test
    fun `a list that cannot be read is read again for the next message`() = runTest {
        RetiredP2PLinksRepositoryImpl(preferences, UnconfinedTestDispatcher(testScheduler)).retire(password(1), nowMillis = 10)
        val readable = stored.value
        val connectorId = RetiredP2PLink(password(1), retiredAtMillis = 10).connectorId
        val sut = RetiredP2PLinksRepositoryImpl(preferences, UnconfinedTestDispatcher(testScheduler))

        stored.value = Result.failure(IllegalStateException("cannot decrypt"))
        assertFalse(sut.isRetired(connectorId))
        stored.value = readable
        assertTrue(sut.isRetired(connectorId))
    }

    @Test
    fun `a message checked while a link is being retired sees it retired`() = runTest {
        val sut = RetiredP2PLinksRepositoryImpl(preferences, UnconfinedTestDispatcher(testScheduler))
        val connectorId = RetiredP2PLink(password(1), retiredAtMillis = 10).connectorId
        saveGate = CompletableDeferred()

        val retiring = async { sut.retire(password(1), nowMillis = 10) }
        runCurrent()
        val checked = async { sut.isRetired(connectorId) }
        runCurrent()
        saveGate?.complete(Unit)

        retiring.await()
        assertTrue(checked.await())
    }

    @Test
    fun `the retired links are read once for all the incoming messages, and kept up to date`() = runTest {
        RetiredP2PLinksRepositoryImpl(preferences, UnconfinedTestDispatcher(testScheduler)).retire(password(1), nowMillis = 10)
        val sut = RetiredP2PLinksRepositoryImpl(preferences, UnconfinedTestDispatcher(testScheduler))
        val connectorId = sut.observeRetiredP2PLinks().first().single().connectorId
        reads = 0

        repeat(3) { assertTrue(sut.isRetired(connectorId)) }
        assertEquals(1, reads)

        sut.forget(connectorId)
        assertFalse(sut.isRetired(connectorId))
    }
}
