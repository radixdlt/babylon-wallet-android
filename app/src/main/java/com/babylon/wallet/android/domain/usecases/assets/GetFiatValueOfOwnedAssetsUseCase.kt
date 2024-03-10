package com.babylon.wallet.android.domain.usecases.assets

import com.babylon.wallet.android.data.repository.state.StateRepository
import com.babylon.wallet.android.data.repository.tokenprice.TokenPriceRepository
import com.babylon.wallet.android.domain.model.assets.AccountWithAssets
import com.babylon.wallet.android.domain.model.assets.Asset
import com.babylon.wallet.android.domain.model.assets.TokenPrice
import com.babylon.wallet.android.domain.model.resources.XrdResource
import com.babylon.wallet.android.domain.usecases.GetNetworkInfoUseCase
import rdx.works.profile.derivation.model.NetworkId
import java.math.BigDecimal
import javax.inject.Inject

class GetFiatValueOfOwnedAssetsUseCase @Inject constructor(
    private val tokenPriceRepository: TokenPriceRepository,
    private val stateRepository: StateRepository,
    private val getNetworkInfoUseCase: GetNetworkInfoUseCase
) {

    suspend operator fun invoke(accountsWithAssets: List<AccountWithAssets>): Map<AccountWithAssets, List<AssetPrice>> {
        fetchClaimStakesForAccounts(accountsWithAssets)
        tokenPriceRepository.updateTokensPrices()

        val mapOfAccountsWithAssetsAndFiatValue = accountsWithAssets.associateWith { accountWithAssets ->
            val tokenAddressesWithAmounts = accountWithAssets.assets
                ?.ownedTokens
                ?.associate { token ->
                    token.resource.resourceAddress to (token.resource.ownedAmount)
                }.orEmpty()

            val poolUnitAndItemsAddressesWithAmounts = accountWithAssets.assets
                ?.ownedPoolUnits?.associateWith { poolUnit ->
                    poolUnit.pool?.resources.orEmpty().associate { poolItem ->
                        poolItem.resourceAddress to (poolUnit.resourceRedemptionValue(poolItem) ?: BigDecimal.ZERO)
                    }
                }.orEmpty()

            val lsuAddressesWithAmounts = accountWithAssets.assets?.liquidStakeUnits?.associate { liquidStakeUnit ->
                liquidStakeUnit.resourceAddress to liquidStakeUnit.resource.ownedAmount
            }.orEmpty()

            val poolItemsAddresses = poolUnitAndItemsAddressesWithAmounts.values.flatMap { poolItemAddressAndAmount ->
                poolItemAddressAndAmount.keys
            }
            // build a list of resources addresses which consists of token and pool items addresses
            val resourcesAddresses = tokenAddressesWithAmounts.keys + poolItemsAddresses
            // and pass this as an argument to the getTokensPrices along with the lsus addresses
            val priceResult = getTokensPricesForResources(resourcesAddresses, lsuAddressesWithAmounts.keys)

            val accountAssetsWithFiatValue = mutableMapOf<Asset, BigDecimal>()

            val tokensWithFiatValue = accountWithAssets.assets
                ?.ownedTokens
                ?.associateWith { token ->
                    val priceForResource = priceResult[token.resource.resourceAddress]?.price ?: BigDecimal.ZERO
                    priceForResource.multiply(token.resource.ownedAmount ?: BigDecimal.ZERO)
                }.orEmpty()

            val poolUnitsWithFiatValue = accountWithAssets.assets?.poolUnits?.associateWith { poolUnit ->
                val itemsPrices = poolUnitAndItemsAddressesWithAmounts[poolUnit]?.map { poolItemAddressAndAmountMap ->
                    val priceForResource = priceResult[poolItemAddressAndAmountMap.key]?.price ?: BigDecimal.ZERO
                    priceForResource.multiply(poolItemAddressAndAmountMap.value)
                }
                val poolUnitTotalPrice = itemsPrices?.sumOf { it } ?: BigDecimal.ZERO
                poolUnitTotalPrice
            }.orEmpty()

            val lsusWithFiatValue = accountWithAssets.assets?.liquidStakeUnits?.associateWith { lsu ->
                val priceForLSU = priceResult[lsu.resourceAddress]?.price ?: BigDecimal.ZERO
                priceForLSU.multiply(lsu.fungibleResource.ownedAmount ?: BigDecimal.ZERO)
            }.orEmpty()

            val xrdAddress = XrdResource.address(networkId = NetworkId.from(accountWithAssets.account.networkID))
            val claimsWithFiatValue = accountWithAssets.assets?.stakeClaims?.associateWith { stakeClaim ->
                val totalItemXRD = stakeClaim.nonFungibleResource.items.sumOf {
                    it.claimAmountXrd ?: BigDecimal.ZERO
                }
                totalItemXRD * (priceResult[xrdAddress]?.price ?: BigDecimal.ZERO)
            }.orEmpty()

            accountAssetsWithFiatValue.putAll(tokensWithFiatValue + lsusWithFiatValue + poolUnitsWithFiatValue + claimsWithFiatValue)

            accountAssetsWithFiatValue.map {
                AssetPrice(
                    asset = it.key,
                    price = it.value
                )
            }
        }

        return mapOfAccountsWithAssetsAndFiatValue
    }

    private suspend fun getTokensPricesForResources(
        resourcesAddresses: Set<String>,
        lsuAddresses: Set<String>
    ): Map<String, TokenPrice> {
        return tokenPriceRepository.getTokensPrices(
            resourcesAddresses = resourcesAddresses,
            lsusAddresses = lsuAddresses
        )
            .getOrThrow()
            .associateBy { tokenPrice ->
                tokenPrice.resourceAddress
            }
    }

    private suspend fun fetchClaimStakesForAccounts(accountsWithAssets: List<AccountWithAssets>) {
        getNetworkInfoUseCase()
        accountsWithAssets.map { accountWithAssets ->
            val account = accountWithAssets.account
            val ownedValidatorsWithStakes = accountWithAssets.assets?.ownedValidatorsWithStakes ?: return
            val areAnyUnknownLSUs = ownedValidatorsWithStakes.any {
                    validatorWithStakes ->
                !validatorWithStakes.isDetailsAvailable
            }
            if (areAnyUnknownLSUs) {
                stateRepository.updateLSUsInfo(
                    account = account,
                    validatorsWithStakes = ownedValidatorsWithStakes
                )
            }
        }
    }

    data class AssetPrice(
        val asset: Asset,
        val price: BigDecimal
    )
}
