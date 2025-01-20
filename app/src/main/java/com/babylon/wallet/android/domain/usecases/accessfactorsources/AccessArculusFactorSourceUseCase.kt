package com.babylon.wallet.android.domain.usecases.accessfactorsources

import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.HierarchicalDeterministicFactorInstance
import com.radixdlt.sargon.KeyDerivationRequestPerFactorSource
import com.radixdlt.sargon.os.signing.PerFactorOutcome
import com.radixdlt.sargon.os.signing.PerFactorSourceInput
import com.radixdlt.sargon.os.signing.Signable
import javax.inject.Inject

class AccessArculusFactorSourceUseCase @Inject constructor() : AccessFactorSource<FactorSource.ArculusCard> {

    override suspend fun derivePublicKeys(
        factorSource: FactorSource.ArculusCard,
        input: KeyDerivationRequestPerFactorSource
    ): Result<List<HierarchicalDeterministicFactorInstance>> {
        TODO("Future implementation")
    }

    override suspend fun signMono(
        factorSource: FactorSource.ArculusCard,
        input: PerFactorSourceInput<out Signable.Payload, out Signable.ID>
    ): Result<PerFactorOutcome<Signable.ID>> {
        TODO("Future implementation")
    }
}
