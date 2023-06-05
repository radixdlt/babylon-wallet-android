package com.babylon.wallet.android.data.transaction

import rdx.works.profile.data.model.factorsources.FactorSource

sealed interface SigningEvent {
    data class SigningWithDevice(val ledgerDevice: FactorSource) : SigningEvent
    data class SigningWithDeviceSucceeded(val factorSourceId: FactorSource.ID) : SigningEvent
    data class SigningWithDeviceFailed(val factorSourceId: FactorSource.ID) : SigningEvent
    data class SigningWithLedger(val ledgerDevice: FactorSource) : SigningEvent
    data class SigningWithLedgerSucceeded(val factorSourceId: FactorSource.ID) : SigningEvent
    data class SigningWithLedgerFailed(val factorSourceId: FactorSource.ID) : SigningEvent
}
