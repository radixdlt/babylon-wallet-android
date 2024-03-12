package com.babylon.wallet.android.domain.usecases

import com.babylon.wallet.android.domain.model.assets.Asset
import com.babylon.wallet.android.domain.usecases.assets.GetFiatValueUseCase
import com.babylon.wallet.android.fakes.StateRepositoryFake
import com.babylon.wallet.android.fakes.TokenPriceRepositoryFake
import com.babylon.wallet.android.mockdata.mockAccountsWithMockAssets
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.math.RoundingMode

class GetFiatValueUseCaseTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private val tokenPriceRepositoryFake = TokenPriceRepositoryFake()
    private val getFiatValueUseCase = GetFiatValueUseCase(
        tokenPriceRepository = tokenPriceRepositoryFake,
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
        val expectedTotalFiatValueOfTokens = BigDecimal.valueOf(110415990.053).stripTrailingZeros()
        val expectedTotalFiatValueOfLSUs = BigDecimal.valueOf(1927.12059).stripTrailingZeros()
        val expectedTotalFiatValueOfPoolUnits = BigDecimal.valueOf(63769.68279).stripTrailingZeros()
        val expectedTotalFiatValueOfStakeClaims = BigDecimal.ZERO
        val expectedTotalFiatValueOfAccount = BigDecimal.valueOf(110481686.856)

        val accountsWithAssets = mockAccountsWithMockAssets
        val accountWithAssets = accountsWithAssets[0]

        testScope.runTest {
            val assetPricesForAccount = getFiatValueUseCase.forAccount(accountWithAssets).getOrThrow()

            val pricesPerAsset: Map<Asset, BigDecimal> = assetPricesForAccount.associate { assetPrice ->
                assetPrice.asset to (assetPrice.price ?: BigDecimal.ZERO)
            }

            val allTokensOfTheAccount = accountWithAssets.assets?.tokens
            val tokensTotalFiatValue = allTokensOfTheAccount?.sumOf {
                pricesPerAsset[it] ?: BigDecimal.ZERO
            } ?: BigDecimal.ZERO
            val actualTotalFiatValueOfTokens = tokensTotalFiatValue.setScale(3, RoundingMode.FLOOR).stripTrailingZeros()
            assertEquals(expectedTotalFiatValueOfTokens, actualTotalFiatValueOfTokens)

            val allLSUsOfTheAccount = accountWithAssets.assets?.liquidStakeUnits
            val lsusTotalFiatValue = allLSUsOfTheAccount?.sumOf {
                pricesPerAsset[it] ?: BigDecimal.ZERO
            } ?: BigDecimal.ZERO
            val actualTotalFiatValueOfLSUs = lsusTotalFiatValue.setScale(5, RoundingMode.CEILING).stripTrailingZeros()
            assertEquals(expectedTotalFiatValueOfLSUs, actualTotalFiatValueOfLSUs)

            val allPoolUnitsOfTheAccount = accountWithAssets.assets?.poolUnits
            val poolUnitsTotalFiatValue = allPoolUnitsOfTheAccount?.sumOf {
                pricesPerAsset[it] ?: BigDecimal.ZERO
            } ?: BigDecimal.ZERO
            val actualPoolUnitsTotalFiatValue = poolUnitsTotalFiatValue.setScale(5, RoundingMode.FLOOR)
            assertEquals(expectedTotalFiatValueOfPoolUnits, actualPoolUnitsTotalFiatValue)

            val allStakeClaimsOfTheAccount = accountWithAssets.assets?.stakeClaims
            val stakeClaimsTotalFiatValue = allStakeClaimsOfTheAccount?.sumOf {
                pricesPerAsset[it] ?: BigDecimal.ZERO
            } ?: BigDecimal.ZERO
            val actualStakeClaimsTotalFiatValue = stakeClaimsTotalFiatValue.stripTrailingZeros()
            assertEquals(expectedTotalFiatValueOfStakeClaims, actualStakeClaimsTotalFiatValue)

            val accountTotalFiatValue = pricesPerAsset.values.sumOf { it }.setScale(3, RoundingMode.FLOOR)
            assertEquals(expectedTotalFiatValueOfAccount, accountTotalFiatValue)
        }
    }

    @Test
    fun `assert total fiat values of owned assets given an account with some tokens and stake claims`() {
        val expectedTotalFiatValueOfTokens = BigDecimal.valueOf(870565.11).stripTrailingZeros()
        val expectedTotalFiatValueOfLSUs = BigDecimal.ZERO
        val expectedTotalFiatValueOfPoolUnits = BigDecimal.ZERO
        val expectedTotalFiatValueOfStakeClaims = BigDecimal.valueOf(3595.15252)
        val expectedTotalFiatValueOfAccount = BigDecimal.valueOf(874160.26252)

        val accountsWithAssets = mockAccountsWithMockAssets
        val accountWithAssets = accountsWithAssets[1]

        testScope.runTest {
            val assetPricesForAccount = getFiatValueUseCase.forAccount(accountWithAssets).getOrThrow()

            val pricesPerAsset: Map<Asset, BigDecimal> = assetPricesForAccount.associate { assetPrice ->
                assetPrice.asset to (assetPrice.price ?: BigDecimal.ZERO)
            }

            val allTokensOfTheAccount = accountWithAssets.assets?.tokens
            val tokensTotalFiatValue = allTokensOfTheAccount?.sumOf {
                pricesPerAsset[it] ?: BigDecimal.ZERO
            } ?: BigDecimal.ZERO
            val actualTotalFiatValueOfTokens = tokensTotalFiatValue.setScale(2, RoundingMode.FLOOR).stripTrailingZeros()
            assertEquals(expectedTotalFiatValueOfTokens, actualTotalFiatValueOfTokens)

            val allLSUsOfTheAccount = accountWithAssets.assets?.liquidStakeUnits
            val lsusTotalFiatValue = allLSUsOfTheAccount?.sumOf {
                pricesPerAsset[it] ?: BigDecimal.ZERO
            } ?: BigDecimal.ZERO
            val actualTotalFiatValueOfLSUs = lsusTotalFiatValue.stripTrailingZeros()
            assertEquals(expectedTotalFiatValueOfLSUs, actualTotalFiatValueOfLSUs)

            val allPoolUnitsOfTheAccount = accountWithAssets.assets?.poolUnits
            val poolUnitsTotalFiatValue = allPoolUnitsOfTheAccount?.sumOf {
                pricesPerAsset[it] ?: BigDecimal.ZERO
            } ?: BigDecimal.ZERO
            assertEquals(expectedTotalFiatValueOfPoolUnits, poolUnitsTotalFiatValue)

            val allStakeClaimsOfTheAccount = accountWithAssets.assets?.stakeClaims
            val stakeClaimsTotalFiatValue = allStakeClaimsOfTheAccount?.sumOf {
                pricesPerAsset[it] ?: BigDecimal.ZERO
            } ?: BigDecimal.ZERO
            val actualStakeClaimsTotalFiatValue = stakeClaimsTotalFiatValue.setScale(5, RoundingMode.CEILING)
            assertEquals(expectedTotalFiatValueOfStakeClaims, actualStakeClaimsTotalFiatValue)

            val accountTotalFiatValue = pricesPerAsset.values.sumOf { it }.setScale(5, RoundingMode.FLOOR)
            assertEquals(expectedTotalFiatValueOfAccount, accountTotalFiatValue)
        }
    }

    @Test
    fun `assert the wallet total fiat value given two accounts with multiple assets`() {
        val expectedTotalFiatValueOfWallet = BigDecimal.valueOf(111355847.119)

        val accountsWithAssets = mockAccountsWithMockAssets
        var actualTotalFiatValueOfWallet = BigDecimal.ZERO

        testScope.runTest {
            accountsWithAssets.forEach { accountWithAssets ->
                val assetPricesForAccount = getFiatValueUseCase.forAccount(accountWithAssets).getOrThrow()

                val pricesPerAsset: Map<Asset, BigDecimal> = assetPricesForAccount.associate { assetPrice ->
                    assetPrice.asset to (assetPrice.price ?: BigDecimal.ZERO)
                }

                val accountTotalFiatValue = pricesPerAsset.values.sumOf { it }.setScale(5, RoundingMode.FLOOR)

                actualTotalFiatValueOfWallet += accountTotalFiatValue
            }

            assertEquals(expectedTotalFiatValueOfWallet, actualTotalFiatValueOfWallet.setScale(3, RoundingMode.CEILING))
        }
    }

}



