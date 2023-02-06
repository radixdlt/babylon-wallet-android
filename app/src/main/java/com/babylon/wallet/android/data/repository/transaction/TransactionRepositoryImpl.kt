package com.babylon.wallet.android.data.repository.transaction

import com.babylon.wallet.android.data.gateway.GatewayApi
import com.babylon.wallet.android.data.gateway.generated.model.TransactionDetailsRequest
import com.babylon.wallet.android.data.gateway.generated.model.TransactionDetailsResponse
import com.babylon.wallet.android.data.gateway.generated.model.TransactionLookupIdentifier
import com.babylon.wallet.android.data.gateway.generated.model.TransactionRecentRequest
import com.babylon.wallet.android.data.gateway.generated.model.TransactionRecentResponse
import com.babylon.wallet.android.data.gateway.generated.model.TransactionStatusRequest
import com.babylon.wallet.android.data.gateway.generated.model.TransactionStatusResponse
import com.babylon.wallet.android.data.gateway.generated.model.TransactionSubmitRequest
import com.babylon.wallet.android.data.gateway.generated.model.TransactionSubmitResponse
import com.babylon.wallet.android.data.repository.performHttpRequest
import com.babylon.wallet.android.domain.common.Result
import javax.inject.Inject

// TODO translate from network models to domain models
class TransactionRepositoryImpl @Inject constructor(private val gatewayApi: GatewayApi) : TransactionRepository {

    override suspend fun getLedgerEpoch(): Result<Long> {
        return performHttpRequest(
            call = {
                gatewayApi.transactionConstruction()
            },
            map = {
                it.ledgerState.epoch
            }
        )
    }

    override suspend fun getRecentTransactions(
        address: String,
        page: String?,
        limit: Int?
    ): Result<TransactionRecentResponse> {
        return performHttpRequest(
            call = {
                gatewayApi.transactionRecent(TransactionRecentRequest(cursor = page, limit = limit))
            },
            map = {
                it
            }
        )
    }

    override suspend fun submitTransaction(notarizedTransaction: String): Result<TransactionSubmitResponse> {
        return performHttpRequest(
            call = {
                gatewayApi.submitTransaction(TransactionSubmitRequest(notarizedTransaction))
            },
            map = {
                it
            }
        )
    }

    override suspend fun getTransactionStatus(intentHashHex: String?): Result<TransactionStatusResponse> {
        return performHttpRequest(
            call = {
                gatewayApi.transactionStatus(TransactionStatusRequest(intentHashHex = intentHashHex))
            },
            map = {
                it
            }
        )
    }

    override suspend fun getTransactionDetails(
        identifier: TransactionLookupIdentifier
    ): Result<TransactionDetailsResponse> {
        return performHttpRequest(
            call = {
                gatewayApi.transactionDetails(TransactionDetailsRequest(identifier))
            },
            map = {
                it
            }
        )
    }
}
