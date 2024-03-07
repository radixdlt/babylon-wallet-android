package com.babylon.wallet.android.domain.usecases.transaction

import com.babylon.wallet.android.data.repository.state.StateRepository
import com.babylon.wallet.android.domain.model.resources.Badge
import javax.inject.Inject

class GetTransactionBadgesUseCase @Inject constructor(
    private val stateRepository: StateRepository
) {

    suspend operator fun invoke(
        accountProofs: Set<String>
    ): List<Badge> = stateRepository.getDAppsDetails(
        definitionAddresses = accountProofs.toList(),
        isRefreshing = false
    ).map { dApps ->
        dApps.map {
            Badge(
                address = it.dAppAddress,
                metadata = it.metadata
            )
        }
    }.getOrNull().orEmpty()
}
