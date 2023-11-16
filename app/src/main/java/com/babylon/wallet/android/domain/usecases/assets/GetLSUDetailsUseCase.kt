package com.babylon.wallet.android.domain.usecases.assets

import com.babylon.wallet.android.data.repository.state.StateRepository
import com.babylon.wallet.android.di.coroutines.DefaultDispatcher
import com.babylon.wallet.android.domain.model.assets.LiquidStakeUnit
import com.babylon.wallet.android.domain.model.assets.PoolUnit
import com.babylon.wallet.android.domain.model.assets.ValidatorWithStakes
import com.babylon.wallet.android.domain.model.resources.Resource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
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
            stateRepository.getValidator(validatorAddress = validatorAddress, accountAddress = accountAddress).map { validator ->
                ValidatorWithStakes(
                    validatorDetail = validator,
                    liquidStakeUnit = LiquidStakeUnit(stake)
                )
            }
        }
}
