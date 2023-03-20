package com.babylon.wallet.android.domain.usecases

import com.babylon.wallet.android.data.PreferencesManager
import com.babylon.wallet.android.data.repository.transaction.TransactionRepository
import com.babylon.wallet.android.data.transaction.MethodName
import com.babylon.wallet.android.data.transaction.TransactionClient
import com.babylon.wallet.android.di.coroutines.IoDispatcher
import com.babylon.wallet.android.domain.common.Result
import com.babylon.wallet.android.domain.common.onValue
import com.radixdlt.toolkit.builders.ManifestBuilder
import com.radixdlt.toolkit.models.Instruction
import com.radixdlt.toolkit.models.Value
import com.radixdlt.toolkit.models.transaction.TransactionManifest
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import rdx.works.profile.data.repository.ProfileDataSource
import rdx.works.profile.derivation.model.NetworkId
import javax.inject.Inject

class GetFreeXrdUseCase @Inject constructor(
    private val transactionClient: TransactionClient,
    private val transactionRepository: TransactionRepository,
    private val profileDataSource: ProfileDataSource,
    private val preferencesManager: PreferencesManager,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {

    suspend operator fun invoke(
        includeLockFeeInstruction: Boolean,
        address: String
    ): Result<String> {
        return withContext(ioDispatcher) {
            val manifest = buildFaucetManifest(
                networkId = profileDataSource.getCurrentNetworkId(),
                address = address,
                includeLockFeeInstruction = includeLockFeeInstruction
            )
            when (val epochResult = transactionRepository.getLedgerEpoch()) {
                is Result.Error -> epochResult
                is Result.Success -> {
                    val submitResult = transactionClient.signAndSubmitTransaction(manifest, true)
                    submitResult.onValue { txId ->
                        val transactionStatus = transactionClient.pollTransactionStatus(txId)
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
        includeLockFeeInstruction: Boolean,
    ): TransactionManifest {
        var manifest = ManifestBuilder()
            .addInstruction(
                Instruction.CallMethod(
                    componentAddress = Value.ComponentAddress.faucetComponentAddress(
                        networkId = networkId.value.toUByte()
                    ),
                    methodName = Value.String(MethodName.Free.stringValue)
                )
            ).addInstruction(
                Instruction.CallMethod(
                    componentAddress = Value.ComponentAddress(
                        address = address
                    ),
                    methodName = Value.String(MethodName.DepositBatch.stringValue),
                    arguments = arrayOf(Value.Expression("ENTIRE_WORKTOP"))
                )
            )
            .build()

        if (includeLockFeeInstruction) {
            manifest = transactionClient.addLockFeeInstructionToManifest(
                manifest = manifest,
                addressToLockFee = Value.ComponentAddress.faucetComponentAddress(
                    networkId = networkId.value.toUByte()
                ).address.componentAddress
            )
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
