package com.babylon.wallet.android.domain.usecases

import com.babylon.wallet.android.domain.usecases.assets.GetFiatValueUseCase
import com.babylon.wallet.android.fakes.FiatPriceRepositoryFake
import com.babylon.wallet.android.fakes.StateRepositoryFake
import com.babylon.wallet.android.mockdata.mockAccountsWithMockAssets
import com.radixdlt.sargon.Decimal192
import com.radixdlt.sargon.extensions.floor
import com.radixdlt.sargon.extensions.orZero
import com.radixdlt.sargon.extensions.plus
import com.radixdlt.sargon.extensions.sumOf
import com.radixdlt.sargon.extensions.toDecimal192
import io.mockk.mockk
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import rdx.works.core.domain.assets.Asset

class GetFiatValueUseCaseTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private val tokenPriceRepositoryFake = FiatPriceRepositoryFake()
    private val getFiatValueUseCase = GetFiatValueUseCase(
        mainnetFiatPriceRepository = tokenPriceRepositoryFake,
        testnetFiatPriceRepository = mockk(),
        stateRepository = StateRepositoryFake()
    )

    @Test
    fun `assert that all the tokens of the given accounts have their prices`() = testScope.runTest {
        mockAccountsWithMockAssets.forEach { accountWithAssets ->
            val assetPriceAddresses = getFiatValueUseCase.forAccount(accountWithAssets, isRefreshing = false).getOrThrow().map {
                it.asset.resource.address
            }.toSet()

            val ownedTokenAddresses = accountWithAssets.assets?.ownedTokens.orEmpty().map { it.resource.address }.toSet()
            assertTrue(assetPriceAddresses.containsAll(ownedTokenAddresses))
        }
    }

    @Test
    fun `given a token when it is not included in the result of the prices then it does not have fiat value`() = testScope.runTest {
        val assetPrices = getFiatValueUseCase.forAccount(mockAccountsWithMockAssets[0], isRefreshing = false).getOrThrow()

        val assetPrice = assetPrices.find { it.asset.resource.name == "OtherToken" }
        assertNotNull(assetPrice?.asset)
        assertNull(assetPrice?.price)
    }

    @Test
    fun `assert total fiat values of owned assets given an account with some tokens, lsus, and pool units`() {
        val expectedTotalFiatValueOfTokens = 110415990.05306.toDecimal192()
        val expectedTotalFiatValueOfLSUs = 1927.12059.toDecimal192()
        val expectedTotalFiatValueOfPoolUnits = 63769.68279.toDecimal192()
        val expectedTotalFiatValueOfStakeClaims = 0.0.toDecimal192()
        val expectedTotalFiatValueOfAccount = 110481686.85645.toDecimal192()

        val accountsWithAssets = mockAccountsWithMockAssets
        val accountWithAssets = accountsWithAssets[0]

        testScope.runTest {
            val assetPricesForAccount = getFiatValueUseCase.forAccount(accountWithAssets, isRefreshing = false).getOrThrow()

            val pricesPerAsset: Map<Asset, Decimal192> = assetPricesForAccount.associate { assetPrice ->
                assetPrice.asset to assetPrice.price?.price.orZero()
            }

            val allTokensOfTheAccount = accountWithAssets.assets?.tokens
            val tokensTotalFiatValue = allTokensOfTheAccount?.sumOf {
                pricesPerAsset[it].orZero()
            }.orZero().floor(decimalPlaces = 5u)
            assertEquals(expectedTotalFiatValueOfTokens, tokensTotalFiatValue)

            val allLSUsOfTheAccount = accountWithAssets.assets?.liquidStakeUnits
            val lsusTotalFiatValue = allLSUsOfTheAccount?.sumOf {
                pricesPerAsset[it].orZero()
            }.orZero().floor(decimalPlaces = 5u)
            assertEquals(expectedTotalFiatValueOfLSUs, lsusTotalFiatValue)

            val allPoolUnitsOfTheAccount = accountWithAssets.assets?.poolUnits
            val poolUnitsTotalFiatValue = allPoolUnitsOfTheAccount?.sumOf {
                pricesPerAsset[it].orZero()
            }.orZero().floor(decimalPlaces = 5u)
            assertEquals(expectedTotalFiatValueOfPoolUnits, poolUnitsTotalFiatValue)

            val allStakeClaimsOfTheAccount = accountWithAssets.assets?.stakeClaims
            val stakeClaimsTotalFiatValue = allStakeClaimsOfTheAccount?.sumOf {
                pricesPerAsset[it].orZero()
            }.orZero().floor(decimalPlaces = 5u)
            assertEquals(expectedTotalFiatValueOfStakeClaims, stakeClaimsTotalFiatValue)

            val accountTotalFiatValue = pricesPerAsset.values.sumOf { it }.floor(decimalPlaces = 5u)
            assertEquals(expectedTotalFiatValueOfAccount, accountTotalFiatValue)
        }
    }

    @Test
    fun `assert total fiat values of owned assets given an account with some tokens and stake claims`() {
        val expectedTotalFiatValueOfTokens = 870565.11.toDecimal192()
        val expectedTotalFiatValueOfLSUs = 0.0.toDecimal192()
        val expectedTotalFiatValueOfPoolUnits = 0.0.toDecimal192()
        val expectedTotalFiatValueOfStakeClaims = 3595.15252.toDecimal192()
        val expectedTotalFiatValueOfAccount = 874160.26252.toDecimal192()

        val accountsWithAssets = mockAccountsWithMockAssets
        val accountWithAssets = accountsWithAssets[1]

        testScope.runTest {
            val assetPricesForAccount = getFiatValueUseCase.forAccount(accountWithAssets, isRefreshing = false).getOrThrow()

            val pricesPerAsset: Map<Asset, Decimal192> = assetPricesForAccount.associate { assetPrice ->
                assetPrice.asset to assetPrice.price?.price.orZero()
            }

            val allTokensOfTheAccount = accountWithAssets.assets?.tokens
            val tokensTotalFiatValue = allTokensOfTheAccount?.sumOf {
                pricesPerAsset[it].orZero()
            }.orZero()
            assertEquals(expectedTotalFiatValueOfTokens, tokensTotalFiatValue)

            val allLSUsOfTheAccount = accountWithAssets.assets?.liquidStakeUnits
            val lsusTotalFiatValue = allLSUsOfTheAccount?.sumOf {
                pricesPerAsset[it].orZero()
            }.orZero()
            assertEquals(expectedTotalFiatValueOfLSUs, lsusTotalFiatValue)

            val allPoolUnitsOfTheAccount = accountWithAssets.assets?.poolUnits
            val poolUnitsTotalFiatValue = allPoolUnitsOfTheAccount?.sumOf {
                pricesPerAsset[it].orZero()
            }.orZero()
            assertEquals(expectedTotalFiatValueOfPoolUnits, poolUnitsTotalFiatValue)

            val allStakeClaimsOfTheAccount = accountWithAssets.assets?.stakeClaims
            val stakeClaimsTotalFiatValue = allStakeClaimsOfTheAccount?.sumOf {
                pricesPerAsset[it].orZero()
            }.orZero()
            assertEquals(expectedTotalFiatValueOfStakeClaims, stakeClaimsTotalFiatValue)

            val accountTotalFiatValue = pricesPerAsset.values.sumOf { it }
            assertEquals(expectedTotalFiatValueOfAccount, accountTotalFiatValue)
        }
    }

    @Test
    fun `assert the wallet total fiat value given two accounts with multiple assets`() {
        val expectedTotalFiatValueOfWallet = 111355847.11897.toDecimal192()

        val accountsWithAssets = mockAccountsWithMockAssets
        var actualTotalFiatValueOfWallet = 0.toDecimal192()

        testScope.runTest {
            accountsWithAssets.forEach { accountWithAssets ->
                val assetPricesForAccount = getFiatValueUseCase.forAccount(accountWithAssets, isRefreshing = false).getOrThrow()

                val pricesPerAsset: Map<Asset, Decimal192> = assetPricesForAccount.associate { assetPrice ->
                    assetPrice.asset to assetPrice.price?.price.orZero()
                }

                val accountTotalFiatValue = pricesPerAsset.values.sumOf { it }

                actualTotalFiatValueOfWallet += accountTotalFiatValue
            }

            assertEquals(expectedTotalFiatValueOfWallet, actualTotalFiatValueOfWallet.floor(decimalPlaces = 5u))
        }
    }

}



