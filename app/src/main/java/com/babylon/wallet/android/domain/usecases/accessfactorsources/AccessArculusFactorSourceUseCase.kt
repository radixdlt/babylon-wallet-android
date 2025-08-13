package com.babylon.wallet.android.domain.usecases.accessfactorsources

import com.babylon.wallet.android.utils.callSafely
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.HierarchicalDeterministicFactorInstance
import com.radixdlt.sargon.KeyDerivationRequestPerFactorSource
import com.radixdlt.sargon.os.SargonOsManager
import com.radixdlt.sargon.os.signing.PerFactorOutcome
import com.radixdlt.sargon.os.signing.PerFactorSourceInput
import com.radixdlt.sargon.os.signing.Signable
import kotlinx.coroutines.CoroutineDispatcher
import rdx.works.core.di.DefaultDispatcher
import javax.inject.Inject

class AccessArculusFactorSourceUseCase @Inject constructor(
    private val sargonOsManager: SargonOsManager,
    @DefaultDispatcher private val dispatcher: CoroutineDispatcher
) : AccessFactorSource<FactorSource.ArculusCard> {

    override suspend fun derivePublicKeys(
        factorSource: FactorSource.ArculusCard,
        input: KeyDerivationRequestPerFactorSource
    ): Result<List<HierarchicalDeterministicFactorInstance>> {
        return sargonOsManager.callSafely(dispatcher) {
            arculusCardDerivePublicKeys(factorSource.value, input.derivationPaths)
        }
    }

    override suspend fun signMono(
        factorSource: FactorSource.ArculusCard,
        input: PerFactorSourceInput<out Signable.Payload, out Signable.ID>
    ): Result<PerFactorOutcome<Signable.ID>> {
        TODO("Future implementation")
    }

    override suspend fun spotCheck(factorSource: FactorSource.ArculusCard): Result<Boolean> {
        TODO("Future implementation")
    }
}
