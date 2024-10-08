package com.babylon.wallet.android.data.repository.transaction

import com.babylon.wallet.android.data.gateway.apis.TransactionApi
import com.babylon.wallet.android.data.gateway.generated.models.AccountDepositPreValidationRequest
import com.babylon.wallet.android.data.gateway.generated.models.TransactionPreviewRequest
import com.babylon.wallet.android.data.gateway.generated.models.TransactionPreviewResponse
import com.babylon.wallet.android.data.gateway.generated.models.TransactionStatusRequest
import com.babylon.wallet.android.data.gateway.generated.models.TransactionStatusResponse
import com.babylon.wallet.android.data.gateway.generated.models.TransactionSubmitRequest
import com.babylon.wallet.android.data.gateway.generated.models.TransactionSubmitResponse
import com.babylon.wallet.android.data.repository.toResult
import com.babylon.wallet.android.domain.model.AccountDepositResourceRules
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.Epoch
import com.radixdlt.sargon.ResourceAddress
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.string
import javax.inject.Inject

// TODO translate from network models to domain models
interface TransactionRepository {

    suspend fun submitTransaction(notarizedTransaction: String): Result<TransactionSubmitResponse>

    suspend fun getTransactionStatus(identifier: String): Result<TransactionStatusResponse>

    suspend fun getLedgerEpoch(): Result<Epoch>

    suspend fun getTransactionPreview(body: TransactionPreviewRequest): Result<TransactionPreviewResponse>

    suspend fun getAccountDepositPreValidation(
        accountAddress: AccountAddress,
        resourceAddress: List<ResourceAddress>
    ): Result<AccountDepositResourceRules>
}

// TODO translate from network models to domain models
class TransactionRepositoryImpl @Inject constructor(private val transactionApi: TransactionApi) : TransactionRepository {

    override suspend fun getLedgerEpoch(): Result<Epoch> {
        return transactionApi.transactionConstruction().toResult().map { it.ledgerState.epoch.toULong() }
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

    override suspend fun getAccountDepositPreValidation(
        accountAddress: AccountAddress,
        resourceAddress: List<ResourceAddress>
    ): Result<AccountDepositResourceRules> {
        return transactionApi.accountDepositPreValidation(
            AccountDepositPreValidationRequest(
                accountAddress = accountAddress.string,
                resourceAddresses = resourceAddress.map { it.string }
            )
        ).toResult()
            .map { response ->
                AccountDepositResourceRules(
                    canDepositAll = response.allowsTryDepositBatch,
                    accountAddress = accountAddress,
                    resourceRules = response.resourceSpecificBehaviour?.map { behavior ->
                        AccountDepositResourceRules.ResourceDepositRule(
                            resourceAddress = ResourceAddress.init(behavior.resourceAddress),
                            isDepositAllowed = behavior.allowsTryDeposit
                        )
                    }.orEmpty().toSet()
                )
            }
    }
}
