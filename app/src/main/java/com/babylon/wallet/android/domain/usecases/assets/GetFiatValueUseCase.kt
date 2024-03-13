package com.babylon.wallet.android.domain.usecases.assets

import com.babylon.wallet.android.data.repository.state.StateRepository
import com.babylon.wallet.android.data.repository.tokenprice.FiatPriceRepository
import com.babylon.wallet.android.data.repository.tokenprice.FiatPriceRepository.PriceRequestAddress
import com.babylon.wallet.android.domain.model.assets.AccountWithAssets
import com.babylon.wallet.android.domain.model.assets.Asset
import com.babylon.wallet.android.domain.model.assets.AssetPrice
import com.babylon.wallet.android.domain.model.assets.FiatPrice
import com.babylon.wallet.android.domain.model.assets.LiquidStakeUnit
import com.babylon.wallet.android.domain.model.assets.NonFungibleCollection
import com.babylon.wallet.android.domain.model.assets.PoolUnit
import com.babylon.wallet.android.domain.model.assets.StakeClaim
import com.babylon.wallet.android.domain.model.assets.SupportedCurrency
import com.babylon.wallet.android.domain.model.assets.Token
import com.babylon.wallet.android.domain.model.resources.XrdResource
import rdx.works.core.then
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.derivation.model.NetworkId
import java.math.BigDecimal
import javax.inject.Inject

