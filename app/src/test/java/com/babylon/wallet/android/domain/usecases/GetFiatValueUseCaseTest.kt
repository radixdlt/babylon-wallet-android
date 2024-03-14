package com.babylon.wallet.android.domain.usecases

import com.babylon.wallet.android.domain.model.assets.Asset
import com.babylon.wallet.android.domain.usecases.assets.GetFiatValueUseCase
import com.babylon.wallet.android.fakes.FiatPriceRepositoryFake
import com.babylon.wallet.android.fakes.StateRepositoryFake
import com.babylon.wallet.android.mockdata.mockAccountsWithMockAssets
import io.mockk.mockk
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

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
            val assetPriceAddresses = getFiatValueUseCase.forAccount(accountWithAssets).getOrThrow().map {
                it.asset.resource.resourceAddress
            }.toSet()

            val ownedTokenAddresses = accountWithAssets.assets?.ownedTokens.orEmpty().map { it.resource.resourceAddress }.toSet()
            assertTrue(assetPriceAddresses.containsAll(ownedTokenAddresses))
        }
    }

    @Test
    fun `given a token when it is not included in the result of the prices then it does not have fiat value`() = testScope.runTest {
        val assetPrices = getFiatValueUseCase.forAccount(mockAccountsWithMockAssets[0]).getOrThrow()

        val assetPrice = assetPrices.find { it.asset.resource.name == "OtherToken" }
        assertNotNull(assetPrice?.asset)
        assertNull(assetPrice?.price)
    }

    @Test
    fun `assert total fiat values of owned assets given an account with some tokens, lsus, and pool units`() {
        val expectedTotalFiatValueOfTokens = 110415990.053
        val expectedTotalFiatValueOfLSUs = 1927.12059
        val expectedTotalFiatValueOfPoolUnits = 63769.68279
        val expectedTotalFiatValueOfStakeClaims = 0.0
        val expectedTotalFiatValueOfAccount = 110481686.856

        val accountsWithAssets = mockAccountsWithMockAssets
        val accountWithAssets = accountsWithAssets[0]

        testScope.runTest {
            val assetPricesForAccount = getFiatValueUseCase.forAccount(accountWithAssets).getOrThrow()

            val pricesPerAsset: Map<Asset, Double> = assetPricesForAccount.associate { assetPrice ->
                assetPrice.asset to (assetPrice.price?.price ?: 0.0)
            }

            val allTokensOfTheAccount = accountWithAssets.assets?.tokens
            val tokensTotalFiatValue = allTokensOfTheAccount?.sumOf {
                pricesPerAsset[it] ?: 0.0
            } ?: 0.0
            assertEquals(expectedTotalFiatValueOfTokens, tokensTotalFiatValue, 0.001)

            val allLSUsOfTheAccount = accountWithAssets.assets?.liquidStakeUnits
            val lsusTotalFiatValue = allLSUsOfTheAccount?.sumOf {
                pricesPerAsset[it] ?: 0.0
            } ?: 0.0
            assertEquals(expectedTotalFiatValueOfLSUs, lsusTotalFiatValue, 0.001)

            val allPoolUnitsOfTheAccount = accountWithAssets.assets?.poolUnits
            val poolUnitsTotalFiatValue = allPoolUnitsOfTheAccount?.sumOf {
                pricesPerAsset[it] ?: 0.0
            } ?: 0.0
            assertEquals(expectedTotalFiatValueOfPoolUnits, poolUnitsTotalFiatValue, 0.001)

            val allStakeClaimsOfTheAccount = accountWithAssets.assets?.stakeClaims
            val stakeClaimsTotalFiatValue = allStakeClaimsOfTheAccount?.sumOf {
                pricesPerAsset[it] ?: 0.0
            } ?: 0.0
            assertEquals(expectedTotalFiatValueOfStakeClaims, stakeClaimsTotalFiatValue, 0.001)

            val accountTotalFiatValue = pricesPerAsset.values.sumOf { it }
            assertEquals(expectedTotalFiatValueOfAccount, accountTotalFiatValue, 0.001)
        }
    }

    @Test
    fun `assert total fiat values of owned assets given an account with some tokens and stake claims`() {
        val expectedTotalFiatValueOfTokens = 870565.11
        val expectedTotalFiatValueOfLSUs = 0.0
        val expectedTotalFiatValueOfPoolUnits = 0.0
        val expectedTotalFiatValueOfStakeClaims = 3595.15252
        val expectedTotalFiatValueOfAccount = 874160.26252

        val accountsWithAssets = mockAccountsWithMockAssets
        val accountWithAssets = accountsWithAssets[1]

        testScope.runTest {
            val assetPricesForAccount = getFiatValueUseCase.forAccount(accountWithAssets).getOrThrow()

            val pricesPerAsset: Map<Asset, Double> = assetPricesForAccount.associate { assetPrice ->
                assetPrice.asset to (assetPrice.price?.price ?: 0.0)
            }

            val allTokensOfTheAccount = accountWithAssets.assets?.tokens
            val tokensTotalFiatValue = allTokensOfTheAccount?.sumOf {
                pricesPerAsset[it] ?: 0.0
            } ?: 0.0
            assertEquals(expectedTotalFiatValueOfTokens, tokensTotalFiatValue, 0.001)

            val allLSUsOfTheAccount = accountWithAssets.assets?.liquidStakeUnits
            val lsusTotalFiatValue = allLSUsOfTheAccount?.sumOf {
                pricesPerAsset[it] ?: 0.0
            } ?: 0.0
            assertEquals(expectedTotalFiatValueOfLSUs, lsusTotalFiatValue, 0.001)

            val allPoolUnitsOfTheAccount = accountWithAssets.assets?.poolUnits
            val poolUnitsTotalFiatValue = allPoolUnitsOfTheAccount?.sumOf {
                pricesPerAsset[it] ?: 0.0
            } ?: 0.0
            assertEquals(expectedTotalFiatValueOfPoolUnits, poolUnitsTotalFiatValue, 0.001)

            val allStakeClaimsOfTheAccount = accountWithAssets.assets?.stakeClaims
            val stakeClaimsTotalFiatValue = allStakeClaimsOfTheAccount?.sumOf {
                pricesPerAsset[it] ?: 0.0
            } ?: 0.0
            assertEquals(expectedTotalFiatValueOfStakeClaims, stakeClaimsTotalFiatValue, 0.001)

            val accountTotalFiatValue = pricesPerAsset.values.sumOf { it }
            assertEquals(expectedTotalFiatValueOfAccount, accountTotalFiatValue, 0.001)
        }
    }

    @Test
    fun `assert the wallet total fiat value given two accounts with multiple assets`() {
        val expectedTotalFiatValueOfWallet = 111355847.119

        val accountsWithAssets = mockAccountsWithMockAssets
        var actualTotalFiatValueOfWallet = 0.0

        testScope.runTest {
            accountsWithAssets.forEach { accountWithAssets ->
                val assetPricesForAccount = getFiatValueUseCase.forAccount(accountWithAssets).getOrThrow()

                val pricesPerAsset: Map<Asset, Double> = assetPricesForAccount.associate { assetPrice ->
                    assetPrice.asset to (assetPrice.price?.price ?: 0.0)
                }

                val accountTotalFiatValue = pricesPerAsset.values.sumOf { it }

                actualTotalFiatValueOfWallet += accountTotalFiatValue
            }

            assertEquals(expectedTotalFiatValueOfWallet, actualTotalFiatValueOfWallet, 0.001)
        }
    }

}



