package com.babylon.wallet.android.domain.usecases.accessfactorsources

import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.HierarchicalDeterministicFactorInstance
import com.radixdlt.sargon.KeyDerivationRequestPerFactorSource
import com.radixdlt.sargon.os.signing.PerFactorOutcome
import com.radixdlt.sargon.os.signing.PerFactorSourceInput
import com.radixdlt.sargon.os.signing.Signable
import javax.inject.Inject

class AccessPasswordFactorSourceUseCase @Inject constructor() : AccessFactorSource<FactorSource.Password> {

    override suspend fun derivePublicKeys(
        factorSource: FactorSource.Password,
        input: KeyDerivationRequestPerFactorSource
    ): Result<List<HierarchicalDeterministicFactorInstance>> {
        TODO("Future implementation")
    }

    override suspend fun signMono(
        factorSource: FactorSource.Password,
        input: PerFactorSourceInput<out Signable.Payload, out Signable.ID>
    ): Result<PerFactorOutcome<Signable.ID>> {
        TODO("Future implementation")
    }

    override suspend fun spotCheck(factorSource: FactorSource.Password): Result<Boolean> {
        TODO("Future implementation")
    }
}
