package com.babylon.wallet.android.presentation.accessfactorsources.access

import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.os.signing.PerFactorOutcome
import com.radixdlt.sargon.os.signing.PerFactorSourceInput
import com.radixdlt.sargon.os.signing.Signable

interface AccessFactorSource<F: FactorSource> {

    suspend fun signMono(
        factorSource: F,
        input: PerFactorSourceInput<Signable.Payload, Signable.ID>
    ): Result<PerFactorOutcome<Signable.ID>>

}