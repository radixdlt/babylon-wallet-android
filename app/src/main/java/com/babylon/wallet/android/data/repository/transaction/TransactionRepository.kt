package com.babylon.wallet.android.data.repository.transaction

import com.babylon.wallet.android.data.gateway.generated.model.TransactionRecentResponse
import com.babylon.wallet.android.data.gateway.generated.model.TransactionStatusResponse
import com.babylon.wallet.android.data.gateway.generated.model.TransactionSubmitResponse
import com.babylon.wallet.android.domain.common.Result

// TODO translate from network models to domain models
interface TransactionRepository {

    suspend fun getRecentTransactions(address: String, page: String?, limit: Int?): Result<TransactionRecentResponse>

    suspend fun submitTransaction(notarizedTransaction: String): Result<TransactionSubmitResponse>

    suspend fun getTransactionStatus(identifier: String?): Result<TransactionStatusResponse>

    suspend fun getLedgerEpoch(): Result<Long>
}
