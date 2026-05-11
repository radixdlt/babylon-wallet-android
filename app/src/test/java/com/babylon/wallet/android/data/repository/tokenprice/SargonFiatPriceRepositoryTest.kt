package com.babylon.wallet.android.data.repository.tokenprice

import com.radixdlt.sargon.FiatCurrency
import com.radixdlt.sargon.NonFungibleGlobalId
import com.radixdlt.sargon.ResourceAddress
import com.radixdlt.sargon.SargonOs
import com.radixdlt.sargon.TokenPriceService
import com.radixdlt.sargon.extensions.toDecimal192
import com.radixdlt.sargon.os.SargonOsManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import rdx.works.core.domain.assets.SupportedCurrency

class SargonFiatPriceRepositoryTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private val sargonOs = mockk<SargonOs>()
    private val sargonOsManager = mockk<SargonOsManager>().also {
        every { it.sargonOs } returns sargonOs
    }

    private val repository = SargonFiatPriceRepository(
        sargonOsManager = sargonOsManager,
        ioDispatcher = testDispatcher
    )

    @Before
    fun setUp() {
        every { sargonOs.tokenPriceServicesOnCurrentNetwork() } returns listOf(mockk<TokenPriceService>())
    }

    @Test
    fun `fungible requests split tokens and lsus and map fiat values`() = testScope.runTest {
        val token = mockk<ResourceAddress>()
        val lsu = mockk<ResourceAddress>()
        val tokenPrice = 1.25.toDecimal192()
        val lsuPrice = 3.5.toDecimal192()

        coEvery {
            sargonOs.fetchFungibleFiatValues(
                tokens = listOf(token),
                lsus = listOf(lsu),
                currency = FiatCurrency.USD,
                forceFetch = true
            )
        } returns mapOf(
            token to tokenPrice,
            lsu to lsuPrice
        )

        val result = repository.getFiatPrices(
            addresses = setOf(
                FiatPriceRepository.PriceRequestAddress.Regular(token),
                FiatPriceRepository.PriceRequestAddress.LSU(lsu)
            ),
            currency = SupportedCurrency.USD,
            isRefreshing = true
        ).getOrThrow()

        assertEquals(tokenPrice, result[token]?.price)
        assertEquals(lsuPrice, result[lsu]?.price)
        assertEquals(SupportedCurrency.USD, result[token]?.currency)
    }

    @Test
    fun `nft requests map fiat values`() = testScope.runTest {
        val nftId = mockk<NonFungibleGlobalId>()
        val nftPrice = 4.75.toDecimal192()

        coEvery {
            sargonOs.fetchNftFiatValues(
                nftIds = listOf(nftId),
                currency = FiatCurrency.USD,
                forceFetch = false
            )
        } returns mapOf(nftId to nftPrice)

        val result = repository.getNftFiatPrices(
            nftIds = listOf(nftId),
            currency = SupportedCurrency.USD,
            isRefreshing = false
        ).getOrThrow()

        assertEquals(nftPrice, result[nftId]?.price)
        assertEquals(SupportedCurrency.USD, result[nftId]?.currency)
    }

    @Test
    fun `pricing is unavailable when current network has no configured token price service`() = testScope.runTest {
        val token = mockk<ResourceAddress>()
        every { sargonOs.tokenPriceServicesOnCurrentNetwork() } returns emptyList()

        val result = repository.getFiatPrices(
            addresses = setOf(FiatPriceRepository.PriceRequestAddress.Regular(token)),
            currency = SupportedCurrency.USD,
            isRefreshing = false
        )

        assertTrue(result.exceptionOrNull() is FiatPriceRepository.PricesUnavailableOnCurrentNetwork)
        coVerify(exactly = 0) {
            sargonOs.fetchFungibleFiatValues(any(), any(), any(), any())
        }
    }
}
