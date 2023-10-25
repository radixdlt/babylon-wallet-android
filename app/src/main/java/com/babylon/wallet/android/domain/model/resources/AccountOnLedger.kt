package com.babylon.wallet.android.domain.model.resources

import com.babylon.wallet.android.domain.model.assets.Assets
import com.babylon.wallet.android.domain.model.assets.LiquidStakeUnit
import com.babylon.wallet.android.domain.model.assets.PoolUnit
import com.babylon.wallet.android.domain.model.assets.StakeClaim
import com.babylon.wallet.android.domain.model.assets.ValidatorDetail
import com.babylon.wallet.android.domain.model.assets.ValidatorWithStakeResources

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
 * @param validators All the validators details involved in this account
 * @param pools All the pools with their pool tokens involved in this account
 */
data class AccountOnLedger(
    val details: AccountDetails,
    private val fungibles: List<Resource.FungibleResource>?,
    private val nonFungibles: List<Resource.NonFungibleResource>?,
    private val validators: List<ValidatorDetail>?,
    private val pools: Map<String, List<Resource.FungibleResource>>?
) {

    // Just two maps that associate stake/claim addresses with validators
    // for more efficient search among the vast amount of fungibles/non-fungibles a user may have.
    private val stakeUnitAddressToValidators = validators?.associate { it.stakeUnitResourceAddress to it }.orEmpty()
    private val claimTokenAddressToValidators = validators?.associate { it.claimTokenResourceAddress to it }.orEmpty()

    private data class ValidatorAssets(
        val liquidStakeUnits: MutableList<LiquidStakeUnit> = mutableListOf(),
        var stakeClaim: StakeClaim? = null
    )

    fun extractAssets() = if (fungibles != null && nonFungibles != null && validators != null && pools != null) {
        val resultingPoolUnits = mutableListOf<PoolUnit>()
        val resultingValidators = mutableMapOf<ValidatorDetail, ValidatorAssets>()

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
                        pool = fungible,
                        items = poolItems
                    )
                )

                fungiblesIterator.remove()
            }

            val validatorDetails = stakeUnitAddressToValidators[fungible.resourceAddress]
            if (validatorDetails != null) {
                val lsu = LiquidStakeUnit(fungible)

                val assetsForValidator = resultingValidators.getOrDefault(validatorDetails, ValidatorAssets())
                assetsForValidator.liquidStakeUnits.add(lsu)
                resultingValidators[validatorDetails] = assetsForValidator

                // Remove this fungible from the list as it will be included as an lsu
                fungiblesIterator.remove()
            }
        }

        val nonFungiblesIterator = resultingNonFungibles.iterator()
        while (nonFungiblesIterator.hasNext()) {
            val nonFungible = nonFungiblesIterator.next()

            val validatorDetails = claimTokenAddressToValidators[nonFungible.resourceAddress]
            if (validatorDetails != null) {
                val assetsForValidator = resultingValidators.getOrDefault(validatorDetails, ValidatorAssets())
                assetsForValidator.stakeClaim = StakeClaim(nonFungible)
                resultingValidators[validatorDetails] = assetsForValidator

                // Remove this non-fungible from the list as it will be included as a stake claim
                nonFungiblesIterator.remove()
            }
        }

        val resultingValidatorsWithStakeResources = resultingValidators.map {
            ValidatorWithStakeResources(
                validatorDetail = it.key,
                liquidStakeUnits = it.value.liquidStakeUnits,
                stakeClaimNft = it.value.stakeClaim
            )
        }
        Assets(
            fungibles = resultingFungibles,
            nonFungibles = resultingNonFungibles,
            poolUnits = resultingPoolUnits,
            validatorsWithStakeResources = resultingValidatorsWithStakeResources
        )
    } else {
        null
    }
}
