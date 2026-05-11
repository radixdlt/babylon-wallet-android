package com.babylon.wallet.android.data.repository.tokenprice

import com.babylon.wallet.android.data.repository.tokenprice.FiatPriceRepository.PriceRequestAddress
import com.babylon.wallet.android.utils.callSafely
import com.radixdlt.sargon.NonFungibleGlobalId
import com.radixdlt.sargon.ResourceAddress
import com.radixdlt.sargon.SargonOs
import com.radixdlt.sargon.os.SargonOsManager
import kotlinx.coroutines.CoroutineDispatcher
import rdx.works.core.domain.assets.FiatPrice
import rdx.works.core.domain.assets.SupportedCurrency
import rdx.works.core.domain.assets.toSargon
import rdx.works.peerdroid.di.IoDispatcher
import javax.inject.Inject

interface FiatPriceRepository {

    /**
     * It takes two parameters:
     *  - the resourcesAddresses that are tokens and/or pool items
     *  - the lsusAddresses of stake units
     *
     */
    suspend fun getFiatPrices(
        addresses: Set<PriceRequestAddress>,
        currency: SupportedCurrency,
        isRefreshing: Boolean
    ): Result<Map<ResourceAddress, FiatPrice>>

    suspend fun getNftFiatPrices(
        nftIds: List<NonFungibleGlobalId>,
        currency: SupportedCurrency,
        isRefreshing: Boolean
    ): Result<Map<NonFungibleGlobalId, FiatPrice>>

    // This will not be needed when using sargon's Address types
    sealed interface PriceRequestAddress {
        val address: ResourceAddress

        data class Regular(override val address: ResourceAddress) : PriceRequestAddress
        data class LSU(override val address: ResourceAddress) : PriceRequestAddress
    }

    class PricesUnavailableOnCurrentNetwork : IllegalStateException("Pricing unavailable on current network")
}

class SargonFiatPriceRepository @Inject constructor(
    private val sargonOsManager: SargonOsManager,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : FiatPriceRepository {

    override suspend fun getFiatPrices(
        addresses: Set<PriceRequestAddress>,
        currency: SupportedCurrency,
        isRefreshing: Boolean
    ): Result<Map<ResourceAddress, FiatPrice>> {
        if (addresses.isEmpty()) return Result.success(emptyMap())

        val tokens = addresses.filterIsInstance<PriceRequestAddress.Regular>().map { it.address }
        val lsus = addresses.filterIsInstance<PriceRequestAddress.LSU>().map { it.address }

        return sargonOsManager.callSafely(ioDispatcher) {
            requireConfiguredTokenPriceServices()

            fetchFungibleFiatValues(
                tokens = tokens,
                lsus = lsus,
                currency = currency.toSargon(),
                forceFetch = isRefreshing
            ).mapValues { (_, price) ->
                FiatPrice(
                    price = price,
                    currency = currency
                )
            }
        }
    }

    override suspend fun getNftFiatPrices(
        nftIds: List<NonFungibleGlobalId>,
        currency: SupportedCurrency,
        isRefreshing: Boolean
    ): Result<Map<NonFungibleGlobalId, FiatPrice>> {
        if (nftIds.isEmpty()) return Result.success(emptyMap())

        return sargonOsManager.callSafely(ioDispatcher) {
            requireConfiguredTokenPriceServices()

            fetchNftFiatValues(
                nftIds = nftIds,
                currency = currency.toSargon(),
                forceFetch = isRefreshing
            ).mapValues { (_, price) ->
                FiatPrice(
                    price = price,
                    currency = currency
                )
            }
        }
    }

    private fun SargonOs.requireConfiguredTokenPriceServices() {
        if (tokenPriceServicesOnCurrentNetwork().isEmpty()) {
            throw FiatPriceRepository.PricesUnavailableOnCurrentNetwork()
        }
    }
}
