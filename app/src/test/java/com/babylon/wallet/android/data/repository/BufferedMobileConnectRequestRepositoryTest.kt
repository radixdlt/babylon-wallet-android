package com.babylon.wallet.android.data.repository

import com.babylon.wallet.android.domain.model.IncomingMessage
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class BufferedMobileConnectRequestRepositoryTest {

    private lateinit var bufferedMobileConnectRequestRepository: BufferedMobileConnectRequestRepository

    @Before
    fun setup() {
        bufferedMobileConnectRequestRepository = BufferedMobileConnectRequestRepository()
    }

    @Test
    fun `reading buffered request clears it`() = runTest {
        val request = mockk<IncomingMessage.IncomingRequest>()
        bufferedMobileConnectRequestRepository.setBufferedRequest(request)
        bufferedMobileConnectRequestRepository.getBufferedRequest()
        assert(bufferedMobileConnectRequestRepository.getBufferedRequest() == null)
    }

}