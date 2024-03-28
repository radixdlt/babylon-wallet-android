package com.babylon.wallet.android.domain.usecases.assets

import com.babylon.wallet.android.data.repository.state.StateRepository
import rdx.works.core.domain.assets.LiquidStakeUnit
import rdx.works.core.domain.assets.ValidatorWithStakes
import rdx.works.core.domain.resources.Resource
import rdx.works.core.then
import java.lang.RuntimeException
import javax.inject.Inject

/**
 * Returns the LSU with validator details. Currently this use case does not return the associated claims.
 */
class GetLSUDetailsUseCase @Inject constructor(
    private val stateRepository: StateRepository
) {

    suspend operator fun invoke(resourceAddress: String, accountAddress: String): Result<ValidatorWithStakes> =
        stateRepository.getResources(
            addresses = setOf(resourceAddress),
            underAccountAddress = accountAddress,
            withDetails = true
        ).mapCatching {
            it.first() as Resource.FungibleResource
        }.then { stake ->
            val validatorAddress = stake.validatorAddress
                ?: return@then Result.failure(RuntimeException("Resource $resourceAddress has no associated validator"))
            stateRepository.getValidators(validatorAddresses = setOf(validatorAddress)).mapCatching { validators ->
                val validator = validators.first()
                ValidatorWithStakes(
                    validatorDetail = validator,
                    liquidStakeUnit = LiquidStakeUnit(stake, validator)
                )
            }
        }
}
