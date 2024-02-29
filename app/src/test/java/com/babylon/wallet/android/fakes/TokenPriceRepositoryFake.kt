package com.babylon.wallet.android.fakes

import com.babylon.wallet.android.data.repository.tokenprice.TokenPriceRepository
import com.babylon.wallet.android.domain.model.assets.TokenPrice
import java.math.BigDecimal

class TokenPriceRepositoryFake : TokenPriceRepository {

    override suspend fun updateTokensPrices(): Result<Unit> {
        return Result.success(Unit)
    }

    override suspend fun getTokensPrices(
        resourcesAddresses: Set<String>,
        lsusAddresses: Set<String>
    ): Result<List<TokenPrice>> {
        val all = resourcesAddresses + lsusAddresses
        val results = tokensPrices.filter {
            all.contains(it.resourceAddress)
        }
        println("----> results = $results")
        return Result.success(results)
    }

    private val tokensPrices = listOf(
        TokenPrice(
            resourceAddress = "resourceAddress_0",
            price = BigDecimal(1.511),
            currency = "USD"
        ),
        TokenPrice(
            resourceAddress = "resourceAddress_1",
            price = BigDecimal(0.0700098),
            currency = "USD"
        ),
        TokenPrice(
            resourceAddress = "resourceAddress_2",
            price = BigDecimal(6),
            currency = "USD"
        ),
        TokenPrice(
            resourceAddress = "resourceAddress_3",
            price = BigDecimal(15),
            currency = "USD"
        ),
        TokenPrice(
            resourceAddress = "resourceAddress_4",
            price = BigDecimal(11500000),
            currency = "USD"
        ),
        TokenPrice(
            resourceAddress = "resourceAddress_5",
            price = BigDecimal(0.00005),
            currency = "USD"
        ),
    )
}