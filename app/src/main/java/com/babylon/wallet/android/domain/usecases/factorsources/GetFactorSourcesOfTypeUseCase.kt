package com.babylon.wallet.android.domain.usecases.factorsources

import com.radixdlt.sargon.FactorSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import rdx.works.profile.domain.GetProfileUseCase
import javax.inject.Inject

class GetFactorSourcesOfTypeUseCase @Inject constructor(
    val getProfileUseCase: GetProfileUseCase
) {

    inline operator fun <reified T : FactorSource> invoke(): Flow<List<T>> {
        return getProfileUseCase.flow
            .map { profile ->
                profile.factorSources.filterIsInstance<T>()
            }
    }
}
