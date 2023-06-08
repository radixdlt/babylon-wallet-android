package com.babylon.wallet.android.data.transaction

import rdx.works.profile.data.model.factorsources.FactorSource

sealed class SigningState(val factorSource: FactorSource) {
    data class WithDevice(private val deviceFactorSource: FactorSource) : SigningState(deviceFactorSource)
    data class WithDeviceSucceeded(private val deviceFactorSource: FactorSource) : SigningState(deviceFactorSource)
    data class WithDeviceFailed(private val deviceFactorSource: FactorSource) : SigningState(deviceFactorSource)
    data class WithLedger(private val ledgerDevice: FactorSource) : SigningState(ledgerDevice)
    data class WithLedgerSucceeded(private val ledgerDevice: FactorSource) : SigningState(ledgerDevice)
    data class WithLedgerFailed(private val ledgerDevice: FactorSource) : SigningState(ledgerDevice)

    fun usingLedger(): Boolean {
        return this is WithLedger || this is WithLedgerSucceeded || this is WithLedgerFailed
    }
}
