package com.babylon.wallet.android.domain.usecases.assets

import com.babylon.wallet.android.data.repository.state.StateRepository
import com.radixdlt.sargon.extensions.ProfileEntity
import javax.inject.Inject

class GetEntitiesOwnerKeysUseCase @Inject constructor(
    private val stateRepository: StateRepository
) {

    suspend operator fun invoke(entities: List<ProfileEntity>) = stateRepository.getEntityOwnerKeys(entities)
}
