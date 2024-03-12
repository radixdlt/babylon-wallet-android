package com.babylon.wallet.android.domain.usecases.assets

import com.babylon.wallet.android.data.repository.state.StateRepository
import com.babylon.wallet.android.data.repository.tokenprice.TokenPriceRepository
import com.babylon.wallet.android.data.repository.tokenprice.TokenPriceRepository.PriceRequestAddress
import com.babylon.wallet.android.domain.model.assets.AccountWithAssets
import com.babylon.wallet.android.domain.model.assets.Asset
import com.babylon.wallet.android.domain.model.assets.AssetPrice
import com.babylon.wallet.android.domain.model.assets.LiquidStakeUnit
import com.babylon.wallet.android.domain.model.assets.NonFungibleCollection
import com.babylon.wallet.android.domain.model.assets.PoolUnit
import com.babylon.wallet.android.domain.model.assets.StakeClaim
import com.babylon.wallet.android.domain.model.assets.Token
import com.babylon.wallet.android.domain.model.assets.TokenPrice
import com.babylon.wallet.android.domain.model.resources.XrdResource
import rdx.works.core.then
import rdx.works.profile.derivation.model.NetworkId
import java.math.BigDecimal
import javax.inject.Inject

class GetFiatValueUseCase @Inject constructor(
    private val tokenPriceRepository: TokenPriceRepository,
    private val stateRepository: StateRepository
) {

    suspend fun forAccount(accountWithAssets: AccountWithAssets): Result<List<AssetPrice>> = runCatching {
        val networkId = NetworkId.from(accountWithAssets.account.networkID)
        accountWithAssets.assets?.ownedTokens?.map { it.priceRequestAddresses(networkId) }?.flatten().orEmpty() +
            accountWithAssets.assets?.ownedLiquidStakeUnits?.map { it.priceRequestAddresses(networkId) }?.flatten().orEmpty() +
            accountWithAssets.assets?.ownedPoolUnits?.map { it.priceRequestAddresses(networkId) }?.flatten().orEmpty() +
            accountWithAssets.assets?.ownedStakeClaims?.map { it.priceRequestAddresses(networkId) }?.flatten().orEmpty()
    }.then { addresses ->
        tokenPriceRepository.getTokensPrices(addresses = addresses.toSet())
    }.mapCatching { tokenPrices ->
        tokenPrices.associateBy { it.resourceAddress }
    }.mapCatching { prices ->
        val networkId = NetworkId.from(accountWithAssets.account.networkID)
        accountWithAssets.assets
            ?.ownedTokens
            ?.mapNotNull { it.price(prices, networkId) }.orEmpty() +
            accountWithAssets.assets
                ?.ownedLiquidStakeUnits
                ?.mapNotNull { it.price(prices, networkId) }.orEmpty() +
            accountWithAssets.assets
                ?.ownedPoolUnits
                ?.mapNotNull { it.price(prices, networkId) }.orEmpty() +
            accountWithAssets.assets
                ?.ownedStakeClaims
                ?.mapNotNull { it.price(prices, networkId) }.orEmpty()
    }

    suspend fun forAsset(asset: Asset, networkId: NetworkId): Result<AssetPrice?> = runCatching {
        asset.priceRequestAddresses(networkId)
    }.then { addresses ->
        tokenPriceRepository.getTokensPrices(addresses = addresses.toSet())
    }.mapCatching { tokenPrices ->
        tokenPrices.associateBy { it.resourceAddress }
    }.mapCatching { prices ->
        asset.price(prices, networkId)
    }

    private fun Asset.price(tokenPrices: Map<String, TokenPrice>, networkId: NetworkId): AssetPrice? = when (this) {
        is Token -> {
            val tokenPrice = tokenPrices[resource.resourceAddress]
            val totalPrice = tokenPrice?.price?.multiply(resource.ownedAmount ?: BigDecimal.ZERO)

            AssetPrice.TokenPrice(
                asset = this,
                price = totalPrice,
                currencyCode = tokenPrice?.currency
            )
        }

        is LiquidStakeUnit -> {
            val tokenPrice = tokenPrices[resourceAddress]
            val priceForLSU = tokenPrice?.price ?: BigDecimal.ZERO
            val totalPrice = priceForLSU.multiply(fungibleResource.ownedAmount ?: BigDecimal.ZERO)
            AssetPrice.LSUPrice(
                asset = this,
                price = totalPrice,
                currencyCode = tokenPrice?.currency
            )
        }

        is PoolUnit -> {
            val poolItemPrices = pool?.resources?.associateWith { poolItem ->
                val priceForResource = tokenPrices[poolItem.resourceAddress]?.price
                priceForResource?.multiply(resourceRedemptionValue(poolItem))
            }.orEmpty()

            val currencyIndicator = pool?.resources?.firstOrNull()?.let { firstResource ->
                tokenPrices[firstResource.resourceAddress]?.currency
            }

            AssetPrice.PoolUnitPrice(
                asset = this,
                prices = poolItemPrices,
                currencyCode = currencyIndicator
            )
        }

        is StakeClaim -> {
            val xrdAddress = XrdResource.address(networkId = networkId)

            val totalItemXRD = nonFungibleResource.items.sumOf { it.claimAmountXrd ?: BigDecimal.ZERO }
            val totalPrice = totalItemXRD * (tokenPrices[xrdAddress]?.price ?: BigDecimal.ZERO)
            AssetPrice.StakeClaimPrice(
                asset = this,
                price = totalPrice,
                currencyCode = tokenPrices[xrdAddress]?.currency
            )
        }

        is NonFungibleCollection -> null
    }

    private fun Asset.priceRequestAddresses(networkId: NetworkId): List<PriceRequestAddress> = when (this) {
        is NonFungibleCollection -> emptyList()
        is LiquidStakeUnit -> listOf(PriceRequestAddress.LSU(address = resourceAddress))
        is StakeClaim -> {
            listOf(PriceRequestAddress.Regular(XrdResource.address(networkId = networkId)))
        }

        is PoolUnit -> pool?.resources?.map { PriceRequestAddress.Regular(it.resourceAddress) }.orEmpty()
        is Token -> listOf(PriceRequestAddress.Regular(address = resource.resourceAddress))
    }
}
