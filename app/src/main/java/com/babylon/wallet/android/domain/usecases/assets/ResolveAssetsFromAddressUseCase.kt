package com.babylon.wallet.android.domain.usecases.assets

import com.babylon.wallet.android.data.repository.state.StateRepository
import com.babylon.wallet.android.domain.model.assets.Asset
import com.babylon.wallet.android.domain.model.assets.LiquidStakeUnit
import com.babylon.wallet.android.domain.model.assets.NonFungibleCollection
import com.babylon.wallet.android.domain.model.assets.PoolUnit
import com.babylon.wallet.android.domain.model.assets.StakeClaim
import com.babylon.wallet.android.domain.model.assets.Token
import com.babylon.wallet.android.domain.model.assets.ValidatorDetail
import com.babylon.wallet.android.domain.model.resources.Pool
import com.babylon.wallet.android.domain.model.resources.Resource
import javax.inject.Inject

class ResolveAssetsFromAddressUseCase @Inject constructor(
    private val stateRepository: StateRepository
) {
    suspend operator fun invoke(
        fungibleAddresses: Set<String>,
        nonFungibleIds: Map<String, Set<String>>
    ): Result<List<Asset>> = stateRepository
        .getResources(
            addresses = fungibleAddresses + nonFungibleIds.keys,
            underAccountAddress = null,
            withDetails = true
        ).mapCatching { resources ->
            val nfts = nonFungibleIds.mapValues { entry ->
                stateRepository.getNFTDetails(entry.key, entry.value.toSet()).getOrThrow()
            }

            val fungibles = resources.filterIsInstance<Resource.FungibleResource>()
            val nonFungibles = resources.filterIsInstance<Resource.NonFungibleResource>().map { collection ->
                collection.copy(items = nfts[collection.resourceAddress].orEmpty())
            }

            val poolAddresses = fungibles.mapNotNull { it.poolAddress }.toSet()
            val validatorAddresses = (fungibles.mapNotNull { it.validatorAddress } + nonFungibles.mapNotNull { it.validatorAddress })
                .toSet()

            val pools = stateRepository.getPools(poolAddresses = poolAddresses).getOrThrow().associateBy { it.address }
            val validators = stateRepository.getValidators(validatorAddresses = validatorAddresses).getOrThrow().associateBy { it.address }

            fungibles.map {
                it.asAsset(pools = pools, validators = validators)
            } + nonFungibles.map {
                it.asAsset(pools = pools, validators = validators)
            }
        }

    private fun Resource.asAsset(pools: Map<String, Pool>, validators: Map<String, ValidatorDetail>): Asset = when (this) {
        is Resource.FungibleResource -> {
            if (poolAddress?.isNotEmpty() == true) {
                val pool = pools[poolAddress] ?: error("Pool not found")
                PoolUnit(
                    stake = this,
                    pool = pool
                )
            } else if (validatorAddress?.isNotEmpty() == true) {
                val validator = validators[validatorAddress] ?: error("Validator not found")
                LiquidStakeUnit(
                    fungibleResource = this,
                    validator = validator
                )
            } else {
                Token(resource = this)
            }
        }

        is Resource.NonFungibleResource -> {
            if (validatorAddress.isNullOrEmpty()) {
                NonFungibleCollection(
                    collection = this
                )
            } else {
                val validator = validators[validatorAddress] ?: error("Validator not found")
                StakeClaim(
                    nonFungibleResource = this,
                    validator = validator
                )
            }
        }
    }
}
