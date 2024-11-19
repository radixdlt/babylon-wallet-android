package com.babylon.wallet.android.domain.usecases

import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import com.babylon.wallet.android.data.dapp.model.TransactionType
import com.babylon.wallet.android.domain.model.transaction.UnvalidatedManifestData
import com.babylon.wallet.android.domain.model.transaction.prepareInternalTransactionRequest
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.os.SargonOsManager
import javax.inject.Inject

class PrepareTransactionForAccountDeletionUseCase @Inject constructor(
    private val incomingRequestRepository: IncomingRequestRepository,
    private val sargonOsManager: SargonOsManager
) {

    suspend operator fun invoke(
        deletingAccountAddress: AccountAddress,
        accountAddressToTransferResources: AccountAddress? = null
    ): Result<Unit> = runCatching {
        val manifest = sargonOsManager.sargonOs.createDeleteAccountManifest(
            accountAddress = deletingAccountAddress,
            recipientAccountAddress = accountAddressToTransferResources
        )
        val transactionRequest = UnvalidatedManifestData.from(manifest).prepareInternalTransactionRequest(
            blockUntilCompleted = true,
            transactionType = TransactionType.DeleteAccount(deletingAccountAddress)
        )

        incomingRequestRepository.add(transactionRequest)
        return Result.success(Unit)
    }
}
