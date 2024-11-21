package com.babylon.wallet.android.domain.usecases

import com.babylon.wallet.android.data.dapp.model.TransactionType
import com.babylon.wallet.android.di.coroutines.IoDispatcher
import com.babylon.wallet.android.domain.model.messages.TransactionRequest
import com.babylon.wallet.android.domain.model.transaction.UnvalidatedManifestData
import com.babylon.wallet.android.domain.usecases.interaction.PrepareInternalTransactionUseCase
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.os.SargonOsManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject

class PrepareTransactionForAccountDeletionUseCase @Inject constructor(
    private val sargonOsManager: SargonOsManager,
    private val prepareInternalTransactionUseCase: PrepareInternalTransactionUseCase,
    @IoDispatcher private val coroutineDispatcher: CoroutineDispatcher
) {

    suspend operator fun invoke(
        deletingAccountAddress: AccountAddress,
        accountAddressToTransferResources: AccountAddress? = null
    ): Result<Outcome> = withContext(coroutineDispatcher) {
        runCatching {
            val outcome = sargonOsManager.sargonOs.createDeleteAccountManifest(
                accountAddress = deletingAccountAddress,
                recipientAccountAddress = accountAddressToTransferResources
            )
            val transactionRequest = prepareInternalTransactionUseCase(
                unvalidatedManifestData = UnvalidatedManifestData.from(outcome.manifest),
                blockUntilCompleted = true,
                transactionType = TransactionType.DeleteAccount(deletingAccountAddress)
            )

            Outcome(
                transactionRequest = transactionRequest,
                hasNonTransferableResources = outcome.nonTransferableResources.isNotEmpty()
            )
        }
    }

    data class Outcome(
        val transactionRequest: TransactionRequest,
        val hasNonTransferableResources: Boolean
    )
}
