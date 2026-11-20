package com.babylon.wallet.android.data.repository.p2plink

import com.babylon.wallet.android.data.dapp.PeerdroidClient
import com.radixdlt.sargon.P2pLink
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.extensions.asIdentifiable
import com.radixdlt.sargon.extensions.id
import com.radixdlt.sargon.extensions.toJson
import com.radixdlt.sargon.samples.sample
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import rdx.works.profile.datastore.EncryptedPreferencesManager

@OptIn(ExperimentalCoroutinesApi::class, UsesSampleValues::class)
class P2PLinksRepositoryTest {

    private val link = P2pLink.sample()
    private val storedLinks = MutableStateFlow<Result<String>?>(Result.success(listOf(link).asIdentifiable().toJson()))
    private val encryptedPreferencesManager = mockk<EncryptedPreferencesManager> {
        every { getP2PLinkListJson() } returns storedLinks
        coEvery { saveP2PLinkListJson(any()) } coAnswers { storedLinks.value = Result.success(firstArg()) }
    }
    private val peerdroidClient = mockk<PeerdroidClient>(relaxUnitFun = true)
    private val retiredP2PLinksRepository = mockk<RetiredP2PLinksRepository>()

    @Test
    fun `a link whose removal notice cannot be kept is deleted at once`() = runTest {
        coEvery { retiredP2PLinksRepository.retire(any(), any()) } throws IllegalStateException("cannot encrypt")
        val sut = P2PLinksRepositoryImpl(
            peerdroidClient = peerdroidClient,
            retiredP2PLinksRepository = retiredP2PLinksRepository,
            encryptedPreferencesManager = encryptedPreferencesManager,
            preferencesManager = mockk(relaxed = true),
            ioDispatcher = UnconfinedTestDispatcher(testScheduler)
        )

        sut.removeP2PLink(link.id)

        coVerify { peerdroidClient.deleteLink(link.connectionPassword) }
    }

    @Test
    fun `a link removed with its notice kept is not deleted yet`() = runTest {
        coEvery { retiredP2PLinksRepository.retire(any(), any()) } returns Unit
        val sut = P2PLinksRepositoryImpl(
            peerdroidClient = peerdroidClient,
            retiredP2PLinksRepository = retiredP2PLinksRepository,
            encryptedPreferencesManager = encryptedPreferencesManager,
            preferencesManager = mockk(relaxed = true),
            ioDispatcher = UnconfinedTestDispatcher(testScheduler)
        )

        sut.removeP2PLink(link.id)

        coVerify { retiredP2PLinksRepository.retire(link.connectionPassword, any()) }
        coVerify(exactly = 0) { peerdroidClient.deleteLink(any()) }
    }
}
