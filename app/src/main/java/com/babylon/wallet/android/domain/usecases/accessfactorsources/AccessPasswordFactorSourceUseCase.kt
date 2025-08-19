package com.babylon.wallet.android.domain.usecases.accessfactorsources

import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesInput
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesOutput
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.HierarchicalDeterministicFactorInstance
import com.radixdlt.sargon.KeyDerivationRequestPerFactorSource
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
        input: AccessFactorSourcesInput.Sign
    ): Result<AccessFactorSourcesOutput.Sign> {
        TODO("Not yet implemented")
    }

    override suspend fun spotCheck(factorSource: FactorSource.Password): Result<Boolean> {
        TODO("Future implementation")
    }
}
