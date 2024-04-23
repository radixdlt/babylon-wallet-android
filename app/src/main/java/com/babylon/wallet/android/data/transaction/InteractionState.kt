package com.babylon.wallet.android.data.transaction

import com.babylon.wallet.android.domain.RadixWalletException
import com.radixdlt.sargon.FactorSource
import rdx.works.core.domain.SigningPurpose

sealed class InteractionState(val factorSource: FactorSource) {

    abstract val label: String

    sealed class Device(private val deviceFactorSource: FactorSource.Device) : InteractionState(deviceFactorSource) {

        data class DerivingAccounts(private val deviceFactorSource: FactorSource.Device) : Device(deviceFactorSource)

        data class Pending(
            private val deviceFactorSource: FactorSource.Device,
            val signingPurpose: SigningPurpose = SigningPurpose.SignTransaction
        ) : Device(deviceFactorSource)

        data class Success(
            private val deviceFactorSource: FactorSource.Device,
            val signingPurpose: SigningPurpose = SigningPurpose.SignTransaction
        ) : Device(deviceFactorSource)

        override val label: String
            get() = deviceFactorSource.value.hint.name
    }

    sealed class Ledger(
        private val ledgerFactorSource: FactorSource.Ledger
    ) : InteractionState(ledgerFactorSource) {

        data class DerivingPublicKey(val ledgerFactorSource: FactorSource.Ledger) : Ledger(ledgerFactorSource)
        data class DerivingAccounts(val ledgerFactorSource: FactorSource.Ledger) : Ledger(ledgerFactorSource)

        data class Pending(
            val ledgerFactorSource: FactorSource.Ledger,
            val signingPurpose: SigningPurpose = SigningPurpose.SignTransaction
        ) : Ledger(ledgerFactorSource)

        data class Success(
            val ledgerFactorSource: FactorSource.Ledger,
            val signingPurpose: SigningPurpose = SigningPurpose.SignTransaction
        ) : Ledger(ledgerFactorSource)

        data class Error(
            private val ledgerFactorSource: FactorSource.Ledger,
            val signingPurpose: SigningPurpose?,
            val failure: RadixWalletException.LedgerCommunicationException
        ) : Ledger(ledgerFactorSource)

        override val label: String
            get() = ledgerFactorSource.value.hint.name
    }

    val usingLedger: Boolean = this is Ledger
}
