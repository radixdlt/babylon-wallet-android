package com.babylon.wallet.android.data.repository.transaction

import com.babylon.wallet.android.data.gateway.GatewayApi
import com.babylon.wallet.android.data.gateway.generated.model.TransactionRecentRequest
import com.babylon.wallet.android.data.gateway.generated.model.TransactionRecentResponse
import com.babylon.wallet.android.data.gateway.generated.model.TransactionStatusRequest
import com.babylon.wallet.android.data.gateway.generated.model.TransactionStatusResponse
import com.babylon.wallet.android.data.gateway.generated.model.TransactionSubmitRequest
import com.babylon.wallet.android.data.gateway.generated.model.TransactionSubmitResponse
import com.babylon.wallet.android.data.repository.execute
import com.babylon.wallet.android.domain.common.Result
import javax.inject.Inject

// TODO translate from network models to domain models
interface TransactionRepository {

    suspend fun getRecentTransactions(address: String, page: String?, limit: Int?): Result<TransactionRecentResponse>

    suspend fun submitTransaction(notarizedTransaction: String): Result<TransactionSubmitResponse>

    suspend fun getTransactionStatus(identifier: String?): Result<TransactionStatusResponse>

    suspend fun getLedgerEpoch(): Result<Long>
}

// TODO translate from network models to domain models
class TransactionRepositoryImpl @Inject constructor(private val gatewayApi: GatewayApi) : TransactionRepository {

    override suspend fun getLedgerEpoch(): Result<Long> {
        return gatewayApi.transactionConstruction().execute(map = { it.ledgerState.epoch })
    }

    override suspend fun getRecentTransactions(
        address: String,
        page: String?,
        limit: Int?
    ): Result<TransactionRecentResponse> {
        return gatewayApi.transactionRecent(TransactionRecentRequest(cursor = page, limit = limit))
            .execute(map = { it })
    }

    override suspend fun submitTransaction(notarizedTransaction: String): Result<TransactionSubmitResponse> {
        return gatewayApi.submitTransaction(TransactionSubmitRequest(notarizedTransaction))
            .execute(map = { it })
    }

    override suspend fun getTransactionStatus(identifier: String?): Result<TransactionStatusResponse> {
        return gatewayApi.transactionStatus(TransactionStatusRequest(intentHashHex = identifier))
            .execute(map = { it })
    }
}
