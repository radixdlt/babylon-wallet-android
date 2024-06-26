package com.babylon.wallet.android.data.repository

import com.babylon.wallet.android.domain.model.IncomingMessage
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BufferedMobileConnectRequestRepository @Inject constructor() {

    private var bufferedRequest: IncomingMessage.IncomingRequest? = null

    fun setBufferedRequest(request: IncomingMessage.IncomingRequest) {
        bufferedRequest = request
    }

    fun getBufferedRequest(): IncomingMessage.IncomingRequest? {
        return bufferedRequest?.also { bufferedRequest = null }
    }
}
