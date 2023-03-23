package com.babylon.wallet.android.data.repository.transaction

import com.babylon.wallet.android.data.gateway.apis.TransactionApi
import com.babylon.wallet.android.data.gateway.generated.models.*
import com.babylon.wallet.android.data.repository.execute
import com.babylon.wallet.android.domain.common.Result
import javax.inject.Inject

// TODO translate from network models to domain models
interface TransactionRepository {

    suspend fun getRecentTransactions(address: String, page: String?, limit: Int?): Result<TransactionRecentResponse>

    suspend fun submitTransaction(notarizedTransaction: String): Result<TransactionSubmitResponse>

    suspend fun getTransactionStatus(identifier: String): Result<TransactionStatusResponse>

    suspend fun getLedgerEpoch(): Result<Long>
}

// TODO translate from network models to domain models
class TransactionRepositoryImpl @Inject constructor(private val transactionApi: TransactionApi) : TransactionRepository {

    override suspend fun getLedgerEpoch(): Result<Long> {
        return transactionApi.transactionConstruction().execute(map = { it.ledgerState.epoch })
    }

    override suspend fun getRecentTransactions(
        address: String,
        page: String?,
        limit: Int?
    ): Result<TransactionRecentResponse> {
        return transactionApi.transactionRecent(TransactionRecentRequest(cursor = page, limit = limit))
            .execute(map = { it })
    }

    override suspend fun submitTransaction(notarizedTransaction: String): Result<TransactionSubmitResponse> {
        return transactionApi.submitTransaction(TransactionSubmitRequest(notarizedTransaction))
            .execute(map = { it })
    }

    override suspend fun getTransactionStatus(identifier: String): Result<TransactionStatusResponse> {
        return transactionApi.transactionStatus(TransactionStatusRequest(intentHashHex = identifier))
            .execute(map = { it })
    }
}
