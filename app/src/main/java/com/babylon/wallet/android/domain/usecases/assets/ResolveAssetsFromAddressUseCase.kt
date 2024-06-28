package com.babylon.wallet.android.domain.usecases.assets

import com.babylon.wallet.android.data.repository.state.StateRepository
import com.radixdlt.sargon.NonFungibleLocalId
import com.radixdlt.sargon.PoolAddress
import com.radixdlt.sargon.ResourceAddress
import com.radixdlt.sargon.ValidatorAddress
import rdx.works.core.domain.assets.Asset
import rdx.works.core.domain.assets.LiquidStakeUnit
import rdx.works.core.domain.assets.NonFungibleCollection
import rdx.works.core.domain.assets.PoolUnit
import rdx.works.core.domain.assets.StakeClaim
import rdx.works.core.domain.assets.Token
import rdx.works.core.domain.resources.Pool
import rdx.works.core.domain.resources.Resource
import rdx.works.core.domain.resources.Validator
import javax.inject.Inject

class ResolveAssetsFromAddressUseCase @Inject constructor(
    private val stateRepository: StateRepository
) {
    suspend operator fun invoke(
        fungibleAddresses: Set<ResourceAddress>,
        nonFungibleIds: Map<ResourceAddress, Set<NonFungibleLocalId>>,
        withAllMetadata: Boolean = false
    ): Result<List<Asset>> = stateRepository
        .getResources(
            addresses = fungibleAddresses + nonFungibleIds.keys,
            underAccountAddress = null,
            withDetails = true,
            withAllMetadata = withAllMetadata
        ).mapCatching { resources ->
            val nfts = nonFungibleIds.mapValues { entry ->
                stateRepository.getNFTDetails(entry.key, entry.value.toSet()).getOrThrow()
            }

            val fungibles = resources.filterIsInstance<Resource.FungibleResource>()
            val nonFungibles = resources.filterIsInstance<Resource.NonFungibleResource>().map { collection ->
                collection.copy(items = nfts[collection.address].orEmpty())
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

    private fun Resource.asAsset(pools: Map<PoolAddress, Pool>, validators: Map<ValidatorAddress, Validator>): Asset = when (this) {
        is Resource.FungibleResource -> {
            if (poolAddress != null) {
                val pool = pools[poolAddress] ?: error("Pool not found")
                PoolUnit(
                    stake = this,
                    pool = pool
                )
            } else if (validatorAddress != null) {
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
            if (validatorAddress == null) {
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
