package com.babylon.wallet.android.domain.usecases.assets

import com.babylon.wallet.android.data.repository.state.StateRepository
import com.babylon.wallet.android.data.repository.tokenprice.FiatPriceRepository
import com.babylon.wallet.android.data.repository.tokenprice.FiatPriceRepository.PriceRequestAddress
import com.babylon.wallet.android.data.repository.tokenprice.Mainnet
import com.babylon.wallet.android.data.repository.tokenprice.Testnet
import com.babylon.wallet.android.domain.model.assets.AccountWithAssets
import com.radixdlt.sargon.Account
import com.radixdlt.sargon.NetworkId
import com.radixdlt.sargon.ResourceAddress
import com.radixdlt.sargon.extensions.orZero
import com.radixdlt.sargon.extensions.times
import rdx.works.core.domain.assets.Asset
import rdx.works.core.domain.assets.AssetPrice
import rdx.works.core.domain.assets.FiatPrice
import rdx.works.core.domain.assets.LiquidStakeUnit
import rdx.works.core.domain.assets.NonFungibleCollection
import rdx.works.core.domain.assets.PoolUnit
import rdx.works.core.domain.assets.StakeClaim
import rdx.works.core.domain.assets.SupportedCurrency
import rdx.works.core.domain.assets.Token
import rdx.works.core.domain.resources.XrdResource
import rdx.works.core.then
import rdx.works.profile.domain.gateway.GetCurrentGatewayUseCase
import javax.inject.Inject

