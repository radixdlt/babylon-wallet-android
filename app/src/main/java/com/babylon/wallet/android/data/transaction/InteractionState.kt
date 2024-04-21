package com.babylon.wallet.android.data.transaction

import com.babylon.wallet.android.domain.RadixWalletException
import rdx.works.profile.data.model.factorsources.DeviceFactorSource
import rdx.works.profile.data.model.factorsources.FactorSource
import rdx.works.profile.data.model.factorsources.LedgerHardwareWalletFactorSource
import rdx.works.profile.data.model.pernetwork.SigningPurpose

@Deprecated("It will be removed once refactoring of access factor sources is complete.")
sealed class InteractionState(val factorSource: FactorSource) {

    abstract val label: String

    sealed class Device(private val deviceFactorSource: DeviceFactorSource) : InteractionState(deviceFactorSource) {

        data class Pending(
            private val deviceFactorSource: DeviceFactorSource,
            val signingPurpose: SigningPurpose = SigningPurpose.SignTransaction
        ) : Device(deviceFactorSource)

        data class Success(
            private val deviceFactorSource: DeviceFactorSource,
            val signingPurpose: SigningPurpose = SigningPurpose.SignTransaction
        ) : Device(deviceFactorSource)

        override val label: String
            get() = deviceFactorSource.hint.name
    }

    sealed class Ledger(
        private val ledgerFactorSource: LedgerHardwareWalletFactorSource
    ) : InteractionState(ledgerFactorSource) {

        data class DerivingPublicKey(val ledgerFactorSource: LedgerHardwareWalletFactorSource) : Ledger(ledgerFactorSource)

        data class Pending(
            val ledgerFactorSource: LedgerHardwareWalletFactorSource,
            val signingPurpose: SigningPurpose = SigningPurpose.SignTransaction
        ) : Ledger(ledgerFactorSource)

        data class Success(
            val ledgerFactorSource: LedgerHardwareWalletFactorSource,
            val signingPurpose: SigningPurpose = SigningPurpose.SignTransaction
        ) : Ledger(ledgerFactorSource)

        data class Error(
            private val ledgerFactorSource: LedgerHardwareWalletFactorSource,
            val signingPurpose: SigningPurpose?,
            val failure: RadixWalletException.LedgerCommunicationException
        ) : Ledger(ledgerFactorSource)

        override val label: String
            get() = ledgerFactorSource.hint.name
    }

    val usingLedger: Boolean = this is Ledger
}
