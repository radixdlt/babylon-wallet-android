package com.babylon.wallet.android.domain.usecases.accessfactorsources

import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.HierarchicalDeterministicFactorInstance
import com.radixdlt.sargon.KeyDerivationRequestPerFactorSource
import com.radixdlt.sargon.os.signing.PerFactorOutcome
import com.radixdlt.sargon.os.signing.PerFactorSourceInput
import com.radixdlt.sargon.os.signing.Signable

interface AccessFactorSource<F : FactorSource> {

    suspend fun derivePublicKeys(
        factorSource: F,
        input: KeyDerivationRequestPerFactorSource
    ): Result<List<HierarchicalDeterministicFactorInstance>>

    suspend fun signMono(
        factorSource: F,
        input: PerFactorSourceInput<out Signable.Payload, out Signable.ID>
    ): Result<PerFactorOutcome<Signable.ID>>

    suspend fun spotCheck(
        factorSource: F
    ): Result<Boolean>
}
