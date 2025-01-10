package com.babylon.wallet.android.presentation.accessfactorsources.access

import com.babylon.wallet.android.domain.usecases.signing.SignWithLedgerFactorSourceUseCase
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.os.signing.PerFactorOutcome
import com.radixdlt.sargon.os.signing.PerFactorSourceInput
import com.radixdlt.sargon.os.signing.Signable
import javax.inject.Inject

class AccessLedgerHardwareWalletFactorSource @Inject constructor(
    private val signWithLedgerFactorSourceUseCase: SignWithLedgerFactorSourceUseCase
): AccessFactorSource<FactorSource.Ledger> {
    override suspend fun signMono(
        factorSource: FactorSource.Ledger,
        input: PerFactorSourceInput<Signable.Payload, Signable.ID>
    ): Result<PerFactorOutcome<Signable.ID>> = signWithLedgerFactorSourceUseCase.mono(
        ledgerFactorSource = factorSource,
        input = input
    )
}