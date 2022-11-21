package com.babylon.wallet.android.data.repository.transaction

import com.babylon.wallet.android.data.gateway.generated.model.RecentTransactionsResponse
import com.babylon.wallet.android.data.gateway.generated.model.TransactionDetailsResponse
import com.babylon.wallet.android.data.gateway.generated.model.TransactionLookupIdentifier
import com.babylon.wallet.android.data.gateway.generated.model.TransactionStatusResponse
import com.babylon.wallet.android.data.gateway.generated.model.TransactionSubmitResponse
import com.babylon.wallet.android.domain.Result

// TODO translate from network models to domain models
interface TransactionRepository {
    suspend fun getRecentTransactions(address: String, page: String?, limit: Int?): Result<RecentTransactionsResponse>
    suspend fun submitTransaction(notarizedTransaction: String): Result<TransactionSubmitResponse>
    suspend fun getTransactionStatus(identifier: TransactionLookupIdentifier): Result<TransactionStatusResponse>
    suspend fun getTransactionDetails(identifier: TransactionLookupIdentifier): Result<TransactionDetailsResponse>
}
