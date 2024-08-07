package com.babylon.wallet.android.data.repository.tokenprice

import com.babylon.wallet.android.BuildConfig
import com.babylon.wallet.android.data.gateway.apis.TokenPriceApi
import com.babylon.wallet.android.data.gateway.generated.infrastructure.Serializer
import com.babylon.wallet.android.data.gateway.model.TokenPriceResponse.Companion.asEntities
import com.babylon.wallet.android.data.gateway.model.TokensAndLsusPricesRequest
import com.babylon.wallet.android.data.gateway.model.TokensAndLsusPricesResponse.Companion.asEntity
import com.babylon.wallet.android.data.gateway.model.TokensPricesErrorResponse
import com.babylon.wallet.android.data.repository.cache.database.TokenPriceDao
import com.babylon.wallet.android.data.repository.cache.database.TokenPriceDao.Companion.tokenPriceCacheValidity
import com.babylon.wallet.android.data.repository.cache.database.TokenPriceEntity.Companion.toFiatPrices
import com.babylon.wallet.android.data.repository.toResult
import com.babylon.wallet.android.data.repository.tokenprice.FiatPriceRepository.PriceRequestAddress
import com.babylon.wallet.android.domain.RadixWalletException
import com.radixdlt.sargon.NetworkId
import com.radixdlt.sargon.ResourceAddress
import com.radixdlt.sargon.Timestamp
import com.radixdlt.sargon.extensions.string
import com.radixdlt.sargon.extensions.toDecimal192
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import rdx.works.core.domain.assets.FiatPrice
import rdx.works.core.domain.assets.SupportedCurrency
import rdx.works.core.domain.resources.XrdResource
import rdx.works.core.domain.toDouble
import rdx.works.peerdroid.di.IoDispatcher
import timber.log.Timber
import java.time.OffsetDateTime
import javax.inject.Inject
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlin.random.Random

@Retention(AnnotationRetention.BINARY)
@Qualifier
annotation class Mainnet

@Retention(AnnotationRetention.BINARY)
@Qualifier
annotation class Testnet

interface FiatPriceRepository {

    /**
     * It fetches the prices of tokens and updates the database.
     *
     */
    suspend fun updateFiatPrices(currency: SupportedCurrency): Result<Unit>

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

    // This will not be needed when using sargon's Address types
    sealed interface PriceRequestAddress {
        val address: ResourceAddress

        data class Regular(override val address: ResourceAddress) : PriceRequestAddress
        data class LSU(override val address: ResourceAddress) : PriceRequestAddress
    }

    class PricesNotSupportedInNetwork : IllegalStateException("Pricing service available only on Mainnet")
}

class MainnetFiatPriceRepository @Inject constructor(
    private val tokenPriceDao: TokenPriceDao,
    private val tokenPriceApi: TokenPriceApi,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : FiatPriceRepository {

    override suspend fun updateFiatPrices(
        currency: SupportedCurrency
    ): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            tokenPriceApi.tokens()
                .toResult(
                    mapError = { mapTokenPriceApiError(this) }
                )
                .onSuccess { tokensPrices ->
                    tokenPriceDao.insertTokensPrice(
                        tokensPrices = tokensPrices.asEntities()
                    )
                }.onFailure {
                    Timber.e("failed to fetch tokens prices with exception: ${it.message}")
                }
            Unit
        }
    }

    override suspend fun getFiatPrices(
        addresses: Set<PriceRequestAddress>,
        currency: SupportedCurrency,
        isRefreshing: Boolean
    ): Result<Map<ResourceAddress, FiatPrice>> = withContext(ioDispatcher) {
        runCatching {
            val allAddresses = addresses.map { it.address }.toSet()
            val regularResourceAddresses = addresses.filterIsInstance<PriceRequestAddress.Regular>().map { it.address }.toSet()
            val lsuResourceAddresses = addresses.filterIsInstance<PriceRequestAddress.LSU>().map { it.address }.toSet()
            val tokensPrices = tokenPriceDao.getTokensPrices(
                addresses = allAddresses.toSet(),
                minValidity = tokenPriceCacheValidity(isRefreshing = isRefreshing)
            )

            // always add XRD address to ensure that we have the latest price
            val remainingResourcesAddresses = (regularResourceAddresses subtract (tokensPrices.map { it.resourceAddress }.toSet()))

            val remainingLsusAddresses = lsuResourceAddresses subtract tokensPrices.map { it.resourceAddress }.toSet()

            if (remainingResourcesAddresses.isNotEmpty() || remainingLsusAddresses.isNotEmpty()) {
                tokenPriceApi.priceTokens(
                    tokensAndLsusPricesRequest = TokensAndLsusPricesRequest(
                        currency = currency.name,
                        lsus = remainingLsusAddresses.map { it.string },
                        tokens = remainingResourcesAddresses.map { it.string }
                    )
                ).toResult(
                    mapError = { mapTokenPriceApiError(this) }
                ).mapCatching { currentTokensAndLsusPrices ->
                    // update the database with the new tokens prices
                    tokenPriceDao.insertTokensPrice(tokensPrices = currentTokensAndLsusPrices.asEntity())
                    // return all tokens prices from database
                    tokenPriceDao.getTokensPrices(
                        addresses = allAddresses,
                        minValidity = tokenPriceCacheValidity()
                    ).toFiatPrices()
                }.getOrThrow()
            } else {
                tokensPrices.toFiatPrices()
            }
        }
    }

    private fun mapTokenPriceApiError(responseBody: ResponseBody?): RadixWalletException.GatewayException {
        val error = Serializer.kotlinxSerializationJson.decodeFromString<TokensPricesErrorResponse>(
            responseBody?.string().orEmpty()
        )
        return RadixWalletException.GatewayException.HttpError(code = 500, message = error.detail ?: "no message")
    }
}

