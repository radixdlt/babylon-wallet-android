package com.babylon.wallet.android.domain.usecases.transaction

import com.babylon.wallet.android.data.repository.state.StateRepository
import com.babylon.wallet.android.domain.model.resources.Badge
import com.radixdlt.ret.Address
import javax.inject.Inject

class GetTransactionBadgesUseCase @Inject constructor(
    private val stateRepository: StateRepository
) {

    suspend operator fun invoke(
        accountProofs: List<Address>
    ): List<Badge> = stateRepository.getDAppsDetails(
        definitionAddresses = accountProofs.map { it.addressString() },
        skipCache = false
    ).map { dApps ->
        dApps.map {
            Badge(
                address = it.dAppAddress,
                metadata = it.metadata
            )
        }
    }.getOrNull().orEmpty()
}
