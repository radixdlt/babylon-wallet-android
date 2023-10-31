package com.babylon.wallet.android.domain.model.resources

import com.babylon.wallet.android.domain.model.assets.Assets
import com.babylon.wallet.android.domain.model.assets.LiquidStakeUnit
import com.babylon.wallet.android.domain.model.assets.PoolUnit
import com.babylon.wallet.android.domain.model.assets.StakeClaim
import com.babylon.wallet.android.domain.model.assets.ValidatorDetail
import com.babylon.wallet.android.domain.model.assets.ValidatorWithStakes

/**
 * Account data as received from gateway, or from database cache.
 *
 * It is just a medium to transfer all known data for a given account from the repository
 * to the domain layer. There is only one more step ([extractAssets]) that "translates" these data to pure [Assets]
 * since some of the [fungibles] or [nonFungibles] are involved in stakes. This is the reason that most of the parameters
 * are private, since they are unusable in this state by the app.
 *
 * @param details The details associated with this account
 * @param fungibles All the actual fungible assets and the stake units
 * @param nonFungibles All the actual non-fungible assets and the stake claims
 */
data class AccountOnLedger(
    val details: AccountDetails,
    private val fungibles: List<Resource.FungibleResource>?,
    private val nonFungibles: List<Resource.NonFungibleResource>?
) {

    fun extractAssets(validators: List<ValidatorDetail>, pools: Map<String, List<Resource.FungibleResource>>): Assets? {
        val stakeUnitAddressToValidators = validators.associateBy { it.stakeUnitResourceAddress }
        val claimTokenAddressToValidators = validators.associateBy { it.claimTokenResourceAddress }
        return if (fungibles != null && nonFungibles != null) {
            val resultingPoolUnits = mutableListOf<PoolUnit>()
            val resultingStakeUnits = mutableMapOf<ValidatorDetail, ValidatorWithStakes>()

            val resultingFungibles = fungibles.toMutableList()
            val resultingNonFungibles = nonFungibles.toMutableList()

            val poolAddresses = pools.keys
            val fungiblesIterator = resultingFungibles.iterator()
            while (fungiblesIterator.hasNext()) {
                val fungible = fungiblesIterator.next()

                if (fungible.poolAddress in poolAddresses) {
                    val poolItems = pools[fungible.poolAddress].orEmpty()

                    resultingPoolUnits.add(
                        PoolUnit(
                            stake = fungible,
                            pool = Pool(fungible.poolAddress.orEmpty(), poolItems),
                        )
                    )

                    fungiblesIterator.remove()
                }

                val validatorDetails = stakeUnitAddressToValidators[fungible.resourceAddress]
                if (validatorDetails != null) {
                    val lsu = LiquidStakeUnit(fungible)

                    resultingStakeUnits[validatorDetails] = ValidatorWithStakes(
                        validatorDetail = validatorDetails,
                        liquidStakeUnit = lsu
                    )

                    // Remove this fungible from the list as it will be included as an lsu
                    fungiblesIterator.remove()
                }
            }

            val nonFungiblesIterator = resultingNonFungibles.iterator()
            while (nonFungiblesIterator.hasNext()) {
                val nonFungible = nonFungiblesIterator.next()

                val validatorDetails = claimTokenAddressToValidators[nonFungible.resourceAddress]
                if (validatorDetails != null) {
                    resultingStakeUnits[validatorDetails]?.copy(stakeClaimNft = StakeClaim(nonFungible))?.let {
                        resultingStakeUnits[validatorDetails] = it
                    }

                    // Remove this non-fungible from the list as it will be included as a stake claim
                    nonFungiblesIterator.remove()
                }
            }

            val resultingValidatorsWithStakeResources = resultingStakeUnits.map {
                ValidatorWithStakes(
                    validatorDetail = it.key,
                    liquidStakeUnit = it.value.liquidStakeUnit,
                    stakeClaimNft = it.value.stakeClaimNft
                )
            }
            Assets(
                fungibles = resultingFungibles,
                nonFungibles = resultingNonFungibles,
                poolUnits = resultingPoolUnits,
                validatorsWithStakes = resultingValidatorsWithStakeResources
            )
        } else {
            null
        }
    }
}
