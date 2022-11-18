package com.babylon.wallet.android.domain.repository.transaction

import com.babylon.wallet.android.data.gateway.GatewayApi
import com.babylon.wallet.android.data.gateway.generated.model.RecentTransactionsRequest
import com.babylon.wallet.android.data.gateway.generated.model.RecentTransactionsResponse
import com.babylon.wallet.android.data.gateway.generated.model.TransactionDetailsRequest
import com.babylon.wallet.android.data.gateway.generated.model.TransactionDetailsResponse
import com.babylon.wallet.android.data.gateway.generated.model.TransactionLookupIdentifier
import com.babylon.wallet.android.data.gateway.generated.model.TransactionStatusRequest
import com.babylon.wallet.android.data.gateway.generated.model.TransactionStatusResponse
import com.babylon.wallet.android.data.gateway.generated.model.TransactionSubmitRequest
import com.babylon.wallet.android.data.gateway.generated.model.TransactionSubmitResponse
import com.babylon.wallet.android.domain.Result
import com.babylon.wallet.android.domain.repository.performHttpRequest
import javax.inject.Inject

class TransactionRepositoryImpl @Inject constructor(private val gatewayApi: GatewayApi) : TransactionRepository {

    override suspend fun getRecentTransactions(address: String, page: String?, limit: Int?): Result<RecentTransactionsResponse> {
        return performHttpRequest(call = {
            gatewayApi.recentTransactions(RecentTransactionsRequest(cursor = page, limit = limit))
        }, map = {
            it
        })
    }

    override suspend fun submitTransaction(notarizedTransaction: String): Result<TransactionSubmitResponse> {
        return performHttpRequest(call = {
            gatewayApi.submitTransaction(TransactionSubmitRequest(notarizedTransaction))
        }, map = {
            it
        })
    }

    override suspend fun getTransactionStatus(identifier: TransactionLookupIdentifier): Result<TransactionStatusResponse> {
        return performHttpRequest(call = {
            gatewayApi.transactionStatus(TransactionStatusRequest(identifier))
        }, map = {
            it
        })
    }

    override suspend fun getTransactionDetails(identifier: TransactionLookupIdentifier): Result<TransactionDetailsResponse> {
        return performHttpRequest(call = {
            gatewayApi.transactionDetails(TransactionDetailsRequest(identifier))
        }, map = {
            it
        })
    }

}