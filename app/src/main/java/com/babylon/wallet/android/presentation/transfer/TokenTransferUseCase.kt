package com.babylon.wallet.android.presentation.transfer

import com.babylon.wallet.android.data.manifest.addDepositBatchInstruction
import com.babylon.wallet.android.data.manifest.addLockFeeInstruction
import com.babylon.wallet.android.data.manifest.addWithdrawInstruction
import com.babylon.wallet.android.data.repository.transaction.TransactionRepository
import com.babylon.wallet.android.data.transaction.TransactionClient
import com.babylon.wallet.android.di.coroutines.IoDispatcher
import com.babylon.wallet.android.domain.common.Result
import com.babylon.wallet.android.domain.model.KnownAddresses
import com.radixdlt.toolkit.builders.ManifestBuilder
import com.radixdlt.toolkit.models.transaction.TransactionManifest
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import rdx.works.profile.data.repository.ProfileDataSource
import javax.inject.Inject

class TokenTransferUseCase @Inject constructor(
    private val transactionClient: TransactionClient,
    private val transactionRepository: TransactionRepository,
    private val profileDataSource: ProfileDataSource,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {

    suspend operator fun invoke(
        senderAddress: String,
        recipientAddress: String,
        tokenAmount: String
    ): Result<String> {
        return withContext(ioDispatcher) {
            val networkId = profileDataSource.getCurrentNetworkId()
            val knownAddresses = KnownAddresses.addressMap[networkId]
            if (knownAddresses != null) {
                val manifest = buildManifest(knownAddresses, senderAddress, recipientAddress, tokenAmount)
                when (val epochResult = transactionRepository.getLedgerEpoch()) {
                    is Result.Error -> epochResult
                    is Result.Success -> {
                        transactionClient.signAndSubmitTransaction(manifest, true)
                    }
                }
            } else {
                Result.Error()
            }
        }
    }

    private fun buildManifest(
        knownAddresses: KnownAddresses,
        senderAccount: String,
        recipientAccount: String,
        tokenAmount: String
    ): TransactionManifest {
        return ManifestBuilder()
            .addLockFeeInstruction(knownAddresses.faucetAddress)
            .addWithdrawInstruction(
                withdrawComponentAddress = senderAccount,
                tokenResourceAddress = knownAddresses.xrdAddress,
                amount = tokenAmount
            )
            .addDepositBatchInstruction(recipientAccount)
            .build()
    }
}
