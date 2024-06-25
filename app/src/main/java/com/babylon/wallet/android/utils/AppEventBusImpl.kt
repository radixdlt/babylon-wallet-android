package com.babylon.wallet.android.utils

import com.radixdlt.sargon.DappWalletInteractionErrorType
import com.radixdlt.sargon.FactorSource
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject

interface AppEventBus {

    val events: Flow<AppEvent>
    suspend fun sendEvent(event: AppEvent, delayMs: Long = 0L)
}

class AppEventBusImpl @Inject constructor() : AppEventBus {

    private val _events = MutableSharedFlow<AppEvent>()
    override val events: Flow<AppEvent> = _events.asSharedFlow()

    override suspend fun sendEvent(event: AppEvent, delayMs: Long) {
        delay(delayMs)
        _events.emit(event)
    }
}

sealed interface AppEvent {
    data object AppNotSecure : AppEvent
    data object RefreshAssetsNeeded : AppEvent
    data object RestoredMnemonic : AppEvent
    data object BabylonFactorSourceDoesNotExist : AppEvent
    data object NPSSurveySubmitted : AppEvent

    data object SecureFolderWarning : AppEvent
    data class DeferRequestHandling(val interactionId: String) : AppEvent
    sealed interface AccessFactorSources : AppEvent {

        data class SelectedLedgerDevice(val ledgerFactorSource: FactorSource.Ledger) : AccessFactorSources

        data object DerivePublicKey : AccessFactorSources

        data object DeriveAccounts : AccessFactorSources
    }

    sealed class Status : AppEvent {
        abstract val requestId: String
        data class DappInteraction(
            override val requestId: String,
            val dAppName: String?,
            val isMobileConnect: Boolean = false
        ) : Status()

        sealed class Transaction : Status() {

            abstract val transactionId: String
            abstract val isInternal: Boolean
            abstract val blockUntilComplete: Boolean
            abstract val isMobileConnect: Boolean

            data class InProgress(
                override val requestId: String,
                override val transactionId: String,
                override val isInternal: Boolean,
                override val blockUntilComplete: Boolean,
                override val isMobileConnect: Boolean
            ) : Transaction()

            data class Success(
                override val requestId: String,
                override val transactionId: String,
                override val isInternal: Boolean,
                override val blockUntilComplete: Boolean,
                override val isMobileConnect: Boolean
            ) : Transaction()

            data class Fail(
                override val requestId: String,
                override val transactionId: String,
                override val isInternal: Boolean,
                override val blockUntilComplete: Boolean,
                val errorMessage: String?,
                val walletErrorType: DappWalletInteractionErrorType?,
                override val isMobileConnect: Boolean
            ) : Transaction()
        }
    }
}
