package com.babylon.wallet.android.data.repository.transaction

import com.babylon.wallet.android.data.gateway.apis.TransactionApi
import com.babylon.wallet.android.data.gateway.generated.models.AccountDepositPreValidationRequest
import com.babylon.wallet.android.data.repository.toResult
import com.babylon.wallet.android.di.coroutines.IoDispatcher
import com.babylon.wallet.android.domain.RadixWalletException
import com.babylon.wallet.android.domain.RadixWalletException.TransactionSubmitException
import com.babylon.wallet.android.domain.model.AccountDepositResourceRules
import com.babylon.wallet.android.domain.model.transaction.TransactionToReviewData
import com.babylon.wallet.android.domain.model.transaction.UnvalidatedManifestData
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.Blob
import com.radixdlt.sargon.Blobs
import com.radixdlt.sargon.CommonException
import com.radixdlt.sargon.Epoch
import com.radixdlt.sargon.Nonce
import com.radixdlt.sargon.NotarizedTransaction
import com.radixdlt.sargon.PublicKey
import com.radixdlt.sargon.ResourceAddress
import com.radixdlt.sargon.TransactionIntentHash
import com.radixdlt.sargon.TransactionStatus
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.mapError
import com.radixdlt.sargon.extensions.secureRandom
import com.radixdlt.sargon.extensions.string
import com.radixdlt.sargon.os.SargonOsManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject

interface TransactionRepository {

    suspend fun analyzeTransaction(
        manifestData: UnvalidatedManifestData,
        isWalletTransaction: Boolean,
        notaryPublicKey: PublicKey.Ed25519
    ): Result<TransactionToReviewData>

    suspend fun submitTransaction(notarizedTransaction: NotarizedTransaction): Result<TransactionIntentHash>

    suspend fun pollTransactionStatus(intentHash: TransactionIntentHash): TransactionStatus

    suspend fun getLedgerEpoch(): Result<Epoch>

    suspend fun getAccountDepositPreValidation(
        accountAddress: AccountAddress,
        resourceAddress: List<ResourceAddress>
    ): Result<AccountDepositResourceRules>
}

// TODO translate from network models to domain models
class TransactionRepositoryImpl @Inject constructor(
    private val transactionApi: TransactionApi,
    private val sargonOsManager: SargonOsManager,
    @IoDispatcher private val dispatcher: CoroutineDispatcher
) : TransactionRepository {

    override suspend fun analyzeTransaction(
        manifestData: UnvalidatedManifestData,
        isWalletTransaction: Boolean,
        notaryPublicKey: PublicKey.Ed25519
    ): Result<TransactionToReviewData> {
        return withContext(dispatcher) {
            runCatching {
                val sargonOs = sargonOsManager.sargonOs
                val message = manifestData.message
                val transactionToReview = sargonOs.analyseTransactionPreview(
                    instructions = manifestData.instructions,
                    blobs = Blobs.init(blobs = manifestData.blobs.map { Blob.init(it) }),
                    message = message,
                    areInstructionsOriginatingFromHost = isWalletTransaction,
                    nonce = Nonce.secureRandom(),
                    notaryPublicKey = notaryPublicKey,
                )

                TransactionToReviewData(
                    transactionToReview = transactionToReview,
                    message = message
                )
            }
        }
    }

    override suspend fun getLedgerEpoch(): Result<Epoch> {
        return transactionApi.transactionConstruction().toResult().map { it.ledgerState.epoch.toULong() }
    }

    override suspend fun submitTransaction(notarizedTransaction: NotarizedTransaction): Result<TransactionIntentHash> {
        return withContext(dispatcher) {
            runCatching {
                val sargonOs = sargonOsManager.sargonOs
                sargonOs.submitTransaction(notarizedTransaction)
            }.mapError { error ->
                if (error is CommonException.GatewaySubmitDuplicateTx) {
                    TransactionSubmitException.InvalidTXDuplicate(error.intentHash)
                } else {
                    RadixWalletException.PrepareTransactionException.SubmitNotarizedTransaction(error)
                }
            }
        }
    }

    override suspend fun pollTransactionStatus(intentHash: TransactionIntentHash): TransactionStatus {
        return withContext(dispatcher) {
            val sargonOs = sargonOsManager.sargonOs
            sargonOs.pollTransactionStatus(intentHash)
        }
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
