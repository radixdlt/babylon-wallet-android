package com.babylon.wallet.android.fakes

import com.babylon.wallet.android.data.repository.tokenprice.FiatPriceRepository
import com.babylon.wallet.android.domain.model.assets.FiatPrice
import com.babylon.wallet.android.domain.model.assets.SupportedCurrency
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

class FiatPriceRepositoryFake : FiatPriceRepository {

    override suspend fun updateFiatPrices(
        currency: SupportedCurrency
    ): Result<Unit> {
        return Result.success(Unit)
    }

    override suspend fun getFiatPrices(
        addresses: Set<FiatPriceRepository.PriceRequestAddress>,
        currency: SupportedCurrency,
        isRefreshing: Boolean
    ): Result<Map<String, FiatPrice>> {
        val all = addresses.map { it.address }
        val results = fiatPrices.filter {
            all.contains(it.key)
        }

        return Result.success(results)
    }

    private val fiatPrices = mapOf(
        mockResourceAddressXRD to FiatPrice(
            price = mockResourceXRDPrice,
            currency = SupportedCurrency.USD
        ),
        mockResourceAddress1 to FiatPrice(
            price = mockResource1Price,
            currency = SupportedCurrency.USD
        ),
        mockResourceAddress2 to FiatPrice(
            price = mockResource2Price,
            currency = SupportedCurrency.USD
        ),
        mockResourceAddress3 to FiatPrice(
            price = mockResource3Price,
            currency = SupportedCurrency.USD
        ),
        mockResourceAddress4 to FiatPrice(
            price = mockResource4Price,
            currency = SupportedCurrency.USD
        ),
        mockResourceAddress5 to FiatPrice(
            price = mockResource5Price,
            currency = SupportedCurrency.USD
        ),
        mockLSUAddress1 to FiatPrice(
            price = mockLSUPrice1,
            currency = SupportedCurrency.USD
        ),
        mockLSUAddress2 to FiatPrice(
            price = mockLSUPrice2,
            currency = SupportedCurrency.USD
        ),
        mockNFTAddressForStakeClaim1 to FiatPrice(
            price = mockResourceXRDPrice,
            currency = SupportedCurrency.USD
        ),
        mockNFTAddressForStakeClaim2 to FiatPrice(
            price = mockResourceXRDPrice,
            currency = SupportedCurrency.USD
        )
    )

    companion object {
        val mockLSUPrice1 = 0.51
        val mockLSUPrice2 = 80.091
        val mockResourceXRDPrice = 1.511
        val mockResource1Price = 0.070098
        val mockResource2Price = 6.0
        val mockResource3Price = 15.0
        val mockResource4Price = 1150000.0
        val mockResource5Price = 0.0005
    }
}