package com.babylon.wallet.android.utils

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import rdx.works.profile.data.model.factorsources.FactorSource
import rdx.works.profile.data.model.pernetwork.DerivationPath
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppEventBus @Inject constructor() {

    private val _events = MutableSharedFlow<AppEvent>()
    val events = _events.asSharedFlow()

    suspend fun sendEvent(event: AppEvent) {
        _events.emit(event)
    }
}

sealed interface AppEvent {
    object GotFreeXrd : AppEvent
    object ApprovedTransaction : AppEvent
    data class DerivedAccountPublicKeyWithLedger(
        val factorSourceID: FactorSource.ID,
        val derivationPath: DerivationPath,
        val derivedPublicKeyHex: String
    ) : AppEvent
}