class GetFiatValueUseCase @Inject constructor(
    @Mainnet private val mainnetFiatPriceRepository: FiatPriceRepository,
    @Testnet private val testnetFiatPriceRepository: FiatPriceRepository,
    private val stateRepository: StateRepository,
    private val getCurrentGatewayUseCase: GetCurrentGatewayUseCase
) {

    suspend fun forXrd(currency: SupportedCurrency = SupportedCurrency.USD, isRefreshing: Boolean = false): Result<FiatPrice?> {
        return runCatching {
            val networkId = getCurrentGatewayUseCase.invoke().network.id
            val xrdAddress = XrdResource.address(networkId = getCurrentGatewayUseCase.invoke().network.id)
            getFiatPrices(
                networkId = networkId,
                addresses = setOf(PriceRequestAddress.Regular(xrdAddress)),
                currency = currency,
                isRefreshing = isRefreshing
            ).mapCatching { prices ->
                prices[xrdAddress]
            }.getOrNull()
        }
    }

    suspend fun forAccount(
        accountWithAssets: AccountWithAssets,
        currency: SupportedCurrency = SupportedCurrency.USD,
        isRefreshing: Boolean
    ): Result<List<AssetPrice>> {
        val networkId = accountWithAssets.account.networkId
        return runCatching {
            accountWithAssets.assets?.ownedTokens?.map { it.priceRequestAddresses(networkId) }?.flatten().orEmpty() +
                accountWithAssets.assets?.ownedLiquidStakeUnits?.map { it.priceRequestAddresses(networkId) }?.flatten().orEmpty() +
                accountWithAssets.assets?.ownedPoolUnits?.map { it.priceRequestAddresses(networkId) }?.flatten().orEmpty() +
                accountWithAssets.assets?.ownedStakeClaims?.map { it.priceRequestAddresses(networkId) }?.flatten().orEmpty()
        }.then { addresses ->
            getFiatPrices(
                networkId = networkId,
                addresses = addresses.toSet(),
                currency = currency,
                isRefreshing = isRefreshing
            )
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

    /**
     * It is used in Account screen when the forAccount (above) fails to return ALL the assets prices of the account.
     * Then we use this method to get those asset prices that didn't fail.
     *
     */
    suspend fun forAssets(
        assets: List<Asset>,
        account: Account,
        currency: SupportedCurrency = SupportedCurrency.USD,
        isRefreshing: Boolean
    ): List<AssetPrice?> {
        val networkId = account.networkId

        val requestAddresses = runCatching {
            assets.map {
                it.priceRequestAddresses(networkId)
            }.flatten()
        }.getOrNull() ?: return emptyList()

        val prices = getFiatPrices(
            networkId = networkId,
            addresses = requestAddresses.toSet(),
            currency = currency,
            isRefreshing = isRefreshing
        ).getOrNull() ?: return emptyList()

        val stakeClaimsWithMissingNFTs = assets.filterIsInstance<StakeClaim>().filter { stakeClaim ->
            stakeClaim.nonFungibleResource.amount.toInt() != stakeClaim.nonFungibleResource.items.size
        }
        val ensuredStakeClaims = if (stakeClaimsWithMissingNFTs.isNotEmpty()) {
            stateRepository.updateStakeClaims(
                account = account,
                claims = stakeClaimsWithMissingNFTs
            ).getOrNull() ?: stakeClaimsWithMissingNFTs
        } else {
            stakeClaimsWithMissingNFTs
        }

        return assets.map { asset ->
            // find those assets that are claims and replace them with the new ensuredStakeClaims
            val claim = ensuredStakeClaims.find { stakeClaim ->
                stakeClaim.resourceAddress == asset.resource.address
            }
            claim ?: asset
        }.map { asset ->
            asset.price(fiatPrices = prices, networkId = networkId, currency = currency)
        }
    }

    suspend fun forAsset(
        asset: Asset,
        account: Account,
        currency: SupportedCurrency = SupportedCurrency.USD,
        isRefreshing: Boolean
    ): Result<AssetPrice?> {
        val networkId = account.networkId

        return runCatching {
            asset.priceRequestAddresses(networkId)
        }.then { addresses ->
            getFiatPrices(
                networkId = networkId,
                addresses = addresses.toSet(),
                currency = currency,
                isRefreshing = isRefreshing
            )
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
        fiatPrices: Map<ResourceAddress, FiatPrice>,
        networkId: NetworkId,
        currency: SupportedCurrency
    ): AssetPrice? = when (this) {
        is Token -> {
            val tokenPrice = fiatPrices[resource.address]

            AssetPrice.TokenPrice(
                asset = this,
                price = tokenPrice?.let {
                    FiatPrice(
                        price = it.price * resource.ownedAmount.orZero(),
                        currency = it.currency
                    )
                }
            )
        }

        is LiquidStakeUnit -> {
            val tokenPrice = fiatPrices[resourceAddress]
            val priceForLSU = tokenPrice?.price.orZero()
            val totalPrice = priceForLSU * fungibleResource.ownedAmount.orZero()
            val xrdPrice = fiatPrices[XrdResource.address(networkId = networkId)]?.price

            AssetPrice.LSUPrice(
                asset = this,
                price = FiatPrice(
                    price = totalPrice,
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
                val poolItemFiatPrice = fiatPrices[poolItem.address]
                val poolItemRedemptionValue = resourceRedemptionValue(poolItem)

                if (poolItemFiatPrice != null && poolItemRedemptionValue != null) {
                    FiatPrice(
                        price = poolItemFiatPrice.price * poolItemRedemptionValue,
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
                        price = claimAmountXrd * xrdPrice.price,
                        currency = xrdPrice.currency
                    )
                } else {
                    null
                }
            }
            AssetPrice.StakeClaimPrice(
                asset = this,
                prices = prices,
                currency = currency,
                oneXrdPrice = xrdPrice?.let {
                    FiatPrice(
                        price = it.price,
                        currency = currency
                    )
                }
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

        is PoolUnit -> pool?.resources?.map { PriceRequestAddress.Regular(it.address) }.orEmpty()
        is Token -> listOf(PriceRequestAddress.Regular(address = resource.address))
    }

    private suspend fun getFiatPrices(
        networkId: NetworkId,
        addresses: Set<PriceRequestAddress>,
        currency: SupportedCurrency,
        isRefreshing: Boolean
    ): Result<Map<ResourceAddress, FiatPrice>> = if (networkId == NetworkId.MAINNET) {
        mainnetFiatPriceRepository.getFiatPrices(
            addresses = addresses,
            currency = currency,
            isRefreshing = isRefreshing
        )
    } else {
        testnetFiatPriceRepository.getFiatPrices(
            addresses = addresses,
            currency = currency,
            isRefreshing = isRefreshing
        )
    }
}
