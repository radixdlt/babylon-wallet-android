package com.babylon.wallet.android.utils

import com.babylon.wallet.android.data.dapp.model.WalletErrorType
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.FactorSourceId
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppEventBus @Inject constructor() {

    private val _events = MutableSharedFlow<AppEvent>()
    val events: Flow<AppEvent> = _events.asSharedFlow()

    suspend fun sendEvent(event: AppEvent, delayMs: Long = 0L) {
        delay(delayMs)
        _events.emit(event)
    }
}

sealed interface AppEvent {
    data object AppNotSecure : AppEvent
    data object RefreshResourcesNeeded : AppEvent
    data object RestoredMnemonic : AppEvent
    data object BabylonFactorSourceDoesNotExist : AppEvent
    data object NPSSurveySubmitted : AppEvent
    data class BabylonFactorSourceNeedsRecovery(val factorSourceID: FactorSourceId.Hash) : AppEvent

    sealed interface AccessFactorSources : AppEvent {

        data class SelectedLedgerDevice(val ledgerFactorSource: FactorSource.Ledger) : AccessFactorSources

        data object DerivePublicKey : AccessFactorSources

        data object DeriveAccounts : AccessFactorSources
    }

    sealed class Status : AppEvent {
        abstract val requestId: String

        data class DappInteraction(
            override val requestId: String,
            val dAppName: String?
        ) : Status()

        sealed class Transaction : Status() {

            abstract val transactionId: String
            abstract val isInternal: Boolean
            abstract val blockUntilComplete: Boolean

            data class InProgress(
                override val requestId: String,
                override val transactionId: String,
                override val isInternal: Boolean,
                override val blockUntilComplete: Boolean
            ) : Transaction()

            data class Success(
                override val requestId: String,
                override val transactionId: String,
                override val isInternal: Boolean,
                override val blockUntilComplete: Boolean
            ) : Transaction()

            data class Fail(
                override val requestId: String,
                override val transactionId: String,
                override val isInternal: Boolean,
                override val blockUntilComplete: Boolean,
                val errorMessage: String?,
                val walletErrorType: WalletErrorType?
            ) : Transaction()
        }
    }
}
