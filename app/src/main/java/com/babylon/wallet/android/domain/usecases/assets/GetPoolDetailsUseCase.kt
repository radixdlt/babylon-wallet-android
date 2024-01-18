package com.babylon.wallet.android.domain.usecases.assets

import com.babylon.wallet.android.data.repository.state.StateRepository
import com.babylon.wallet.android.domain.model.resources.Pool
import javax.inject.Inject

class GetPoolDetailsUseCase @Inject constructor(
    private val stateRepository: StateRepository
) {

    suspend operator fun invoke(poolAddresses: Set<String>): Result<List<Pool>> =
        stateRepository.getPools(poolAddresses)
}
