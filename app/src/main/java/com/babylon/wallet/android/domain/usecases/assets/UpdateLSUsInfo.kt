package com.babylon.wallet.android.domain.usecases.assets

import com.babylon.wallet.android.data.repository.state.StateRepository
import com.babylon.wallet.android.domain.model.assets.ValidatorWithStakes
import rdx.works.profile.data.model.pernetwork.Network
import javax.inject.Inject

class UpdateLSUsInfo @Inject constructor(
    private val repository: StateRepository
) {

    suspend operator fun invoke(account: Network.Account, validatorsWithStakes: List<ValidatorWithStakes>) =
        repository.updateLSUsInfo(account, validatorsWithStakes)
}
