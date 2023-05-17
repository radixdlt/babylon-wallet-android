package com.babylon.wallet.android.domain.usecases

import com.babylon.wallet.android.data.manifest.addDepositBatchInstruction
import com.babylon.wallet.android.data.manifest.addFreeXrdInstruction
import com.babylon.wallet.android.data.manifest.addLockFeeInstruction
import com.babylon.wallet.android.data.manifest.faucetComponentAddress
import com.babylon.wallet.android.data.repository.transaction.TransactionRepository
import com.babylon.wallet.android.data.transaction.TransactionApprovalRequest
import com.babylon.wallet.android.data.transaction.TransactionClient
import com.babylon.wallet.android.di.coroutines.IoDispatcher
import com.babylon.wallet.android.domain.common.Result
import com.babylon.wallet.android.domain.common.onValue
import com.babylon.wallet.android.domain.usecases.transaction.PollTransactionStatusUseCase
import com.radixdlt.toolkit.builders.ManifestBuilder
import com.radixdlt.toolkit.models.transaction.TransactionManifest
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import rdx.works.core.preferences.PreferencesManager
import rdx.works.profile.derivation.model.NetworkId
import rdx.works.profile.domain.gateway.GetCurrentGatewayUseCase
import javax.inject.Inject

class GetFreeXrdUseCase @Inject constructor(
    private val transactionClient: TransactionClient,
    private val transactionRepository: TransactionRepository,
    private val getCurrentGatewayUseCase: GetCurrentGatewayUseCase,
    private val preferencesManager: PreferencesManager,
    private val pollTransactionStatusUseCase: PollTransactionStatusUseCase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {

    suspend operator fun invoke(
        includeLockFeeInstruction: Boolean,
        address: String
    ): Result<String> {
        return withContext(ioDispatcher) {
            val manifest = buildFaucetManifest(
                networkId = getCurrentGatewayUseCase().network.networkId(),
                address = address,
                includeLockFeeInstruction = includeLockFeeInstruction
            )
            when (val epochResult = transactionRepository.getLedgerEpoch()) {
                is Result.Error -> epochResult
                is Result.Success -> {
                    val request = TransactionApprovalRequest(manifest = manifest, hasLockFee = true)
                    val submitResult = transactionClient.signAndSubmitTransaction(request)
                    submitResult.onValue { txId ->
                        val transactionStatus = pollTransactionStatusUseCase(txId)
                        transactionStatus.onValue {
                            preferencesManager.updateEpoch(address, epochResult.data)
                        }
                    }
                    submitResult
                }
            }
        }
    }

    private fun buildFaucetManifest(
        networkId: NetworkId,
        address: String,
        includeLockFeeInstruction: Boolean
    ): TransactionManifest {
        val manifestBuilder = ManifestBuilder()
        if (includeLockFeeInstruction) {
            manifestBuilder.addLockFeeInstruction(
                addressToLockFee = faucetComponentAddress(networkId.value.toUByte()).address
            )
        }
        manifestBuilder.addFreeXrdInstruction(networkId)
        manifestBuilder.addDepositBatchInstruction(address)

        return manifestBuilder.build()
    }

    fun isAllowedToUseFaucet(address: String): Flow<Boolean> {
        return preferencesManager.getLastUsedEpochFlow(address).map { lastUsedEpoch ->
            if (lastUsedEpoch == null) {
                true
            } else {
                when (val currentEpoch = transactionRepository.getLedgerEpoch()) {
                    is Result.Error -> false
                    is Result.Success -> {
                        when {
                            currentEpoch.data < lastUsedEpoch -> true // edge case ledger was reset - allow
                            else -> {
                                val threshold = 1
                                currentEpoch.data - lastUsedEpoch >= threshold
                            }
                        }
                    }
                }
            }
        }
    }
}
