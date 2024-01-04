package com.babylon.wallet.android.domain.usecases

import com.babylon.wallet.android.data.repository.state.StateRepository
import com.babylon.wallet.android.domain.model.DApp
import javax.inject.Inject

class GetDAppUseCase @Inject constructor(
    private val stateRepository: StateRepository
) {

    suspend operator fun invoke(
        definitionAddress: String,
        needMostRecentData: Boolean
    ): Result<DApp> = stateRepository.getDAppsDetails(
        definitionAddresses = listOf(definitionAddress),
        skipCache = needMostRecentData
    ).mapCatching { dApps ->
        val dApp = dApps.first()
        dApp
    }
}
