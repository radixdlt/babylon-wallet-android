package com.babylon.wallet.android.data.repository.tokenprice

import com.babylon.wallet.android.data.gateway.apis.TokenPriceApi
import com.babylon.wallet.android.data.gateway.generated.infrastructure.Serializer
import com.babylon.wallet.android.data.gateway.model.TokenPriceResponse.Companion.asEntities
import com.babylon.wallet.android.data.gateway.model.TokensAndLsusPricesRequest
import com.babylon.wallet.android.data.gateway.model.TokensAndLsusPricesResponse.Companion.asEntity
import com.babylon.wallet.android.data.gateway.model.TokensPricesErrorResponse
import com.babylon.wallet.android.data.repository.cache.database.TokenPriceDao
import com.babylon.wallet.android.data.repository.cache.database.TokenPriceDao.Companion.tokenPriceCacheValidity
import com.babylon.wallet.android.data.repository.cache.database.TokenPriceEntity.Companion.toTokenPrice
import com.babylon.wallet.android.data.repository.toResult
import com.babylon.wallet.android.data.repository.tokenprice.TokenPriceRepository.PriceRequestAddress
import com.babylon.wallet.android.domain.RadixWalletException
import com.babylon.wallet.android.domain.model.assets.TokenPrice
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import rdx.works.peerdroid.di.IoDispatcher
import timber.log.Timber
import javax.inject.Inject

interface TokenPriceRepository {

    /**
     * It fetches the prices of tokens and updates the database.
     *
     */
    suspend fun updateTokensPrices(): Result<Unit>

    /**
     * It takes two parameters:
     *  - the resourcesAddresses that are tokens and/or pool items
     *  - the lsusAddresses of stake units
     *
     */
    suspend fun getTokensPrices(
        addresses: Set<PriceRequestAddress>
    ): Result<List<TokenPrice>>

    // This will not be needed when using sargon's Address types
    sealed interface PriceRequestAddress {
        val address: String
        data class Regular(override val address: String) : PriceRequestAddress
        data class LSU(override val address: String) : PriceRequestAddress
    }
}

class TokenPriceRepositoryImpl @Inject constructor(
    private val tokenPriceDao: TokenPriceDao,
    private val tokenPriceApi: TokenPriceApi,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : TokenPriceRepository {

    override suspend fun updateTokensPrices(): Result<Unit> = withContext(ioDispatcher) {
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

    override suspend fun getTokensPrices(
        addresses: Set<PriceRequestAddress>
    ): Result<List<TokenPrice>> = withContext(ioDispatcher) {
        runCatching {
            val allAddresses = addresses.map { it.address }.toSet()
            val regularResourceAddresses = allAddresses.filterIsInstance<PriceRequestAddress.Regular>().map { it.address }.toSet()
            val lsuResourceAddresses = allAddresses.filterIsInstance<PriceRequestAddress.LSU>().map { it.address }.toSet()
            val tokensPrices = tokenPriceDao.getTokensPrices(
                addresses = allAddresses,
                minValidity = tokenPriceCacheValidity()
            )

            // always add XRD address to ensure that we have the latest price
            val remainingResourcesAddresses = (regularResourceAddresses subtract (tokensPrices.map { it.resourceAddress }.toSet()))

            val remainingLsusAddresses = lsuResourceAddresses subtract tokensPrices.map { it.resourceAddress }.toSet()

            if (remainingResourcesAddresses.isNotEmpty() || remainingLsusAddresses.isNotEmpty()) {
                tokenPriceApi.priceTokens(
                    tokensAndLsusPricesRequest = TokensAndLsusPricesRequest(
                        currency = TokenPrice.CURRENCY_USD,
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
                    ).toTokenPrice()
                }.getOrElse {
                    Timber.e("failed to fetch tokens prices with exception: ${it.message}")
                    emptyList()
                }
            } else {
                tokensPrices.toTokenPrice()
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
