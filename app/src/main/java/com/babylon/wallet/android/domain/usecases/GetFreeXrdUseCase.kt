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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import rdx.works.core.preferences.PreferencesManager
import rdx.works.core.ret.BabylonManifestBuilder
import rdx.works.core.ret.buildSafely
import rdx.works.profile.data.model.apppreferences.Radix
import rdx.works.profile.data.model.currentGateway
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.gateways
import java.math.BigDecimal
import javax.inject.Inject
import kotlin.Result
import com.babylon.wallet.android.domain.common.Result as ResultInternal

@Suppress("LongParameterList")
class GetFreeXrdUseCase @Inject constructor(
    private val transactionClient: TransactionClient,
    private val transactionRepository: TransactionRepository,
    private val getProfileUseCase: GetProfileUseCase,
    private val preferencesManager: PreferencesManager,
    private val pollTransactionStatusUseCase: PollTransactionStatusUseCase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {

    suspend operator fun invoke(address: String): Result<String> {
        return withContext(ioDispatcher) {
            val gateway = getProfileUseCase().map { it.currentGateway }.first()
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

    fun getFaucetState(address: String): Flow<FaucetState> = combine(
        getProfileUseCase.gateways,
        preferencesManager.getLastUsedEpochFlow(address)
    ) { gateways, lastUsedEpoch ->
        if (gateways.current().network == Radix.Gateway.mainnet.network) {
            FaucetState.Unavailable
        } else {
            if (lastUsedEpoch == null) return@combine FaucetState.Available(isEnabled = true)

            val isEnabled = when (val currentEpoch = transactionRepository.getLedgerEpoch()) {
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
            FaucetState.Available(isEnabled = isEnabled)
        }
    }
}

sealed interface FaucetState {
    object Unavailable : FaucetState
    data class Available(val isEnabled: Boolean) : FaucetState
}
