package com.babylon.wallet.android.domain.usecases

import com.babylon.wallet.android.data.repository.transaction.TransactionRepository
import com.babylon.wallet.android.data.transaction.TransactionConfig
import com.babylon.wallet.android.data.transaction.TransactionConfig.TIP_PERCENTAGE
import com.babylon.wallet.android.di.coroutines.IoDispatcher
import com.babylon.wallet.android.domain.RadixWalletException
import com.babylon.wallet.android.domain.usecases.transaction.PollTransactionStatusUseCase
import com.babylon.wallet.android.domain.usecases.transaction.SubmitTransactionUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import rdx.works.core.preferences.PreferencesManager
import rdx.works.profile.data.model.apppreferences.Radix
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.gateways
import rdx.works.profile.ret.ManifestPoet
import java.math.BigDecimal
import javax.inject.Inject

@Suppress("LongParameterList")
class GetFreeXrdUseCase @Inject constructor(
    private val signTransactionUseCase: SignTransactionUseCase,
    private val transactionRepository: TransactionRepository,
    private val getProfileUseCase: GetProfileUseCase,
    private val preferencesManager: PreferencesManager,
    private val pollTransactionStatusUseCase: PollTransactionStatusUseCase,
    private val submitTransactionUseCase: SubmitTransactionUseCase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {

    suspend operator fun invoke(address: String): Result<String> {
        return withContext(ioDispatcher) {
            val manifest = ManifestPoet
                .buildFaucet(toAddress = address)
                .getOrElse {
                    return@withContext Result.failure(it)
                }

            val epochResult = transactionRepository.getLedgerEpoch()
            epochResult.getOrNull()?.let { epoch ->
                signTransactionUseCase.sign(
                    request = SignTransactionUseCase.Request(
                        manifest = manifest,
                        lockFee = BigDecimal.valueOf(TransactionConfig.DEFAULT_LOCK_FEE),
                        tipPercentage = TIP_PERCENTAGE
                    ),
                    deviceBiometricAuthenticationProvider = { true }
                ).mapCatching { notarization ->
                    submitTransactionUseCase(
                        notarization.txIdHash,
                        notarization.notarizedTransactionIntentHex,
                        endEpoch = notarization.endEpoch
                    ).getOrThrow()
                }.onSuccess { submitTransactionResult ->
                    pollTransactionStatusUseCase(
                        txID = submitTransactionResult.txId,
                        requestId = "",
                        endEpoch = submitTransactionResult.endEpoch
                    ).result.onSuccess {
                        preferencesManager.updateEpoch(address, epoch)
                    }
                }.mapCatching {
                    it.txId
                }
            } ?: Result.failure(
                exception = epochResult.exceptionOrNull() ?: RadixWalletException.PrepareTransactionException.PrepareNotarizedTransaction()
            )
        }
    }

    fun getFaucetState(address: String): Flow<FaucetState> = combine(
        getProfileUseCase.gateways,
        preferencesManager.getLastUsedEpochFlow(address)
    ) { gateways, lastUsedEpoch ->
        if (gateways.current().network.id == Radix.Gateway.mainnet.network.id) {
            FaucetState.Unavailable
        } else {
            if (lastUsedEpoch == null) return@combine FaucetState.Available(isEnabled = true)

            val isEnabled = transactionRepository.getLedgerEpoch().getOrNull()?.let { currentEpoch ->
                when {
                    currentEpoch < lastUsedEpoch -> true // edge case ledger was reset - allow
                    else -> {
                        val threshold = 1
                        currentEpoch - lastUsedEpoch >= threshold
                    }
                }
            } ?: false
            FaucetState.Available(isEnabled = isEnabled)
        }
    }
}

sealed interface FaucetState {
    data object Unavailable : FaucetState
    data class Available(val isEnabled: Boolean) : FaucetState
}
