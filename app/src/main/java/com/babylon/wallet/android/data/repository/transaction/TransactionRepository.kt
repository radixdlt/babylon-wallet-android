package com.babylon.wallet.android.data.repository.transaction

import com.babylon.wallet.android.data.gateway.apis.TransactionApi
import com.babylon.wallet.android.data.gateway.generated.models.TransactionPreviewRequest
import com.babylon.wallet.android.data.gateway.generated.models.TransactionPreviewResponse
import com.babylon.wallet.android.data.gateway.generated.models.TransactionStatusRequest
import com.babylon.wallet.android.data.gateway.generated.models.TransactionStatusResponse
import com.babylon.wallet.android.data.gateway.generated.models.TransactionSubmitRequest
import com.babylon.wallet.android.data.gateway.generated.models.TransactionSubmitResponse
import com.babylon.wallet.android.data.repository.toResult
import javax.inject.Inject

// TODO translate from network models to domain models
interface TransactionRepository {

    suspend fun submitTransaction(notarizedTransaction: String): Result<TransactionSubmitResponse>

    suspend fun getTransactionStatus(identifier: String): Result<TransactionStatusResponse>

    suspend fun getLedgerEpoch(): Result<Long>

    suspend fun getTransactionPreview(body: TransactionPreviewRequest): Result<TransactionPreviewResponse>
}

// TODO translate from network models to domain models
class TransactionRepositoryImpl @Inject constructor(private val transactionApi: TransactionApi) : TransactionRepository {

    override suspend fun getLedgerEpoch(): Result<Long> {
        return transactionApi.transactionConstruction().toResult().map { it.ledgerState.epoch }
    }

    override suspend fun submitTransaction(notarizedTransaction: String): Result<TransactionSubmitResponse> {
        return transactionApi.transactionSubmit(TransactionSubmitRequest(notarizedTransaction)).toResult()
    }

    override suspend fun getTransactionStatus(identifier: String): Result<TransactionStatusResponse> {
        return transactionApi.transactionStatus(TransactionStatusRequest(intentHash = identifier)).toResult()
    }

    override suspend fun getTransactionPreview(body: TransactionPreviewRequest): Result<TransactionPreviewResponse> {
        return transactionApi.transactionPreview(body).toResult()
    }
}
