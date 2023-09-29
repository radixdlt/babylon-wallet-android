package com.babylon.wallet.android.utils

import com.babylon.wallet.android.presentation.common.UiMessage
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import rdx.works.profile.data.model.factorsources.FactorSource
import rdx.works.profile.data.model.pernetwork.DerivationPath
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
    data object GotFreeXrd : AppEvent
    data object RestoredMnemonic : AppEvent
    data object BabylonFactorSourceDoesNotExist : AppEvent
    data class BabylonFactorSourceNeedsRecovery(val factorSourceID: FactorSource.FactorSourceID.FromHash) : AppEvent
    data class DerivedAccountPublicKeyWithLedger(
        val factorSourceID: FactorSource.FactorSourceID.FromHash,
        val derivationPath: DerivationPath,
        val derivedPublicKeyHex: String
    ) : AppEvent

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
                val errorMessage: UiMessage.ErrorMessage?
            ) : Transaction()
        }
    }
}
