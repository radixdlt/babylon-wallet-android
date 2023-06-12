package com.babylon.wallet.android.utils

import androidx.annotation.StringRes
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.serialization.Serializable
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

    @Serializable
    sealed class Status: AppEvent {
        abstract val requestId: String

        @Serializable
        data class DappInteraction(
            override val requestId: String,
            val dAppName: String?
        ): Status()

        @Serializable
        sealed class Transaction : Status() {

            abstract val transactionId: String
            abstract val isInternal: Boolean

            @Serializable
            data class Sent(
                override val requestId: String,
                override val transactionId: String,
                override val isInternal: Boolean
            ) : Transaction()

            @Serializable
            data class Successful(
                override val requestId: String,
                override val transactionId: String,
                override val isInternal: Boolean
            ) : Transaction()

            @Serializable
            data class Failed(
                override val requestId: String,
                override val transactionId: String,
                override val isInternal: Boolean,
                @StringRes val errorMessageRes: Int?
            ) : Transaction()
        }
    }
}
