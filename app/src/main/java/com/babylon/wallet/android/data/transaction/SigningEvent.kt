package com.babylon.wallet.android.data.transaction

import rdx.works.profile.data.model.factorsources.FactorSource

sealed interface SigningEvent {
    data class WithDevice(val ledgerDevice: FactorSource) : SigningEvent
    data class WithDeviceSucceeded(val factorSourceId: FactorSource.ID) : SigningEvent
    data class WithDeviceFailed(val factorSourceId: FactorSource.ID) : SigningEvent
    data class WithLedger(val ledgerDevice: FactorSource) : SigningEvent
    data class WithLedgerSucceeded(val factorSourceId: FactorSource.ID) : SigningEvent
    data class WithLedgerFailed(val factorSourceId: FactorSource.ID) : SigningEvent
}
