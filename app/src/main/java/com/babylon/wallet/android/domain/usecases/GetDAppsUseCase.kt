package com.babylon.wallet.android.domain.usecases

import com.babylon.wallet.android.data.repository.state.StateRepository
import com.babylon.wallet.android.domain.model.DApp
import javax.inject.Inject

class GetDAppsUseCase @Inject constructor(
    private val stateRepository: StateRepository
) {

    suspend operator fun invoke(
        definitionAddresses: Set<String>,
        needMostRecentData: Boolean
    ): Result<List<DApp>> = stateRepository.getDAppsDetails(
        definitionAddresses = definitionAddresses.toList(),
        skipCache = needMostRecentData
    )

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
