package com.babylon.wallet.android.domain.usecases.assets

import com.babylon.wallet.android.data.repository.state.StateRepository
import com.radixdlt.sargon.PoolAddress
import javax.inject.Inject

class GetPoolsUseCase @Inject constructor(private val stateRepository: StateRepository) {

    suspend operator fun invoke(poolAddresses: Set<PoolAddress>) = stateRepository.getPools(poolAddresses)

}