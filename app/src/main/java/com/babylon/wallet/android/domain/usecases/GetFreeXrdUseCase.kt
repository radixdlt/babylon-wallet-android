package com.babylon.wallet.android.domain.usecases

import com.babylon.wallet.android.data.manifest.addDepositBatchInstruction
import com.babylon.wallet.android.data.manifest.addFreeXrdInstruction
import com.babylon.wallet.android.data.manifest.addLockFeeInstruction
import com.babylon.wallet.android.data.repository.networkinfo.NetworkInfoRepository
import com.babylon.wallet.android.data.repository.transaction.TransactionRepository
import com.babylon.wallet.android.data.transaction.TransactionApprovalRequest
import com.babylon.wallet.android.data.transaction.TransactionClient
import com.babylon.wallet.android.di.coroutines.IoDispatcher
import com.babylon.wallet.android.domain.common.Result
import com.babylon.wallet.android.domain.common.onValue
import com.babylon.wallet.android.domain.common.value
import com.babylon.wallet.android.domain.usecases.transaction.PollTransactionStatusUseCase
import com.radixdlt.toolkit.builders.ManifestBuilder
import com.radixdlt.toolkit.models.transaction.TransactionManifest
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import rdx.works.core.preferences.PreferencesManager
import rdx.works.profile.domain.gateway.GetCurrentGatewayUseCase
import timber.log.Timber
import javax.inject.Inject

@Suppress("LongParameterList")
class GetFreeXrdUseCase @Inject constructor(
    private val transactionClient: TransactionClient,
    private val transactionRepository: TransactionRepository,
    private val networkInfoRepository: NetworkInfoRepository,
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
            networkInfoRepository.getFaucetComponentAddress(getCurrentGatewayUseCase().url).value()?.let { faucetComponentAddress ->
                val manifest = buildFaucetManifest(
                    address = address,
                    includeLockFeeInstruction = includeLockFeeInstruction,
                    faucetComponentAddress = faucetComponentAddress
                )
                when (val epochResult = transactionRepository.getLedgerEpoch()) {
                    is Result.Error -> {
                        Timber.e("GetFreeXrdUseCase failed to get ledger epoch")
                        epochResult
                    }
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
            } ?: run {
                Result.Error(Throwable("Unable to fetch faucet address"))
            }
        }
    }

    private fun buildFaucetManifest(
        faucetComponentAddress: String,
        address: String,
        includeLockFeeInstruction: Boolean
    ): TransactionManifest {
        val manifestBuilder = ManifestBuilder()
        if (includeLockFeeInstruction) {
            manifestBuilder.addLockFeeInstruction(
                addressToLockFee = faucetComponentAddress
            )
        }
        manifestBuilder.addFreeXrdInstruction(faucetComponentAddress)
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
