package com.babylon.wallet.android.presentation

import androidx.lifecycle.SavedStateHandle
import com.babylon.wallet.android.data.dapp.IncomingRequestRepositoryImpl
import com.babylon.wallet.android.fakes.AccountRepositoryFake
import com.babylon.wallet.android.fakes.DAppMessengerFake
import com.babylon.wallet.android.fakes.DappMetadataRepositoryFake
import com.babylon.wallet.android.mockdata.accountsRequestAtLeast
import com.babylon.wallet.android.mockdata.accountsRequestExact
import com.babylon.wallet.android.mockdata.accountsTwoRequestExact
import com.babylon.wallet.android.presentation.dapp.accountonetime.ARG_REQUEST_ID
import com.babylon.wallet.android.presentation.dapp.accountonetime.OneTimeChooseAccountsEvent
import com.babylon.wallet.android.presentation.dapp.accountonetime.OneTimeChooseAccountsViewModel
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
    private val dappMetadataRepository = DappMetadataRepositoryFake()

    private val dAppMessenger = DAppMessengerFake()
    private val incomingRequestRepository = IncomingRequestRepositoryImpl()

    private lateinit var viewModel: OneTimeChooseAccountsViewModel

    @Before
    fun setup() = runTest {
        incomingRequestRepository.add(accountsRequestAtLeast)

        viewModel = OneTimeChooseAccountsViewModel(
            savedStateHandle = SavedStateHandle(mapOf(ARG_REQUEST_ID to accountsRequestAtLeast.requestId)),
            accountRepository = accountRepository,
            dAppMessenger = dAppMessenger,
            incomingRequestRepository = incomingRequestRepository,
            dappMetadataRepository = dappMetadataRepository
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
    fun `given a request for exactly 1 account, when selected one, then continue button is enabled`() =
        runTest {
            // given
            incomingRequestRepository.add(accountsRequestExact)

            viewModel = OneTimeChooseAccountsViewModel(
                savedStateHandle = SavedStateHandle(mapOf(ARG_REQUEST_ID to accountsRequestExact.requestId)),
                accountRepository = accountRepository,
                dAppMessenger = dAppMessenger,
                incomingRequestRepository = incomingRequestRepository,
                dappMetadataRepository = dappMetadataRepository
            )

            viewModel.state
            advanceUntilIdle()

            // when
            viewModel.onAccountSelect(0)

            // then
            assertTrue(viewModel.state.isContinueButtonEnabled)
        }

    @Test
    fun `given a request for exactly 2 account, when selecting one, then continue button is disabled`() =
        runTest {
            // given
            incomingRequestRepository.add(accountsTwoRequestExact)

            viewModel = OneTimeChooseAccountsViewModel(
                savedStateHandle = SavedStateHandle(mapOf(ARG_REQUEST_ID to accountsTwoRequestExact.requestId)),
                accountRepository = accountRepository,
                dAppMessenger = dAppMessenger,
                incomingRequestRepository = incomingRequestRepository,
                dappMetadataRepository = dappMetadataRepository
            )

            viewModel.state
            advanceUntilIdle()

            // when
            viewModel.onAccountSelect(0)

            // then
            assertFalse(viewModel.state.isContinueButtonEnabled)
        }

    @Test
    fun `given a request for exactly 2 account, when selecting two, then continue button is enabled`() =
        runTest {
            // given
            incomingRequestRepository.add(accountsTwoRequestExact)

            viewModel = OneTimeChooseAccountsViewModel(
                savedStateHandle = SavedStateHandle(mapOf(ARG_REQUEST_ID to accountsTwoRequestExact.requestId)),
                accountRepository = accountRepository,
                dAppMessenger = dAppMessenger,
                incomingRequestRepository = incomingRequestRepository,
                dappMetadataRepository = dappMetadataRepository
            )

            viewModel.state
            advanceUntilIdle()

            // when
            viewModel.onAccountSelect(0)
            viewModel.onAccountSelect(1)

            // then
            assertTrue(viewModel.state.isContinueButtonEnabled)
        }

    @Test
    fun `when a account response is sent then navigate to completion screen`() =
        runTest {
            viewModel.sendAccountsResponse()
            advanceUntilIdle()
            assert(viewModel.oneOffEvent.first() is OneTimeChooseAccountsEvent.NavigateToCompletionScreen)
        }
}
