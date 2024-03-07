package com.babylon.wallet.android.domain.usecases

import com.babylon.wallet.android.domain.model.assets.Asset
import com.babylon.wallet.android.domain.usecases.assets.GetFiatValueOfOwnedAssetsUseCase
import com.babylon.wallet.android.fakes.TokenPriceRepositoryFake
import com.babylon.wallet.android.mockdata.mockAccountsWithMockAssets
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.math.RoundingMode

class GetFiatValueOfOwnedAssetsUseCaseTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private val tokenPriceRepositoryFake = TokenPriceRepositoryFake()
    private val getFiatValueOfOwnedAssetsUseCase: GetFiatValueOfOwnedAssetsUseCase = GetFiatValueOfOwnedAssetsUseCase(tokenPriceRepositoryFake)

    @Test
    fun `assert that all the tokens of the given accounts have their prices`() {
        val accountsWithAssets = mockAccountsWithMockAssets

        testScope.runTest {
            val result = getFiatValueOfOwnedAssetsUseCase(accountsWithAssets = accountsWithAssets)

            result.map { mapOfAccountWithAssetsAndPrices ->
                val accountWithAssets = mapOfAccountWithAssetsAndPrices.key
                val tokensPrices = mapOfAccountWithAssetsAndPrices.value

                val ownedTokensNames = accountWithAssets.assets?.ownedTokens?.map {
                    it.resource.name
                } ?: emptyList()

                val tokensPricesNames = tokensPrices.map {
                    it.asset.resource.name
                }

                assertTrue(tokensPricesNames.containsAll(ownedTokensNames))
            }
        }
    }

    @Test
    fun `assert that a token that is not included in the token price server it has zero fiat value`() {
        val accountsWithAssets = mockAccountsWithMockAssets

        testScope.runTest {
            val result = getFiatValueOfOwnedAssetsUseCase(accountsWithAssets = accountsWithAssets)

            val assetsPricesList = result.values.first()
            val otherToken = assetsPricesList.find {
                it.asset.resource.name == "OtherToken"
            }
            assertNotNull(otherToken)
            assertTrue(otherToken!!.price == BigDecimal.ZERO)
        }
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
            val mapOfAccountsWithAssetsAndPrices = getFiatValueOfOwnedAssetsUseCase(accountsWithAssets = listOf(accountWithAssets))

            val assetPricesForAccount = mapOfAccountsWithAssetsAndPrices[accountWithAssets]!!
            val pricesPerAsset: Map<Asset, BigDecimal> = assetPricesForAccount.associate { assetPrice ->
                assetPrice.asset to assetPrice.price
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
            val actualPoolUnitsTotalFiatValue =  poolUnitsTotalFiatValue.setScale(5, RoundingMode.FLOOR)
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
            val mapOfAccountsWithAssetsAndPrices = getFiatValueOfOwnedAssetsUseCase(accountsWithAssets = listOf(accountWithAssets))

            val assetPricesForAccount = mapOfAccountsWithAssetsAndPrices[accountWithAssets]!!
            val pricesPerAsset: Map<Asset, BigDecimal> = assetPricesForAccount.associate { assetPrice ->
                assetPrice.asset to assetPrice.price
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
            val result = getFiatValueOfOwnedAssetsUseCase(accountsWithAssets = accountsWithAssets)

            accountsWithAssets.map { accountWithAssets ->
                val assetPricesForAccount = result[accountWithAssets]!!
                val pricesPerAsset: Map<Asset, BigDecimal> = assetPricesForAccount.associate { it.asset to it.price }

                val accountTotalFiatValue = pricesPerAsset.values.sumOf { it }.setScale(5, RoundingMode.FLOOR)

                actualTotalFiatValueOfWallet += accountTotalFiatValue
            }
            assertEquals(expectedTotalFiatValueOfWallet, actualTotalFiatValueOfWallet.setScale(3, RoundingMode.CEILING))
        }
    }

}



