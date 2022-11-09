package com.babylon.wallet.android.presentation.dapp

import com.babylon.wallet.android.data.dapp.DAppDetailsResponse
import com.babylon.wallet.android.data.dapp.DAppResult
import com.babylon.wallet.android.data.profile.model.Account
import com.babylon.wallet.android.data.profile.model.Address
import com.babylon.wallet.android.domain.dapp.DAppAccountsResult
import com.babylon.wallet.android.domain.dapp.RequestAccountsUseCase
import com.babylon.wallet.android.presentation.TestDispatcherRule
import com.babylon.wallet.android.presentation.dapp.account.ChooseAccountUiState
import com.babylon.wallet.android.presentation.dapp.account.ChooseDAppAccountViewModel
import com.babylon.wallet.android.presentation.dapp.account.SelectedAccountUiState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class ChooseDAppAccountViewModelTest {

    @get:Rule
    val coroutineRule = TestDispatcherRule()

    private val getDAppAccountsUseCase = Mockito.mock(RequestAccountsUseCase::class.java)

    private val accounts = listOf(
        SelectedAccountUiState(
            account = Account(
                name = "Name",
                address = Address("df32f23f"),
                value = "1000",
                currency = "$"
            ),
            selected = false
        ),
        SelectedAccountUiState(
            account = Account(
                name = "Name2",
                address = Address("1132vve3"),
                value = "2000",
                currency = "$"
            ),
            selected = false
        ),
        SelectedAccountUiState(
            account = Account(
                name = "Name3",
                address = Address("kfer9k9if"),
                value = "3000",
                currency = "$"
            ),
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
        whenever(getDAppAccountsUseCase.getAccountsResult()).thenReturn(dAppAccountsResult)

        // when
        val viewModel = ChooseDAppAccountViewModel(
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
        whenever(getDAppAccountsUseCase.getAccountsResult()).thenReturn(dAppAccountsResult)

        // when
        val viewModel = ChooseDAppAccountViewModel(
            getDAppAccountsUseCase
        )

        advanceUntilIdle()

        // then
        Assert.assertEquals(
            ChooseAccountUiState(
                accounts = accounts,
                dAppDetails = dAppAccountsResult.dAppResult?.dAppDetails,
                accountAddresses = 1,
                error = false,
                showProgress = false
            ),
            viewModel.accountsState
        )
    }

    @Test
    fun `given dApp not verified, when view model init, verify error shown`() = runTest {
        // given
        val dAppAccountsResult = DAppAccountsResult(
            accounts = accounts,
            dAppResult = null
        )
        whenever(getDAppAccountsUseCase.getAccountsResult()).thenReturn(dAppAccountsResult)

        // when
        val viewModel = ChooseDAppAccountViewModel(
            getDAppAccountsUseCase
        )

        advanceUntilIdle()

        // then
        Assert.assertEquals(
            ChooseAccountUiState(
                accounts = null,
                dAppDetails = null,
                accountAddresses = null,
                error = true,
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
                account = Account(
                    name = "Name",
                    address = Address("df32f23f"),
                    value = "1000",
                    currency = "$"
                ),
                selected = true
            ),
            SelectedAccountUiState(
                account = Account(
                    name = "Name2",
                    address = Address("1132vve3"),
                    value = "2000",
                    currency = "$"
                ),
                selected = false
            ),
            SelectedAccountUiState(
                account = Account(
                    name = "Name3",
                    address = Address("kfer9k9if"),
                    value = "3000",
                    currency = "$"
                ),
                selected = false
            )
        )
        whenever(getDAppAccountsUseCase.getAccountsResult()).thenReturn(dAppAccountsResult)

        val selectedAccount = accounts.first()
        val viewModel = ChooseDAppAccountViewModel(
            getDAppAccountsUseCase
        )

        advanceUntilIdle()

        // when
        viewModel.onAccountSelect(selectedAccount)

        // then
        Assert.assertEquals(
            ChooseAccountUiState(
                accounts = updatedAccounts,
                dAppDetails = dAppAccountsResult.dAppResult?.dAppDetails,
                accountAddresses = 1,
                continueButtonEnabled = true,
                error = false,
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
                account = Account(
                    name = "Name",
                    address = Address("df32f23f"),
                    value = "1000",
                    currency = "$"
                ),
                selected = true
            ),
            SelectedAccountUiState(
                account = Account(
                    name = "Name2",
                    address = Address("1132vve3"),
                    value = "2000",
                    currency = "$"
                ),
                selected = false
            ),
            SelectedAccountUiState(
                account = Account(
                    name = "Name3",
                    address = Address("kfer9k9if"),
                    value = "3000",
                    currency = "$"
                ),
                selected = false
            )
        )
        whenever(getDAppAccountsUseCase.getAccountsResult()).thenReturn(dAppAccountsResult)

        val selectedAccount = accounts[0]
        val viewModel = ChooseDAppAccountViewModel(
            getDAppAccountsUseCase
        )

        advanceUntilIdle()

        // when
        viewModel.onAccountSelect(selectedAccount)

        // then
        Assert.assertEquals(
            ChooseAccountUiState(
                accounts = updatedAccounts,
                dAppDetails = dAppAccountsResult.dAppResult?.dAppDetails,
                accountAddresses = 2,
                continueButtonEnabled = false,
                error = false,
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
                account = Account(
                    name = "Name",
                    address = Address("df32f23f"),
                    value = "1000",
                    currency = "$"
                ),
                selected = true
            ),
            SelectedAccountUiState(
                account = Account(
                    name = "Name2",
                    address = Address("1132vve3"),
                    value = "2000",
                    currency = "$"
                ),
                selected = true
            ),
            SelectedAccountUiState(
                account = Account(
                    name = "Name3",
                    address = Address("kfer9k9if"),
                    value = "3000",
                    currency = "$"
                ),
                selected = false
            )
        )
        whenever(getDAppAccountsUseCase.getAccountsResult()).thenReturn(dAppAccountsResult)

        val selectedAccount = accounts[0]
        val selectedSecondAccount = accounts[1]
        val viewModel = ChooseDAppAccountViewModel(
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
                dAppDetails = dAppAccountsResult.dAppResult?.dAppDetails,
                accountAddresses = 2,
                continueButtonEnabled = true,
                error = false,
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
                account = Account(
                    name = "Name",
                    address = Address("df32f23f"),
                    value = "1000",
                    currency = "$"
                ),
                selected = true
            ),
            SelectedAccountUiState(
                account = Account(
                    name = "Name2",
                    address = Address("1132vve3"),
                    value = "2000",
                    currency = "$"
                ),
                selected = false
            ),
            SelectedAccountUiState(
                account = Account(
                    name = "Name3",
                    address = Address("kfer9k9if"),
                    value = "3000",
                    currency = "$"
                ),
                selected = false
            )
        )
        whenever(getDAppAccountsUseCase.getAccountsResult()).thenReturn(dAppAccountsResult)

        val selectedAccount = accounts[0]
        val selectedSecondAccount = accounts[1]
        val viewModel = ChooseDAppAccountViewModel(
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
                dAppDetails = dAppAccountsResult.dAppResult?.dAppDetails,
                accountAddresses = 1,
                continueButtonEnabled = true,
                error = false,
                showProgress = false
            ),
            viewModel.accountsState
        )
    }
}