// Keeps an in-memory cache of prices, so that prices of test tokens don't fluctuate between screens
@Singleton
class TestnetFiatPriceRepository @Inject constructor(
    @Mainnet private val delegate: FiatPriceRepository
) : FiatPriceRepository {

    private val mainnetXrdAddress = XrdResource.address(networkId = NetworkId.MAINNET)

    private val testnetPricesCache: MutableMap<ResourceAddress, TestnetPrice> = mutableMapOf()

    override suspend fun updateFiatPrices(currency: SupportedCurrency): Result<Unit> {
        if (!BuildConfig.EXPERIMENTAL_FEATURES_ENABLED) {
            return Result.failure(FiatPriceRepository.PricesNotSupportedInNetwork())
        }
        return delegate.updateFiatPrices(currency)
    }

    override suspend fun getFiatPrices(
        addresses: Set<PriceRequestAddress>,
        currency: SupportedCurrency,
        isRefreshing: Boolean
    ): Result<Map<ResourceAddress, FiatPrice>> {
        if (!BuildConfig.EXPERIMENTAL_FEATURES_ENABLED) {
            return Result.failure(FiatPriceRepository.PricesNotSupportedInNetwork())
        }

        return delegate.getFiatPrices(
            addresses = setOf(PriceRequestAddress.Regular(mainnetXrdAddress)),
            currency = currency,
            isRefreshing = isRefreshing
        ).map { result ->
            if (result.isNotEmpty()) {
                val prices = result.toMutableMap()
                val xrdPrice = prices.remove(mainnetXrdAddress)

                addresses.associate { priceRequestAddress ->
                    if (priceRequestAddress.address in XrdResource.addressesPerNetwork().values) {
                        priceRequestAddress.address to xrdPrice
                    } else {
                        priceRequestAddress.address to getTestnetPrice(address = priceRequestAddress.address, isRefreshing = isRefreshing)
                    }
                }.mapNotNull { addressAndFiatPrice ->
                    addressAndFiatPrice.value?.let { fiatPrice -> addressAndFiatPrice.key to fiatPrice }
                }.toMap()
            } else {
                emptyMap()
            }
        }
    }

    private fun getTestnetPrice(address: ResourceAddress, isRefreshing: Boolean): FiatPrice {
        val cachedPrice = testnetPricesCache[address]

        return if (cachedPrice == null) {
            FiatPrice(
                price = Random.nextDouble(from = 0.01, until = 1.0).toDecimal192(),
                currency = SupportedCurrency.USD
            ).also {
                testnetPricesCache[address] = TestnetPrice(
                    price = it,
                    updatedAt = Timestamp.now()
                )
            }
        } else {
            val lastUpdatedAt = cachedPrice.updatedAt
            if (isRefreshing || lastUpdatedAt.isBefore(OffsetDateTime.now().minusMinutes(MEMORY_CACHE_VALIDITY_MINUTES))) {
                val price = cachedPrice.price.price.toDouble()
                FiatPrice(
                    price = Random.nextDouble(
                        from = (price - PRICE_MINIMUM).coerceAtLeast(PRICE_MINIMUM),
                        until = price + PRICE_MINIMUM
                    ).toDecimal192(),
                    currency = SupportedCurrency.USD
                ).also {
                    testnetPricesCache[address] = TestnetPrice(
                        price = it,
                        updatedAt = Timestamp.now()
                    )
                }
            } else {
                cachedPrice.price
            }
        }
    }

    private data class TestnetPrice(
        val price: FiatPrice,
        val updatedAt: Timestamp
    )

    companion object {
        private const val MEMORY_CACHE_VALIDITY_MINUTES = 2L
        private const val PRICE_MINIMUM = 0.0000001
    }
}
