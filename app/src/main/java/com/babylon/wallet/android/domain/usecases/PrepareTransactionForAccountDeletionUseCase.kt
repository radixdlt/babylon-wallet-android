package com.babylon.wallet.android.domain.usecases

import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import com.babylon.wallet.android.data.dapp.model.TransactionType
import com.babylon.wallet.android.domain.model.transaction.UnvalidatedManifestData
import com.babylon.wallet.android.domain.model.transaction.prepareInternalTransactionRequest
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.NetworkId
import com.radixdlt.sargon.TransactionManifest
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.string
import javax.inject.Inject
import kotlin.random.Random

class PrepareTransactionForAccountDeletionUseCase @Inject constructor(
    private val incomingRequestRepository: IncomingRequestRepository
) {

    suspend operator fun invoke(
        deletingAccountAddress: AccountAddress,
        accountAddressToTransferResources: AccountAddress? = null
    ): Result<Unit> {
        val instructions = if (accountAddressToTransferResources != null) {
            """
                CALL_METHOD
                    Address("${deletingAccountAddress.string}")
                    "withdraw"
                    Address("resource_tdx_2_1tknxxxxxxxxxradxrdxxxxxxxxx009923554798xxxxxxxxxtfd2jc")
                    Decimal("2")
                ;
                TAKE_FROM_WORKTOP
                    Address("resource_tdx_2_1tknxxxxxxxxxradxrdxxxxxxxxx009923554798xxxxxxxxxtfd2jc")
                    Decimal("2")
                    Bucket("bucket1")
                ;
                CALL_METHOD
                    Address("${accountAddressToTransferResources.string}")
                    "try_deposit_or_abort"
                    Bucket("bucket1")
                    Enum<0u8>()
                ;

            """.trimIndent()
        } else {
            """
                SET_METADATA
                    Address("${deletingAccountAddress.string}")
                    "random"
                    Enum<0u8>(
                        "${Random.nextInt(until = 10000)}"
                    )
                ;
            """.trimIndent()
        }

        val manifest = TransactionManifest.init(
            networkId = NetworkId.STOKENET,
            instructionsString = instructions
        )
        val transactionRequest = UnvalidatedManifestData.from(manifest).prepareInternalTransactionRequest(
            blockUntilCompleted = true,
            transactionType = TransactionType.DeleteAccount(deletingAccountAddress)
        )

        incomingRequestRepository.add(transactionRequest)
        return Result.success(Unit)
    }

}