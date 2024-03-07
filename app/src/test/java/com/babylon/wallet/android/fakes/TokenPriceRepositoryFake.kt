package com.babylon.wallet.android.fakes

import com.babylon.wallet.android.data.repository.tokenprice.TokenPriceRepository
import com.babylon.wallet.android.domain.model.assets.TokenPrice
import com.babylon.wallet.android.mockdata.mockLSUAddress1
import com.babylon.wallet.android.mockdata.mockLSUAddress2
import com.babylon.wallet.android.mockdata.mockNFTAddressForStakeClaim1
import com.babylon.wallet.android.mockdata.mockNFTAddressForStakeClaim2
import com.babylon.wallet.android.mockdata.mockResourceAddress1
import com.babylon.wallet.android.mockdata.mockResourceAddress2
import com.babylon.wallet.android.mockdata.mockResourceAddress3
import com.babylon.wallet.android.mockdata.mockResourceAddress4
import com.babylon.wallet.android.mockdata.mockResourceAddress5
import com.babylon.wallet.android.mockdata.mockResourceAddressXRD
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

        return Result.success(results)
    }

    private val tokensPrices = listOf(
        TokenPrice(
            resourceAddress = mockResourceAddressXRD,
            price = mockResourceXRDPrice,
            currency = "USD"
        ),
        TokenPrice(
            resourceAddress = mockResourceAddress1,
            price = mockResource1Price,
            currency = "USD"
        ),
        TokenPrice(
            resourceAddress = mockResourceAddress2,
            price = mockResource2Price,
            currency = "USD"
        ),
        TokenPrice(
            resourceAddress = mockResourceAddress3,
            price = mockResource3Price,
            currency = "USD"
        ),
        TokenPrice(
            resourceAddress = mockResourceAddress4,
            price = mockResource4Price,
            currency = "USD"
        ),
        TokenPrice(
            resourceAddress = mockResourceAddress5,
            price = mockResource5Price,
            currency = "USD"
        ),
        TokenPrice(
            resourceAddress = mockLSUAddress1,
            price = mockLSUPrice1,
            currency = "USD"
        ),
        TokenPrice(
            resourceAddress = mockLSUAddress2,
            price = mockLSUPrice2,
            currency = "USD"
        ),
        TokenPrice(
            resourceAddress = mockNFTAddressForStakeClaim1,
            price = mockResourceXRDPrice,
            currency = "USD"
        ),
        TokenPrice(
            resourceAddress = mockNFTAddressForStakeClaim2,
            price = mockResourceXRDPrice,
            currency = "USD"
        )
    )

    companion object {
        val mockLSUPrice1 = BigDecimal(0.51)
        val mockLSUPrice2 = BigDecimal(80.091)
        val mockResourceXRDPrice = BigDecimal(1.511)
        val mockResource1Price = BigDecimal(0.070098)
        val mockResource2Price = BigDecimal(6)
        val mockResource3Price = BigDecimal(15)
        val mockResource4Price = BigDecimal(1150000)
        val mockResource5Price = BigDecimal(0.0005)
    }
}