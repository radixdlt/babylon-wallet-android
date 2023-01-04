package com.babylon.wallet.android.data.dapp

import com.babylon.wallet.android.domain.model.MessageFromDataChannel.IncomingRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IncomingRequestRepository @Inject constructor() {

    private val listOfIncomingRequests = mutableMapOf<String, IncomingRequest>()

    fun add(incomingRequest: IncomingRequest) {
        synchronized(this) {
            val id = incomingRequest.id
            if (id != null) {
                listOfIncomingRequests.putIfAbsent(id, incomingRequest)
            }
        }
    }

    fun getAccountsRequest(requestId: String): IncomingRequest.AccountsRequest {
        require(listOfIncomingRequests.containsKey(requestId)) {
            "IncomingRequestRepository does not contain this request"
        }

        return (listOfIncomingRequests.remove(requestId) as IncomingRequest.AccountsRequest)
    }

    fun getTransactionWriteRequest(requestId: String): IncomingRequest.TransactionWriteRequest {
        require(listOfIncomingRequests.containsKey(requestId)) {
            "IncomingRequestRepository does not contain this request"
        }

        return (listOfIncomingRequests.remove(requestId) as IncomingRequest.TransactionWriteRequest)
    }

    fun getAmountOfRequests() = listOfIncomingRequests.size
}
