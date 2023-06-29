package com.babylon.wallet.android.data.transaction

import rdx.works.profile.data.model.factorsources.DeviceFactorSource
import rdx.works.profile.data.model.factorsources.FactorSource
import rdx.works.profile.data.model.factorsources.LedgerHardwareWalletFactorSource

sealed class SigningState(val factorSource: FactorSource) {

    abstract val label: String

    sealed class Device(private val deviceFactorSource: DeviceFactorSource) : SigningState(deviceFactorSource) {
        data class Pending(private val deviceFactorSource: DeviceFactorSource) : Device(deviceFactorSource)
        data class Success(private val deviceFactorSource: DeviceFactorSource) : Device(deviceFactorSource)
        data class Failure(private val deviceFactorSource: DeviceFactorSource) : Device(deviceFactorSource)

        override val label: String
            get() = deviceFactorSource.hint.name
    }

    sealed class Ledger(
        private val ledgerFactorSource: LedgerHardwareWalletFactorSource
    ) : SigningState(ledgerFactorSource) {
        data class Pending(
            private val ledgerFactorSource: LedgerHardwareWalletFactorSource
        ) : Ledger(ledgerFactorSource)

        data class Success(
            private val ledgerFactorSource: LedgerHardwareWalletFactorSource
        ) : Ledger(ledgerFactorSource)

        data class Failure(
            private val ledgerFactorSource: LedgerHardwareWalletFactorSource
        ) : Ledger(ledgerFactorSource)

        override val label: String
            get() = ledgerFactorSource.hint.name
    }

    val usingLedger: Boolean = this is Ledger
}
