package com.babylon.wallet.android.domain.usecases

import com.babylon.wallet.android.data.PreferencesManager
import com.babylon.wallet.android.data.manifest.addDepositBatchInstruction
import com.babylon.wallet.android.data.manifest.addFreeXrdInstruction
import com.babylon.wallet.android.data.manifest.addLockFeeInstruction
import com.babylon.wallet.android.data.repository.transaction.TransactionRepository
import com.babylon.wallet.android.data.transaction.TransactionClient
import com.babylon.wallet.android.di.coroutines.IoDispatcher
import com.babylon.wallet.android.domain.common.Result
import com.babylon.wallet.android.domain.model.KnownAddresses
import com.radixdlt.toolkit.builders.ManifestBuilder
import com.radixdlt.toolkit.models.transaction.TransactionManifest
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import rdx.works.profile.data.repository.ProfileDataSource
import javax.inject.Inject

class GetFreeXrdUseCase @Inject constructor(
    private val transactionClient: TransactionClient,
    private val transactionRepository: TransactionRepository,
    private val profileDataSource: ProfileDataSource,
    private val preferencesManager: PreferencesManager,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {

    suspend operator fun invoke(includeLockFeeInstruction: Boolean, address: String): Result<String> {
        return withContext(ioDispatcher) {
            val networkId = profileDataSource.getCurrentNetworkId()
            val knownAddresses = KnownAddresses.addressMap[networkId]
            if (knownAddresses != null) {
                val manifest = buildFaucetManifest(knownAddresses, address, includeLockFeeInstruction)
                when (val epochResult = transactionRepository.getLedgerEpoch()) {
                    is Result.Error -> epochResult
                    is Result.Success -> {
                        val submitResult = transactionClient.signAndSubmitTransaction(manifest, true)
                        if (submitResult is Result.Success) {
                            preferencesManager.updateEpoch(address, epochResult.data)
                        }
                        submitResult
                    }
                }
            } else {
                Result.Error()
            }
        }
    }

    private fun buildFaucetManifest(
        knownAddresses: KnownAddresses,
        address: String,
        includeLockFeeInstruction: Boolean,
    ): TransactionManifest {
        val manifestBuilder = ManifestBuilder()
        if (includeLockFeeInstruction) {
            manifestBuilder.addLockFeeInstruction(knownAddresses.faucetAddress)
        }
        manifestBuilder.addFreeXrdInstruction(knownAddresses.faucetAddress)
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
                        if (currentEpoch.data < lastUsedEpoch) {
                            false
                        } else {
                            val threshold = 1
                            currentEpoch.data - lastUsedEpoch >= threshold
                        }
                    }
                }
            }
        }
    }
}
