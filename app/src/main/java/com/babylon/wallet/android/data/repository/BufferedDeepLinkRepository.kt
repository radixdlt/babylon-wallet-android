package com.babylon.wallet.android.data.repository

import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import com.babylon.wallet.android.domain.model.IncomingMessage
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BufferedDeepLinkRepository @Inject constructor(
    private val incomingRequestRepository: IncomingRequestRepository,
) {

    private var bufferedRequest: IncomingMessage.IncomingRequest? = null

    fun setBufferedRequest(request: IncomingMessage.IncomingRequest) {
        bufferedRequest = request
    }

    suspend fun processBufferedRequest() {
        bufferedRequest?.let {
            incomingRequestRepository.addMobileConnectRequest(it)
            bufferedRequest = null
        }
    }
}
