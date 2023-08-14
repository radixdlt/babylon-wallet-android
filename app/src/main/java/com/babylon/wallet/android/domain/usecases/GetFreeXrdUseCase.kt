package com.babylon.wallet.android.domain.usecases

import com.babylon.wallet.android.data.repository.transaction.TransactionRepository
import com.babylon.wallet.android.data.transaction.DappRequestFailure
import com.babylon.wallet.android.data.transaction.TransactionClient
import com.babylon.wallet.android.data.transaction.TransactionConfig
import com.babylon.wallet.android.data.transaction.TransactionConfig.TIP_PERCENTAGE
import com.babylon.wallet.android.data.transaction.model.TransactionApprovalRequest
import com.babylon.wallet.android.di.coroutines.IoDispatcher
import com.babylon.wallet.android.domain.common.onValue
import com.babylon.wallet.android.domain.usecases.transaction.PollTransactionStatusUseCase
import com.radixdlt.ret.Address
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import rdx.works.core.preferences.PreferencesManager
import rdx.works.core.ret.BabylonManifestBuilder
import rdx.works.core.ret.buildSafely
import rdx.works.profile.domain.gateway.GetCurrentGatewayUseCase
import java.math.BigDecimal
import javax.inject.Inject
import kotlin.Result
import com.babylon.wallet.android.domain.common.Result as ResultInternal

@Suppress("LongParameterList")
class GetFreeXrdUseCase @Inject constructor(
    private val transactionClient: TransactionClient,
    private val transactionRepository: TransactionRepository,
    private val getCurrentGatewayUseCase: GetCurrentGatewayUseCase,
    private val preferencesManager: PreferencesManager,
    private val pollTransactionStatusUseCase: PollTransactionStatusUseCase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {

    suspend operator fun invoke(address: String): Result<String> {
        return withContext(ioDispatcher) {
            val gateway = getCurrentGatewayUseCase()
            val manifest = BabylonManifestBuilder()
                .lockFee()
                .freeXrd()
                .accountTryDepositBatchOrAbort(
                    toAddress = Address(address)
                )
                .buildSafely(gateway.network.id)
                .getOrElse {
                    return@withContext Result.failure(it)
                }
            when (val epochResult = transactionRepository.getLedgerEpoch()) {
                is ResultInternal.Error -> Result.failure(
                    exception = epochResult.exception ?: DappRequestFailure.TransactionApprovalFailure.PrepareNotarizedTransaction
                )

                is ResultInternal.Success -> {
                    val request = TransactionApprovalRequest(
                        manifest = manifest,
                        networkId = gateway.network.networkId(),
                        hasLockFee = true
                    )
                    val lockFee = BigDecimal.valueOf(TransactionConfig.DEFAULT_LOCK_FEE)
                    transactionClient.signAndSubmitTransaction(
                        request = request,
                        lockFee = lockFee,
                        tipPercentage = TIP_PERCENTAGE,
                        deviceBiometricAuthenticationProvider = { true }
                    ).onSuccess { txId ->
                        pollTransactionStatusUseCase(txId).onValue {
                            preferencesManager.updateEpoch(address, epochResult.data)
                        }
                    }
                }
            }
        }
    }

    fun isAllowedToUseFaucet(address: String): Flow<Boolean> {
        return preferencesManager.getLastUsedEpochFlow(address).map { lastUsedEpoch ->
            if (lastUsedEpoch == null) {
                true
            } else {
                when (val currentEpoch = transactionRepository.getLedgerEpoch()) {
                    is ResultInternal.Error -> false
                    is ResultInternal.Success -> {
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
