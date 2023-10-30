package com.babylon.wallet.android.domain.usecases.assets

import com.babylon.wallet.android.data.repository.state.StateRepository
import rdx.works.profile.data.model.pernetwork.Network
import java.math.BigDecimal
import javax.inject.Inject

class GetXrdForAccountsUseCase @Inject constructor(
    private val stateRepository: StateRepository
) {

    suspend operator fun invoke(accounts: List<Network.Account>): Result<Map<Network.Account, BigDecimal>> =
        stateRepository.getOwnedXRD(accounts)

}
