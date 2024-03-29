package com.babylon.wallet.android.domain.usecases

import com.babylon.wallet.android.data.repository.state.StateRepository
import com.radixdlt.sargon.AccountAddress
import rdx.works.core.domain.DApp
import javax.inject.Inject

class GetDAppsUseCase @Inject constructor(
    private val stateRepository: StateRepository
) {

    suspend operator fun invoke(
        definitionAddresses: Set<AccountAddress>,
        needMostRecentData: Boolean
    ): Result<List<DApp>> = stateRepository.getDAppsDetails(
        definitionAddresses = definitionAddresses.toList(),
        isRefreshing = needMostRecentData
    )

    suspend operator fun invoke(
        definitionAddress: AccountAddress,
        needMostRecentData: Boolean
    ): Result<DApp> = stateRepository.getDAppsDetails(
        definitionAddresses = listOf(definitionAddress),
        isRefreshing = needMostRecentData
    ).mapCatching { dApps ->
        val dApp = dApps.first()
        dApp
    }
}
