package com.babylon.wallet.android.utils

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

    suspend fun sendEvent(event: AppEvent) {
        _events.emit(event)
    }
}

sealed interface AppEvent {
    object GotFreeXrd : AppEvent
    object RestoredMnemonic : AppEvent
    data class DerivedAccountPublicKeyWithLedger(
        val factorSourceID: FactorSource.ID,
        val derivationPath: DerivationPath,
        val derivedPublicKeyHex: String
    ) : AppEvent

    sealed class TransactionEvent(val requestId: String) : AppEvent {
        data class Sent(private val reqId: String) : TransactionEvent(reqId)
        data class Successful(private val reqId: String) : TransactionEvent(reqId)
        data class Failed(private val reqId: String, val errorTextRes: Int?) : TransactionEvent(reqId)
    }
}
