package com.babylon.wallet.android.domain.usecases.assets

import com.babylon.wallet.android.data.repository.state.StateRepository
import rdx.works.profile.data.model.pernetwork.Entity
import javax.inject.Inject

class GetEntitiesOwnerKeysUseCase @Inject constructor(
    private val stateRepository: StateRepository
) {

    suspend operator fun invoke(entities: List<Entity>) = stateRepository.getEntityOwnerKeys(entities)
}
