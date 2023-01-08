package com.babylon.wallet.android.presentation.dapp

import com.babylon.wallet.android.data.dapp.DAppDetailsResponse
import com.babylon.wallet.android.data.dapp.DAppResult
import com.babylon.wallet.android.domain.common.Result
import com.babylon.wallet.android.domain.dapp.GetAccountsUseCase
import com.babylon.wallet.android.presentation.TestDispatcherRule
import com.babylon.wallet.android.presentation.dapp.account.ChooseAccountUiState
import com.babylon.wallet.android.presentation.dapp.account.ChooseAccountsViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever

/*@OptIn(ExperimentalCoroutinesApi::class)
class ChooseAccountsViewModelTest {

    @get:Rule
    val coroutineRule = TestDispatcherRule()

    private val getDAppAccountsUseCase = Mockito.mock(GetAccountsUseCase::class.java)

    private val accounts = listOf(
        SelectedAccountUiState(
            accountName = "Name",
            accountAddress = "df32f23f",
            accountCurrency = "$",
            accountValue = "1000",
            selected = false
        ),
        SelectedAccountUiState(
            accountName = "Name2",
            accountAddress = "1132vve3",
            accountCurrency = "$",
            accountValue = "2000",
            selected = false
        ),
        SelectedAccountUiState(
            accountName = "Name3",
            accountAddress = "kfer9k9if",
            accountCurrency = "$",
            accountValue = "3000",
            selected = false
        )
    )

    @Test
    fun `when view model init, verify view state is in default state`() = runTest {
        // given
        val dAppAccountsResult = DAppAccountsResult(
            accounts = accounts,
            dAppResult = DAppResult(
                dAppDetails = DAppDetailsResponse(
                    "url",
                    "Radaswap"
                ),
                accountAddresses = 1
            )
        )
        whenever(getDAppAccountsUseCase(
            any()
        )).thenReturn(Result.Success(dAppAccountsResult))

        // when
        val viewModel = ChooseAccountsViewModel(
            getDAppAccountsUseCase
        )

        // then
        Assert.assertEquals(ChooseAccountUiState(), viewModel.accountsState)
    }

    @Test
    fun `when view model init, verify all accounts unselected and continue button disabled`() = runTest {
        // given
        val dAppAccountsResult = DAppAccountsResult(
            accounts = accounts,
            dAppResult = DAppResult(
                dAppDetails = DAppDetailsResponse(
                    "url",
                    "Radaswap"
                ),
                accountAddresses = 1
            )
        )
        whenever(getDAppAccountsUseCase(
            any()
        )).thenReturn(Result.Success(dAppAccountsResult))

        // when
        val viewModel = ChooseAccountsViewModel(
            getDAppAccountsUseCase
        )

        advanceUntilIdle()

        // then
        Assert.assertEquals(
            ChooseAccountUiState(
                accounts = accounts,
                dAppDetails = dAppAccountsResult.dAppResult.dAppDetails,
                accountAddresses = 1,
                error = null,
                showProgress = false
            ),
            viewModel.accountsState
        )
    }

    @Test
    fun `given dApp not verified, when view model init, verify error shown`() = runTest {
        // given
        whenever(getDAppAccountsUseCase(
            any()
        )).thenReturn(Result.Error(Exception("Error")))

        // when
        val viewModel = ChooseAccountsViewModel(
            getDAppAccountsUseCase
        )

        advanceUntilIdle()

        // then
        Assert.assertEquals(
            ChooseAccountUiState(
                accounts = null,
                dAppDetails = null,
                accountAddresses = null,
                error = "Error",
                showProgress = false
            ),
            viewModel.accountsState
        )
    }

    @Test
    fun `given one account required, when one account selected, verify continue button enabled`() = runTest {
        // given
        val dAppAccountsResult = DAppAccountsResult(
            accounts = accounts,
            dAppResult = DAppResult(
                dAppDetails = DAppDetailsResponse(
                    "url",
                    "Radaswap"
                ),
                accountAddresses = 1
            )
        )
        val updatedAccounts = listOf(
            SelectedAccountUiState(
                accountName = "Name",
                accountAddress = "df32f23f",
                accountCurrency = "$",
                accountValue = "1000",
                selected = true
            ),
            SelectedAccountUiState(
                accountName = "Name2",
                accountAddress = "1132vve3",
                accountCurrency = "$",
                accountValue = "2000",
                selected = false
            ),
            SelectedAccountUiState(
                accountName = "Name3",
                accountAddress = "kfer9k9if",
                accountCurrency = "$",
                accountValue = "3000",
                selected = false
            )
        )
        whenever(getDAppAccountsUseCase(
            any()
        )).thenReturn(Result.Success(dAppAccountsResult))

        val selectedAccount = accounts.first()
        val viewModel = ChooseAccountsViewModel(
            getDAppAccountsUseCase
        )

        advanceUntilIdle()

        // when
        viewModel.onAccountSelect(selectedAccount)

        // then
        Assert.assertEquals(
            ChooseAccountUiState(
                accounts = updatedAccounts,
                dAppDetails = dAppAccountsResult.dAppResult.dAppDetails,
                accountAddresses = 1,
                continueButtonEnabled = true,
                error = null,
                showProgress = false
            ),
            viewModel.accountsState
        )
    }

    @Test
    fun `given two accounts required, when one account selected, verify continue button disabled`() = runTest {
        // given
        val dAppAccountsResult = DAppAccountsResult(
            accounts = accounts,
            dAppResult = DAppResult(
                dAppDetails = DAppDetailsResponse(
                    "url",
                    "Radaswap"
                ),
                accountAddresses = 2
            )
        )
        val updatedAccounts = listOf(
            SelectedAccountUiState(
                accountName = "Name",
                accountAddress = "df32f23f",
                accountCurrency = "$",
                accountValue = "1000",
                selected = true
            ),
            SelectedAccountUiState(
                accountName = "Name2",
                accountAddress = "1132vve3",
                accountCurrency = "$",
                accountValue = "2000",
                selected = false
            ),
            SelectedAccountUiState(
                accountName = "Name3",
                accountAddress = "kfer9k9if",
                accountCurrency = "$",
                accountValue = "3000",
                selected = false
            )
        )
        whenever(getDAppAccountsUseCase(
            any()
        )).thenReturn(Result.Success(dAppAccountsResult))

        val selectedAccount = accounts[0]
        val viewModel = ChooseAccountsViewModel(
            getDAppAccountsUseCase
        )

        advanceUntilIdle()

        // when
        viewModel.onAccountSelect(selectedAccount)

        // then
        Assert.assertEquals(
            ChooseAccountUiState(
                accounts = updatedAccounts,
                dAppDetails = dAppAccountsResult.dAppResult.dAppDetails,
                accountAddresses = 2,
                continueButtonEnabled = false,
                error = null,
                showProgress = false
            ),
            viewModel.accountsState
        )
    }

    @Test
    fun `given two accounts required, when two accounts selected, verify continue button enabled`() = runTest {
        // given
        val dAppAccountsResult = DAppAccountsResult(
            accounts = accounts,
            dAppResult = DAppResult(
                dAppDetails = DAppDetailsResponse(
                    "url",
                    "Radaswap"
                ),
                accountAddresses = 2
            )
        )
        val updatedAccounts = listOf(
            SelectedAccountUiState(
                accountName = "Name",
                accountAddress = "df32f23f",
                accountCurrency = "$",
                accountValue = "1000",
                selected = true
            ),
            SelectedAccountUiState(
                accountName = "Name2",
                accountAddress = "1132vve3",
                accountCurrency = "$",
                accountValue = "2000",
                selected = true
            ),
            SelectedAccountUiState(
                accountName = "Name3",
                accountAddress = "kfer9k9if",
                accountCurrency = "$",
                accountValue = "3000",
                selected = false
            )
        )
        whenever(getDAppAccountsUseCase(
            any()
        )).thenReturn(Result.Success(dAppAccountsResult))

        val selectedAccount = accounts[0]
        val selectedSecondAccount = accounts[1]
        val viewModel = ChooseAccountsViewModel(
            getDAppAccountsUseCase
        )

        advanceUntilIdle()

        // when
        viewModel.onAccountSelect(selectedAccount)
        viewModel.onAccountSelect(selectedSecondAccount)

        // then
        Assert.assertEquals(
            ChooseAccountUiState(
                accounts = updatedAccounts,
                dAppDetails = dAppAccountsResult.dAppResult.dAppDetails,
                accountAddresses = 2,
                continueButtonEnabled = true,
                error = null,
                showProgress = false
            ),
            viewModel.accountsState
        )
    }

    @Test
    fun `given max accounts selection 1, when trying to select more than 1, the 2nd account not selected`() = runTest {
        // given
        val dAppAccountsResult = DAppAccountsResult(
            accounts = accounts,
            dAppResult = DAppResult(
                dAppDetails = DAppDetailsResponse(
                    "url",
                    "Radaswap"
                ),
                accountAddresses = 1
            )
        )
        val updatedAccounts = listOf(
            SelectedAccountUiState(
                accountName = "Name",
                accountAddress = "df32f23f",
                accountCurrency = "$",
                accountValue = "1000",
                selected = true
            ),
            SelectedAccountUiState(
                accountName = "Name2",
                accountAddress = "1132vve3",
                accountCurrency = "$",
                accountValue = "2000",
                selected = false
            ),
            SelectedAccountUiState(
                accountName = "Name3",
                accountAddress = "kfer9k9if",
                accountCurrency = "$",
                accountValue = "3000",
                selected = false
            )
        )
        whenever(getDAppAccountsUseCase(
            any()
        )).thenReturn(Result.Success(dAppAccountsResult))

        val selectedAccount = accounts[0]
        val selectedSecondAccount = accounts[1]
        val viewModel = ChooseAccountsViewModel(
            getDAppAccountsUseCase
        )

        advanceUntilIdle()

        // when
        viewModel.onAccountSelect(selectedAccount)
        viewModel.onAccountSelect(selectedSecondAccount)

        // then
        Assert.assertEquals(
            ChooseAccountUiState(
                accounts = updatedAccounts,
                dAppDetails = dAppAccountsResult.dAppResult.dAppDetails,
                accountAddresses = 1,
                continueButtonEnabled = true,
                error = null,
                showProgress = false
            ),
            viewModel.accountsState
        )
    }
}*/
