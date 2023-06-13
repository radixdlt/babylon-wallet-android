package com.babylon.wallet.android.data.transaction

import rdx.works.profile.data.model.factorsources.FactorSource

sealed class SigningState(val factorSource: FactorSource) {

    sealed class Device(deviceFactorSource: FactorSource) : SigningState(deviceFactorSource) {
        data class Pending(private val deviceFactorSource: FactorSource) : Device(deviceFactorSource)
        data class Success(private val deviceFactorSource: FactorSource) : Device(deviceFactorSource)
        data class Failure(private val deviceFactorSource: FactorSource) : Device(deviceFactorSource)
    }

    sealed class Ledger(ledgerFactorSource: FactorSource) : SigningState(ledgerFactorSource) {
        data class Pending(private val ledgerFactorSource: FactorSource) : Ledger(ledgerFactorSource)
        data class Success(private val ledgerFactorSource: FactorSource) : Ledger(ledgerFactorSource)
        data class Failure(private val ledgerFactorSource: FactorSource) : Ledger(ledgerFactorSource)
    }

    fun usingLedger(): Boolean {
        return this is Ledger
    }
}
