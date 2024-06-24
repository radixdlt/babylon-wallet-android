package com.babylon.wallet.android.data.repository

import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import com.babylon.wallet.android.domain.model.IncomingMessage
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class BufferedDeepLinkRepositoryTest {

    private lateinit var bufferedDeepLinkRepository: BufferedDeepLinkRepository
    private val incomingRequestRepository = mockk<IncomingRequestRepository>()

    @Before
    fun setup() {
        bufferedDeepLinkRepository = BufferedDeepLinkRepository(incomingRequestRepository)
        coEvery { incomingRequestRepository.addMobileConnectRequest(any()) } returns Unit
    }

    @Test
    fun `add buffered request to incoming request repository`() = runTest {
        val request = mockk<IncomingMessage.IncomingRequest>()
        bufferedDeepLinkRepository.setBufferedRequest(request)
        bufferedDeepLinkRepository.processBufferedRequest()
        coVerify(exactly = 1) { incomingRequestRepository.addMobileConnectRequest(request) }
    }

}