package com.babylon.wallet.android.presentation

import androidx.lifecycle.SavedStateHandle
import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import com.babylon.wallet.android.fakes.AccountRepositoryFake
import com.babylon.wallet.android.fakes.DAppMessengerFake
import com.babylon.wallet.android.mockdata.accountsRequest
import com.babylon.wallet.android.presentation.dapp.account.ChooseAccountsEvent
import com.babylon.wallet.android.presentation.dapp.account.ChooseAccountsViewModel
import com.babylon.wallet.android.presentation.navigation.Screen.Companion.ARG_INCOMING_REQUEST_ID
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChooseAccountsViewModelTest {

    @OptIn(ExperimentalCoroutinesApi::class)
    @get:Rule
    val coroutineRule = TestDispatcherRule()

    private val accountRepository = AccountRepositoryFake()

    private val dAppMessenger = DAppMessengerFake()
    private val incomingRequestRepository = IncomingRequestRepository()

    private lateinit var viewModel: ChooseAccountsViewModel

    @Before
    fun setup() = runTest {
        incomingRequestRepository.add(accountsRequest)

        viewModel = ChooseAccountsViewModel(
            savedStateHandle = SavedStateHandle(mapOf(ARG_INCOMING_REQUEST_ID to accountsRequest.requestId)),
            accountRepository = accountRepository,
            dAppMessenger = dAppMessenger,
            incomingRequestRepository = incomingRequestRepository
        )
    }

    @Test
    fun `given a profile with 2 accounts, when Choose Accounts screen is launched, then it shows two profiles to select`() =
        runTest {
            viewModel.state
            advanceUntilIdle()
            assertTrue(viewModel.state.availableAccountItems.size == 2)
        }

    @Test
    fun `given a request for at least 2 accounts, when Choose Accounts screen is launched, then continue button is disabled`() =
        runTest {
            viewModel.state
            advanceUntilIdle()
            assertFalse(viewModel.state.isContinueButtonEnabled)
        }

    @Test
    fun `given a request for at least 2 accounts, when user selects one, then continue button is disabled`() =
        runTest {
            viewModel.state
            advanceUntilIdle()
            viewModel.onAccountSelect(0)
            assertFalse(viewModel.state.isContinueButtonEnabled)
        }

    @Test
    fun `given a request for at least 2 accounts, when user selects two, then continue button is enabled`() =
        runTest {
            viewModel.state
            advanceUntilIdle()
            viewModel.onAccountSelect(0)
            viewModel.onAccountSelect(1)
            assertTrue(viewModel.state.isContinueButtonEnabled)
        }

    @Test
    fun `given a request for at least 2 accounts, when user selects two and unselect the last selected, then continue button is disabled`() =
        runTest {
            viewModel.state
            advanceUntilIdle()
            viewModel.onAccountSelect(0)
            viewModel.onAccountSelect(1)
            viewModel.onAccountSelect(1)
            assertFalse(viewModel.state.isContinueButtonEnabled)
        }

    @Test
    fun `when a account response is sent then navigate to completion screen`() =
        runTest {
            viewModel.sendAccountsResponse()
            advanceUntilIdle()
            assert(viewModel.oneOffEvent.first() is ChooseAccountsEvent.NavigateToCompletionScreen)
        }
}
