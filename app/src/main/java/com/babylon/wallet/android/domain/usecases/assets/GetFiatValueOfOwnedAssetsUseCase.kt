package com.babylon.wallet.android.domain.usecases.assets

import com.babylon.wallet.android.data.repository.state.StateRepository
import com.babylon.wallet.android.data.repository.tokenprice.TokenPriceRepository
import com.babylon.wallet.android.domain.model.assets.AccountWithAssets
import com.babylon.wallet.android.domain.model.assets.AssetPrice
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

            val poolUnitAndItemsWithAmounts = accountWithAssets.assets
                ?.ownedPoolUnits?.associateWith { poolUnit ->
                    poolUnit.pool?.resources.orEmpty().associate { poolItem ->
                        poolItem to (poolUnit.resourceRedemptionValue(poolItem) ?: BigDecimal.ZERO)
                    }
                }.orEmpty()

            val lsuAddressesWithAmounts = accountWithAssets.assets?.liquidStakeUnits?.associate { liquidStakeUnit ->
                liquidStakeUnit.resourceAddress to liquidStakeUnit.resource.ownedAmount
            }.orEmpty()

            val poolItemsAddresses = poolUnitAndItemsWithAmounts.values.flatMap { poolItemAddressAndAmount ->
                poolItemAddressAndAmount.keys.map { fungibleResource ->
                    fungibleResource.resourceAddress
                }
            }
            // build a list of resources addresses which consists of token and pool items addresses
            val resourcesAddresses = tokenAddressesWithAmounts.keys + poolItemsAddresses
            // and pass this as an argument to the getTokensPrices along with the lsus addresses
            val priceResult = getTokensPricesForResources(resourcesAddresses, lsuAddressesWithAmounts.keys)

            val accountAssetsWithFiatValue = mutableListOf<AssetPrice>()

            val tokensWithFiatValue = accountWithAssets.assets
                ?.ownedTokens
                ?.map { token ->
                    val priceForResource = priceResult[token.resource.resourceAddress]?.price
                    val totalPrice = priceForResource?.multiply(token.resource.ownedAmount ?: BigDecimal.ZERO)
                    AssetPrice.TokenPrice(
                        asset = token,
                        price = totalPrice
                    )
                }.orEmpty()

            val poolUnitsWithFiatValue = accountWithAssets.assets?.poolUnits?.map { poolUnit ->
                val itemsPrices = poolUnitAndItemsWithAmounts[poolUnit]?.map { poolItemAndAmountMap ->
                    val priceForResource = priceResult[poolItemAndAmountMap.key.resourceAddress]?.price ?: BigDecimal.ZERO
                    poolItemAndAmountMap.key to priceForResource.multiply(poolItemAndAmountMap.value)
                }
                AssetPrice.PoolUnitPrice(
                    asset = poolUnit,
                    prices = itemsPrices?.associate {
                        it
                    }.orEmpty()
                )
            }.orEmpty()

            val lsusWithFiatValue = accountWithAssets.assets?.liquidStakeUnits?.map { lsu ->
                val priceForLSU = priceResult[lsu.resourceAddress]?.price ?: BigDecimal.ZERO
                val totalPrice = priceForLSU.multiply(lsu.fungibleResource.ownedAmount ?: BigDecimal.ZERO)
                AssetPrice.LSUPrice(
                    asset = lsu,
                    price = totalPrice
                )
            }.orEmpty()

            val xrdAddress = XrdResource.address(networkId = NetworkId.from(accountWithAssets.account.networkID))
            val claimsWithFiatValue = accountWithAssets.assets?.stakeClaims?.map { stakeClaim ->
                val totalItemXRD = stakeClaim.nonFungibleResource.items.sumOf {
                    it.claimAmountXrd ?: BigDecimal.ZERO
                }
                val totalPrice = totalItemXRD * (priceResult[xrdAddress]?.price ?: BigDecimal.ZERO)
                AssetPrice.StakeClaimPrice(
                    asset = stakeClaim,
                    price = totalPrice
                )
            }.orEmpty()

            accountAssetsWithFiatValue.addAll(tokensWithFiatValue + lsusWithFiatValue + poolUnitsWithFiatValue + claimsWithFiatValue)
            accountAssetsWithFiatValue
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
}
