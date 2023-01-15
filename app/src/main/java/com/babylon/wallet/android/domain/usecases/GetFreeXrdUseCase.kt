package com.babylon.wallet.android.domain.usecases

import com.babylon.wallet.android.data.PreferencesManager
import com.babylon.wallet.android.data.repository.transaction.TransactionRepository
import com.babylon.wallet.android.data.transaction.MethodName
import com.babylon.wallet.android.data.transaction.TransactionClient
import com.babylon.wallet.android.di.coroutines.IoDispatcher
import com.babylon.wallet.android.domain.common.Result
import com.babylon.wallet.android.domain.model.KnownAddresses
import com.radixdlt.toolkit.builders.ManifestBuilder
import com.radixdlt.toolkit.models.CallMethodReceiver
import com.radixdlt.toolkit.models.Instruction
import com.radixdlt.toolkit.models.Value
import com.radixdlt.toolkit.models.transaction.TransactionManifest
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import rdx.works.profile.data.repository.NetworkRepository
import javax.inject.Inject

class GetFreeXrdUseCase @Inject constructor(
    private val transactionClient: TransactionClient,
    private val transactionRepository: TransactionRepository,
    private val networkRepository: NetworkRepository,
    private val preferencesManager: PreferencesManager,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {

    suspend operator fun invoke(includeLockFeeInstruction: Boolean, address: String): Result<String> {
        return withContext(ioDispatcher) {
            val networkId = networkRepository.getCurrentNetworkId()
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
        var manifest = ManifestBuilder().addInstruction(
            Instruction.CallMethod(
                componentAddress = CallMethodReceiver.ComponentAddress(
                    Value.ComponentAddress(knownAddresses.faucetAddress)
                ),
                methodName = Value.String(MethodName.Free.stringValue)
            )
        ).addInstruction(
            Instruction.CallMethod(
                componentAddress = CallMethodReceiver.ComponentAddress(
                    Value.ComponentAddress(address)
                ),
                methodName = Value.String(MethodName.DepositBatch.stringValue),
                arrayOf(Value.Expression("ENTIRE_WORKTOP"))
            )
        ).build()
        if (includeLockFeeInstruction) {
            manifest = transactionClient.addLockFeeInstructionToManifest(manifest, knownAddresses.faucetAddress)
        }
        return manifest
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
