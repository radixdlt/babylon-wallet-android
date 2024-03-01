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
        resourcesAddresses: Set<String>,
        lsusAddresses: Set<String>
    ): Result<List<TokenPrice>>
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
        resourcesAddresses: Set<String>,
        lsusAddresses: Set<String>
    ): Result<List<TokenPrice>> = withContext(ioDispatcher) {
        runCatching {
            val allResourcesAddresses = resourcesAddresses + lsusAddresses

            val tokensPrices = tokenPriceDao.getTokensPrices(
                addresses = allResourcesAddresses,
                minValidity = tokenPriceCacheValidity()
            )

            val remainingResourcesAddresses = resourcesAddresses subtract tokensPrices.map { it.resourceAddress }.toSet()
            val remainingLsusAddresses = lsusAddresses subtract tokensPrices.map { it.resourceAddress }.toSet()

            if (remainingResourcesAddresses.isNotEmpty() || remainingLsusAddresses.isNotEmpty()) {
                tokenPriceApi.priceTokens(
                    tokensAndLsusPricesRequest = TokensAndLsusPricesRequest(
                        currency = "USD",
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
                        addresses = allResourcesAddresses,
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
