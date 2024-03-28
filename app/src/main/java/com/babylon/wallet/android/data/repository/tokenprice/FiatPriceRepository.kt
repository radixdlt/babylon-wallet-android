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
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import rdx.works.core.domain.assets.FiatPrice
import rdx.works.core.domain.assets.SupportedCurrency
import rdx.works.core.domain.resources.XrdResource
import rdx.works.peerdroid.di.IoDispatcher
import rdx.works.profile.derivation.model.NetworkId
import timber.log.Timber
import java.lang.IllegalStateException
import javax.inject.Inject
import javax.inject.Qualifier
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
    ): Result<Map<String, FiatPrice>>

    // This will not be needed when using sargon's Address types
    sealed interface PriceRequestAddress {
        val address: String
        data class Regular(override val address: String) : PriceRequestAddress
        data class LSU(override val address: String) : PriceRequestAddress
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
    ): Result<Map<String, FiatPrice>> = withContext(ioDispatcher) {
        runCatching {
            val allAddresses = addresses.map { it.address }.toSet()
            val regularResourceAddresses = addresses.filterIsInstance<PriceRequestAddress.Regular>().map { it.address }.toSet()
            val lsuResourceAddresses = addresses.filterIsInstance<PriceRequestAddress.LSU>().map { it.address }.toSet()
            val tokensPrices = tokenPriceDao.getTokensPrices(
                addresses = allAddresses,
                minValidity = tokenPriceCacheValidity(isRefreshing = isRefreshing)
            )

            // always add XRD address to ensure that we have the latest price
            val remainingResourcesAddresses = (regularResourceAddresses subtract (tokensPrices.map { it.resourceAddress }.toSet()))

            val remainingLsusAddresses = lsuResourceAddresses subtract tokensPrices.map { it.resourceAddress }.toSet()

            if (remainingResourcesAddresses.isNotEmpty() || remainingLsusAddresses.isNotEmpty()) {
                tokenPriceApi.priceTokens(
                    tokensAndLsusPricesRequest = TokensAndLsusPricesRequest(
                        currency = currency.name,
                        lsus = remainingLsusAddresses.toList(),
                        tokens = remainingResourcesAddresses.toList()
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
                }.getOrElse {
                    Timber.e("failed to fetch tokens prices with exception: ${it.message}")
                    emptyMap()
                }
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

class TestnetFiatPriceRepository @Inject constructor(
    @Mainnet private val delegate: FiatPriceRepository
) : FiatPriceRepository {

    private val mainnetXrdAddress = XrdResource.address(networkId = NetworkId.Mainnet.value)
    private val mainnetAddresses = listOf(
        "resource_rdx1t4tjx4g3qzd98nayqxm7qdpj0a0u8ns6a0jrchq49dyfevgh6u0gj3",
        "resource_rdx1t45js47zxtau85v0tlyayerzrgfpmguftlfwfr5fxzu42qtu72tnt0",
        "resource_rdx1tk7g72c0uv2g83g3dqtkg6jyjwkre6qnusgjhrtz0cj9u54djgnk3c",
        "resource_rdx1tkk83magp3gjyxrpskfsqwkg4g949rmcjee4tu2xmw93ltw2cz94sq"
    )

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
    ): Result<Map<String, FiatPrice>> {
        if (!BuildConfig.EXPERIMENTAL_FEATURES_ENABLED) {
            return Result.failure(FiatPriceRepository.PricesNotSupportedInNetwork())
        }

        val mainnetAddressesToRequest = if (addresses.any { it.address in XrdResource.addressesPerNetwork().values }) {
            mainnetAddresses + mainnetXrdAddress
        } else {
            mainnetAddresses
        }.map { PriceRequestAddress.Regular(it) }.toSet()

        return delegate.getFiatPrices(
            addresses = mainnetAddressesToRequest,
            currency = currency,
            isRefreshing = isRefreshing
        ).map { result ->
            val prices = result.toMutableMap()
            val xrdPrice = prices.remove(mainnetXrdAddress)

            addresses.associate { priceRequestAddress ->
                if (prices.isEmpty()) {
                    // if the token price service (getFiatPrices) returns emptyMap we can't give random price
                    priceRequestAddress.address to FiatPrice(0.0, currency)
                } else if (priceRequestAddress.address in XrdResource.addressesPerNetwork().values) {
                    priceRequestAddress.address to xrdPrice
                } else {
                    val randomPrice = prices.entries.elementAt(Random.nextInt(prices.entries.size)).value
                    priceRequestAddress.address to randomPrice
                }
            }.mapNotNull { addressAndFiatPrice ->
                addressAndFiatPrice.value?.let { fiatPrice -> addressAndFiatPrice.key to fiatPrice }
            }.toMap()
        }
    }
}
