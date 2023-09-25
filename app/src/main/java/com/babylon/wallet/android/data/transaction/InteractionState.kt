package com.babylon.wallet.android.data.transaction

import rdx.works.profile.data.model.factorsources.DeviceFactorSource
import rdx.works.profile.data.model.factorsources.FactorSource
import rdx.works.profile.data.model.factorsources.LedgerHardwareWalletFactorSource
import rdx.works.profile.data.model.pernetwork.SigningPurpose

sealed class InteractionState(val factorSource: FactorSource) {

    abstract val label: String
    abstract val signingPurpose: SigningPurpose?

    sealed class Device(private val deviceFactorSource: DeviceFactorSource) : InteractionState(deviceFactorSource) {
        data class Pending(
            private val deviceFactorSource: DeviceFactorSource,
            override val signingPurpose: SigningPurpose = SigningPurpose.SignTransaction
        ) : Device(deviceFactorSource)

        data class Success(
            private val deviceFactorSource: DeviceFactorSource,
            override val signingPurpose: SigningPurpose = SigningPurpose.SignTransaction
        ) : Device(deviceFactorSource)

        override val label: String
            get() = deviceFactorSource.hint.name
    }

    sealed class Ledger(
        private val ledgerFactorSource: LedgerHardwareWalletFactorSource
    ) : InteractionState(ledgerFactorSource) {

        data class DerivingPublicKey(
            private val ledgerFactorSource: LedgerHardwareWalletFactorSource,
            override val signingPurpose: SigningPurpose? = null
        ) : Ledger(ledgerFactorSource)

        data class Pending(
            private val ledgerFactorSource: LedgerHardwareWalletFactorSource,
            override val signingPurpose: SigningPurpose = SigningPurpose.SignTransaction
        ) : Ledger(ledgerFactorSource)

        data class Success(
            private val ledgerFactorSource: LedgerHardwareWalletFactorSource,
            override val signingPurpose: SigningPurpose = SigningPurpose.SignTransaction
        ) : Ledger(ledgerFactorSource)

        data class Error(
            private val ledgerFactorSource: LedgerHardwareWalletFactorSource,
            override val signingPurpose: SigningPurpose?,
            val failure: DappRequestFailure
        ) : Ledger(ledgerFactorSource)

        override val label: String
            get() = ledgerFactorSource.hint.name
    }

    val usingLedger: Boolean = this is Ledger
}
