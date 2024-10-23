package com.babylon.wallet.android.domain.usecases

import com.babylon.wallet.android.data.repository.transaction.TransactionRepository
import com.babylon.wallet.android.di.coroutines.IoDispatcher
import com.babylon.wallet.android.domain.RadixWalletException
import com.babylon.wallet.android.domain.usecases.signing.SignTransactionUseCase
import com.babylon.wallet.android.domain.usecases.transaction.PollTransactionStatusUseCase
import com.babylon.wallet.android.domain.usecases.transaction.TransactionConfig
import com.babylon.wallet.android.domain.usecases.transaction.TransactionConfig.TIP_PERCENTAGE
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.NetworkId
import com.radixdlt.sargon.TransactionManifest
import com.radixdlt.sargon.extensions.faucet
import com.radixdlt.sargon.extensions.toDecimal192
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import rdx.works.core.domain.TransactionManifestData
import rdx.works.core.preferences.PreferencesManager
import rdx.works.core.then
import rdx.works.profile.domain.GetProfileUseCase
import javax.inject.Inject

@Suppress("LongParameterList")
class GetFreeXrdUseCase @Inject constructor(
    private val signTransactionUseCase: SignTransactionUseCase,
    private val transactionRepository: TransactionRepository,
    private val getProfileUseCase: GetProfileUseCase,
    private val preferencesManager: PreferencesManager,
    private val pollTransactionStatusUseCase: PollTransactionStatusUseCase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {

    suspend operator fun invoke(address: AccountAddress): Result<String> {
        return withContext(ioDispatcher) {
            val manifest = runCatching {
                TransactionManifest.faucet(
                    includeLockFeeInstruction = true,
                    addressOfReceivingAccount = address
                )
            }.getOrElse {
                return@withContext Result.failure(it)
            }
            val manifestData = TransactionManifestData.from(manifest = manifest)

            val epochResult = transactionRepository.getLedgerEpoch()
            epochResult.getOrNull()?.let { epoch ->
                signTransactionUseCase(
                    request = SignTransactionUseCase.Request(
                        manifestData = manifestData,
                        lockFee = TransactionConfig.DEFAULT_LOCK_FEE.toDecimal192(),
                        tipPercentage = TIP_PERCENTAGE
                    )
                ).then { notarization ->
                    transactionRepository.submitTransaction(notarization.notarizedTransaction)
                        .map { notarization }
                }.onSuccess { notarization ->
                    pollTransactionStatusUseCase(
                        intentHash = notarization.intentHash,
                        requestId = "",
                        endEpoch = notarization.endEpoch
                    ).result.onSuccess {
                        preferencesManager.updateEpoch(address, epoch)
                    }
                }.mapCatching { notarization ->
                    notarization.intentHash.bech32EncodedTxId
                }
            } ?: Result.failure(
                exception = epochResult.exceptionOrNull() ?: RadixWalletException.PrepareTransactionException.PrepareNotarizedTransaction()
            )
        }
    }

    fun getFaucetState(address: AccountAddress): Flow<FaucetState> = combine(
        getProfileUseCase.flow.map { it.appPreferences.gateways },
        preferencesManager.getLastUsedEpochFlow(address)
    ) { gateways, lastUsedEpoch ->
        if (gateways.current.network.id == NetworkId.MAINNET) {
            FaucetState.Unavailable
        } else {
            if (lastUsedEpoch == null) return@combine FaucetState.Available(isEnabled = true)

            val isEnabled = transactionRepository.getLedgerEpoch().getOrNull()?.let { currentEpoch ->
                when {
                    currentEpoch < lastUsedEpoch -> true // edge case ledger was reset - allow
                    else -> currentEpoch - lastUsedEpoch >= 1.toULong()
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
