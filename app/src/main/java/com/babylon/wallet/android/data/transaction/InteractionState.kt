package com.babylon.wallet.android.data.transaction

import com.babylon.wallet.android.domain.RadixWalletException
import com.babylon.wallet.android.domain.usecases.transaction.SignRequest
import com.radixdlt.sargon.FactorSource

@Deprecated("It will be removed once refactoring of access factor sources is complete.")
sealed class InteractionState(val factorSource: FactorSource) {

    abstract val label: String

    sealed class Device(private val deviceFactorSource: FactorSource.Device) : InteractionState(deviceFactorSource) {

        data class Pending(
            private val deviceFactorSource: FactorSource.Device,
            val signingPurpose: SigningPurpose = SigningPurpose.Transaction
        ) : Device(deviceFactorSource)

        override val label: String
            get() = deviceFactorSource.value.hint.name
    }

    sealed class Ledger(
        private val ledgerFactorSource: FactorSource.Ledger
    ) : InteractionState(ledgerFactorSource) {

        data class DerivingPublicKey(val ledgerFactorSource: FactorSource.Ledger) : Ledger(ledgerFactorSource)

        data class Pending(
            val ledgerFactorSource: FactorSource.Ledger,
            val signingPurpose: SigningPurpose = SigningPurpose.Transaction
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

    enum class SigningPurpose {
        AuthChallenge,
        Transaction;

        companion object {
            fun from(request: SignRequest) = when (request) {
                is SignRequest.SignAuthChallengeRequest -> AuthChallenge
                is SignRequest.SignTransactionRequest -> Transaction
            }
        }
    }

}
