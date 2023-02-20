package com.babylon.wallet.android.presentation.transfer

import com.babylon.wallet.android.data.manifest.addDepositBatchInstruction
import com.babylon.wallet.android.data.manifest.addLockFeeInstruction
import com.babylon.wallet.android.data.manifest.addWithdrawInstruction
import com.babylon.wallet.android.data.repository.transaction.TransactionRepository
import com.babylon.wallet.android.data.transaction.TransactionClient
import com.babylon.wallet.android.di.coroutines.IoDispatcher
import com.babylon.wallet.android.domain.common.Result
import com.radixdlt.toolkit.builders.ManifestBuilder
import com.radixdlt.toolkit.models.Value
import com.radixdlt.toolkit.models.transaction.TransactionManifest
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import rdx.works.profile.data.repository.ProfileDataSource
import rdx.works.profile.derivation.model.NetworkId
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
            val manifest = buildManifest(networkId, senderAddress, recipientAddress, tokenAmount)
            when (val epochResult = transactionRepository.getLedgerEpoch()) {
                is Result.Error -> epochResult
                is Result.Success -> {
                    transactionClient.signAndSubmitTransaction(manifest, true)
                }
            }
        }
    }

    private fun buildManifest(
        networkId: NetworkId,
        senderAccount: String,
        recipientAccount: String,
        tokenAmount: String
    ): TransactionManifest {
        return ManifestBuilder()
            .addLockFeeInstruction(
                addressToLockFee = Value.ComponentAddress.faucetComponentAddress(
                    networkId = networkId.value.toUByte()
                ).address.componentAddress
            )
            .addWithdrawInstruction(
                withdrawComponentAddress = senderAccount,
                tokenResourceAddress = Value.ResourceAddress.xrdResourceAddress(
                    networkId = networkId.value.toUByte()
                ).address.resourceAddress,
                amount = tokenAmount
            )
            .addDepositBatchInstruction(recipientAccount)
            .build()
    }
}
