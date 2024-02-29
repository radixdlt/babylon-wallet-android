package com.babylon.wallet.android.domain.usecases

import com.babylon.wallet.android.domain.usecases.assets.GetFiatValueOfOwnedAssetsUseCase
import com.babylon.wallet.android.fakes.TokenPriceRepositoryFake
import com.babylon.wallet.android.mockdata.mockAccountsWithMockAssets
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

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

}



