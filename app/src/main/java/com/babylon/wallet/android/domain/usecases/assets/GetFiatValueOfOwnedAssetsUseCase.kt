package com.babylon.wallet.android.domain.usecases.assets

import com.babylon.wallet.android.data.repository.tokenprice.TokenPriceRepository
import com.babylon.wallet.android.domain.model.assets.AccountWithAssets
import com.babylon.wallet.android.domain.model.assets.Asset
import java.math.BigDecimal
import javax.inject.Inject

class GetFiatValueOfOwnedAssetsUseCase @Inject constructor(
    private val tokenPriceRepository: TokenPriceRepository
) {

    suspend operator fun invoke(accountsWithAssets: List<AccountWithAssets>): Map<AccountWithAssets, List<AssetPrice>> {
        tokenPriceRepository.updateTokensPrices()

        val mapOfAccountWithAssetsAndPrices = accountsWithAssets.associateWith { accountWithAssets ->
            val accountPrice = mutableMapOf<Asset, BigDecimal>()

            val tokenAddressesWithAmounts = accountWithAssets.assets
                ?.ownedTokens
                ?.filterNot {
                    it.resource.ownedAmount == null || it.resource.ownedAmount == BigDecimal.ZERO
                }
                ?.associate { token ->
                    token.resource.resourceAddress to (token.resource.ownedAmount)
                }.orEmpty()

            val poolUnitAddressesWithTokenAmounts = accountWithAssets.assets
                ?.ownedPoolUnits?.associateWith { poolUnit ->
                    poolUnit.pool?.resources.orEmpty().associate { poolItem ->
                        poolItem.resourceAddress to (poolUnit.resourceRedemptionValue(poolItem) ?: BigDecimal.ZERO)
                    }
                }.orEmpty()

            val lsuAddressesWithAmounts = accountWithAssets.assets?.liquidStakeUnits?.associate { liquidStakeUnit ->
                liquidStakeUnit.resourceAddress to liquidStakeUnit.resource.ownedAmount
            }.orEmpty()

            val resourcesAddresses = tokenAddressesWithAmounts.keys + poolUnitAddressesWithTokenAmounts.keys.map {
                    poolUnit ->
                poolUnit.resourceAddress
            }

            val priceResult = tokenPriceRepository.getTokensPrices(resourcesAddresses, lsuAddressesWithAmounts.keys)
                .getOrThrow()
                .associateBy { tokenPrice ->
                    tokenPrice.resourceAddress
                }

            val tokensWithPrices = accountWithAssets.assets
                ?.ownedTokens
                ?.filterNot {
                    it.resource.ownedAmount == null || it.resource.ownedAmount == BigDecimal.ZERO
                }
                ?.associateWith { token ->
                    priceResult[token.resource.resourceAddress]?.price ?: BigDecimal.ZERO
                }.orEmpty()
            accountPrice.putAll(tokensWithPrices)

            val poolsWithPrices = accountWithAssets.assets?.poolUnits?.associateWith { poolUnit ->
                poolUnit.pool?.resources.orEmpty().sumOf { poolItem ->
                    priceResult[poolItem.resourceAddress]?.price ?: BigDecimal.ZERO
                }
            }.orEmpty()
            accountPrice.putAll(poolsWithPrices)

            val lsusWithPrices = accountWithAssets.assets?.liquidStakeUnits?.associateWith { lsu ->
                priceResult[lsu.resourceAddress]?.price ?: BigDecimal.ZERO
            }.orEmpty()
            accountPrice.putAll(lsusWithPrices)

            accountPrice.map {
                AssetPrice(
                    asset = it.key,
                    price = it.value
                )
            }
        }

        return mapOfAccountWithAssetsAndPrices
    }

    data class AssetPrice(
        val asset: Asset,
        val price: BigDecimal
    )
}
