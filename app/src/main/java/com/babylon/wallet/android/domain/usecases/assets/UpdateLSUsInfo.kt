package com.babylon.wallet.android.domain.usecases.assets

import com.babylon.wallet.android.data.repository.state.StateRepository
import com.radixdlt.sargon.Account
import rdx.works.core.domain.assets.ValidatorWithStakes
import javax.inject.Inject

class UpdateLSUsInfo @Inject constructor(
    private val repository: StateRepository
) {

    suspend operator fun invoke(account: Account, validatorsWithStakes: List<ValidatorWithStakes>) =
        repository.updateLSUsInfo(account, validatorsWithStakes)
}
