package com.babylon.wallet.android.domain.repository.transaction

import com.babylon.wallet.android.data.gateway.generated.model.*
import com.babylon.wallet.android.domain.Result

interface TransactionRepository {
    suspend fun getRecentTransactions(address: String, page: String?, limit: Int?): Result<RecentTransactionsResponse>
    suspend fun submitTransaction(notarizedTransaction: String): Result<TransactionSubmitResponse>
    suspend fun getTransactionStatus(identifier: TransactionLookupIdentifier): Result<TransactionStatusResponse>
    suspend fun getTransactionDetails(identifier: TransactionLookupIdentifier): Result<TransactionDetailsResponse>
}