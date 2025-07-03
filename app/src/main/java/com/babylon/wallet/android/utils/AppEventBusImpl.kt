package com.babylon.wallet.android.utils

import com.babylon.wallet.android.presentation.ui.composables.actionableaddress.ActionableAddress
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.DappWalletInteractionErrorType
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.SubintentHash
import com.radixdlt.sargon.TransactionIntentHash
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import kotlin.time.Duration

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

    data object RefreshAssetsNeeded : AppEvent

    data object RestoredMnemonic : AppEvent

    data object BabylonFactorSourceDoesNotExist : AppEvent

    data object NPSSurveySubmitted : AppEvent

    data object SecureFolderWarning : AppEvent

    data class DeferRequestHandling(val interactionId: String) : AppEvent

    data object ProcessBufferedDeepLinkRequest : AppEvent

    data class AddressDetails(val address: ActionableAddress) : AppEvent

    /**
     * An account was just deleted. This event is fired when a transaction of [TransactionType.DeleteAccount] succeeds. The user
     * should pre presented with the [DeletedAccountScreen]
     */
    data class AccountDeleted(val address: AccountAddress) : AppEvent

    /**
     * Some accounts were detected to have been deleted and need to sync with profile. The user will be presented to a simple modal.
     */
    data object AccountsPreviouslyDeletedDetected : AppEvent

    // events that trigger the access factor sources bottom sheet dialogs
    sealed interface AccessFactorSources : AppEvent {

        sealed interface SelectLedgerOutcome : AccessFactorSources {

            data class Selected(val ledgerFactorSource: FactorSource.Ledger) : SelectLedgerOutcome

            data object Rejected : SelectLedgerOutcome
        }

        data object RequestAuthorization : AccessFactorSources

        data object DerivePublicKeys : AccessFactorSources

        data object GetSignatures : AccessFactorSources

        data object SpotCheck : AccessFactorSources
    }

    data class AddFactorSource(val withKind: Boolean) : AppEvent

    data object SelectFactorSource : AppEvent

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
            abstract val dAppName: String?

            data class InProgress(
                override val requestId: String,
                override val transactionId: String,
                override val isInternal: Boolean,
                override val blockUntilComplete: Boolean,
                override val isMobileConnect: Boolean,
                override val dAppName: String?
            ) : Transaction()

            data class Success(
                override val requestId: String,
                override val transactionId: String,
                override val isInternal: Boolean,
                override val blockUntilComplete: Boolean,
                override val isMobileConnect: Boolean,
                override val dAppName: String?
            ) : Transaction()

            data class Fail(
                override val requestId: String,
                override val transactionId: String,
                override val isInternal: Boolean,
                override val blockUntilComplete: Boolean,
                val errorMessage: String?,
                val walletErrorType: DappWalletInteractionErrorType?,
                override val isMobileConnect: Boolean,
                override val dAppName: String?
            ) : Transaction()
        }

        sealed class PreAuthorization : Status() {

            abstract val preAuthorizationId: SubintentHash
            abstract val isMobileConnect: Boolean
            abstract val dAppName: String?

            data class Sent(
                override val requestId: String,
                override val preAuthorizationId: SubintentHash,
                override val isMobileConnect: Boolean,
                override val dAppName: String?,
                val remainingTime: Duration
            ) : PreAuthorization()

            data class Success(
                override val requestId: String,
                override val preAuthorizationId: SubintentHash,
                override val isMobileConnect: Boolean,
                override val dAppName: String?,
                val transactionId: TransactionIntentHash
            ) : PreAuthorization()

            data class Expired(
                override val requestId: String,
                override val preAuthorizationId: SubintentHash,
                override val isMobileConnect: Boolean,
                override val dAppName: String?
            ) : PreAuthorization()

            val encodedPreAuthorizationId: String
                get() = preAuthorizationId.bech32EncodedTxId
        }
    }
}
