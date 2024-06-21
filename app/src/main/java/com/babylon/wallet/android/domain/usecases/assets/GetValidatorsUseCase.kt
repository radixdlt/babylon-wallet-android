package com.babylon.wallet.android.domain.usecases.assets

import com.babylon.wallet.android.data.repository.state.StateRepository
import com.radixdlt.sargon.ValidatorAddress
import javax.inject.Inject

class GetValidatorsUseCase @Inject constructor(private val stateRepository: StateRepository) {

    suspend operator fun invoke(validatorAddresses: Set<ValidatorAddress>) = stateRepository.getValidators(validatorAddresses)
}