class GetFiatValueUseCase @Inject constructor(
    private val fiatPriceRepository: FiatPriceRepository,
    private val stateRepository: StateRepository
) {

    suspend fun forAccount(
        accountWithAssets: AccountWithAssets,
        currency: SupportedCurrency = SupportedCurrency.USD
    ): Result<List<AssetPrice>> {
        val networkId = NetworkId.from(accountWithAssets.account.networkID)
        return runCatching {
            accountWithAssets.assets?.ownedTokens?.map { it.priceRequestAddresses(networkId) }?.flatten().orEmpty() +
                accountWithAssets.assets?.ownedLiquidStakeUnits?.map { it.priceRequestAddresses(networkId) }?.flatten().orEmpty() +
                accountWithAssets.assets?.ownedPoolUnits?.map { it.priceRequestAddresses(networkId) }?.flatten().orEmpty() +
                accountWithAssets.assets?.ownedStakeClaims?.map { it.priceRequestAddresses(networkId) }?.flatten().orEmpty()
        }.then { addresses ->
            fiatPriceRepository.getFiatPrices(addresses = addresses.toSet(), currency = currency)
        }.mapCatching { prices ->
            // ensure that all claims are fetched
            val claims = if (accountWithAssets.assets?.ownedStakeClaims?.isNotEmpty() == true) {
                stateRepository.updateStakeClaims(
                    account = accountWithAssets.account,
                    claims = accountWithAssets.assets.ownedStakeClaims
                ).getOrThrow()
            } else {
                accountWithAssets.assets?.ownedStakeClaims
            }

            accountWithAssets.assets
                ?.ownedTokens
                ?.mapNotNull { it.price(prices, networkId, currency) }.orEmpty() +
                accountWithAssets.assets
                    ?.ownedLiquidStakeUnits
                    ?.mapNotNull { it.price(prices, networkId, currency) }.orEmpty() +
                accountWithAssets.assets
                    ?.ownedPoolUnits
                    ?.mapNotNull { it.price(prices, networkId, currency) }.orEmpty() +
                claims?.mapNotNull { it.price(prices, networkId, currency) }.orEmpty()
        }
    }

    suspend fun forAsset(
        asset: Asset,
        account: Network.Account,
        currency: SupportedCurrency = SupportedCurrency.USD
    ): Result<AssetPrice?> {
        val networkId = NetworkId.from(account.networkID)

        return runCatching {
            asset.priceRequestAddresses(networkId)
        }.then { addresses ->
            fiatPriceRepository.getFiatPrices(addresses = addresses.toSet(), currency = currency)
        }.mapCatching { prices ->
            // ensure that all claims are fetched
            val ensured = if (asset is StakeClaim) {
                stateRepository.updateStakeClaims(
                    account = account,
                    claims = listOf(asset)
                ).getOrThrow().first()
            } else {
                asset
            }

            ensured.price(prices, networkId, currency)
        }
    }

    @Suppress("LongMethod", "CyclomaticComplexMethod")
    private fun Asset.price(
        fiatPrices: Map<String, FiatPrice>,
        networkId: NetworkId,
        currency: SupportedCurrency
    ): AssetPrice? = when (this) {
        is Token -> {
            val tokenPrice = fiatPrices[resource.resourceAddress]

            AssetPrice.TokenPrice(
                asset = this,
                price = tokenPrice?.let {
                    FiatPrice(
                        price = it.price.toBigDecimal().multiply(resource.ownedAmount ?: BigDecimal.ZERO).toDouble(),
                        currency = it.currency
                    )
                }
            )
        }

        is LiquidStakeUnit -> {
            val tokenPrice = fiatPrices[resourceAddress]
            val priceForLSU = tokenPrice?.price?.toBigDecimal() ?: BigDecimal.ZERO
            val totalPrice = priceForLSU.multiply(fungibleResource.ownedAmount ?: BigDecimal.ZERO)
            val xrdPrice = fiatPrices[XrdResource.address(networkId = networkId)]?.price

            AssetPrice.LSUPrice(
                asset = this,
                price = FiatPrice(
                    price = totalPrice.toDouble(),
                    currency = currency
                ),
                oneXrdPrice = xrdPrice?.let {
                    FiatPrice(
                        price = it,
                        currency = currency
                    )
                }
            )
        }

        is PoolUnit -> {
            val poolItemPrices = pool?.resources?.associateWith { poolItem ->
                val poolItemFiatPrice = fiatPrices[poolItem.resourceAddress]
                val poolItemRedemptionValue = resourceRedemptionValue(poolItem)

                if (poolItemFiatPrice != null && poolItemRedemptionValue != null) {
                    FiatPrice(
                        price = (poolItemFiatPrice.price.toBigDecimal() * poolItemRedemptionValue).toDouble(),
                        currency = poolItemFiatPrice.currency
                    )
                } else {
                    null
                }
            }.orEmpty()

            AssetPrice.PoolUnitPrice(
                asset = this,
                prices = poolItemPrices,
                currency = currency
            )
        }

        is StakeClaim -> {
            val xrdAddress = XrdResource.address(networkId = networkId)
            val xrdPrice = fiatPrices[xrdAddress]

            val prices = nonFungibleResource.items.associateWith {
                val claimAmountXrd = it.claimAmountXrd
                if (claimAmountXrd != null && xrdPrice != null) {
                    FiatPrice(
                        price = claimAmountXrd.multiply(xrdPrice.price.toBigDecimal()).toDouble(),
                        currency = xrdPrice.currency
                    )
                } else {
                    null
                }
            }
            AssetPrice.StakeClaimPrice(
                asset = this,
                prices = prices,
                currency = currency
            )
        }

        is NonFungibleCollection -> null
    }

    private fun Asset.priceRequestAddresses(networkId: NetworkId): List<PriceRequestAddress> = when (this) {
        is NonFungibleCollection -> emptyList()
        is LiquidStakeUnit -> listOf(
            PriceRequestAddress.LSU(address = resourceAddress),
            PriceRequestAddress.Regular(XrdResource.address(networkId = networkId))
        )
        is StakeClaim -> {
            listOf(PriceRequestAddress.Regular(XrdResource.address(networkId = networkId)))
        }

        is PoolUnit -> pool?.resources?.map { PriceRequestAddress.Regular(it.resourceAddress) }.orEmpty()
        is Token -> listOf(PriceRequestAddress.Regular(address = resource.resourceAddress))
    }
}
