package com.babylon.wallet.android.domain.usecases.factorsources

import com.radixdlt.sargon.FactorSource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapConcat
import rdx.works.profile.domain.GetProfileUseCase
import javax.inject.Inject

class GetFactorSourcesUseCaseOfType @Inject constructor(
    val getProfileUseCase: GetProfileUseCase
) {

    @OptIn(ExperimentalCoroutinesApi::class)
    inline operator fun <reified T : FactorSource> invoke(): Flow<T> {
        return getProfileUseCase.flow
            .flatMapConcat { profile ->
                profile.factorSources.asFlow()
            }
            .filterIsInstance<T>()
    }
}
