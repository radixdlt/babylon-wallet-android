package com.babylon.wallet.android.domain.usecases

import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import com.babylon.wallet.android.data.dapp.model.TransactionType
import com.babylon.wallet.android.domain.model.transaction.UnvalidatedManifestData
import com.babylon.wallet.android.domain.model.transaction.prepareInternalTransactionRequest
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.TransactionManifest
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.samples.sample
import javax.inject.Inject

class PrepareTransactionForAccountDeletionUseCase @Inject constructor(
    private val incomingRequestRepository: IncomingRequestRepository
) {

    @OptIn(UsesSampleValues::class)
    suspend operator fun invoke(
        deletingAccountAddress: AccountAddress,
        accountAddressToTransferResources: AccountAddress? = null
    ): Result<Unit> {
        val manifest = TransactionManifest.sample()
        val transactionRequest = UnvalidatedManifestData.from(manifest).prepareInternalTransactionRequest(
            blockUntilCompleted = true,
            transactionType = TransactionType.DeleteAccount
        )

        incomingRequestRepository.add(transactionRequest)
        return Result.success(Unit)
    }

}