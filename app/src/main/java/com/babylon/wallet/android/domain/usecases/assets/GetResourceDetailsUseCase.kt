package com.babylon.wallet.android.domain.usecases.assets

import com.babylon.wallet.android.data.repository.state.StateRepository
import com.babylon.wallet.android.domain.model.resources.Resource
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetResourceDetailsUseCase @Inject constructor(
    private val stateRepository: StateRepository
) {

    operator fun invoke(resourceAddress: String, accountAddress: String?): Flow<Resource> =
        stateRepository.observeResourceDetails(resourceAddress, accountAddress)

}
