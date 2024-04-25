package com.babylon.wallet.android.presentation

import com.babylon.wallet.android.data.dapp.IncomingRequestRepositoryImpl
import com.babylon.wallet.android.fakes.FakeProfileRepository
import com.babylon.wallet.android.presentation.dapp.unauthorized.accountonetime.OneTimeChooseAccountsViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Rule
import rdx.works.profile.domain.GetProfileUseCase

@OptIn(ExperimentalCoroutinesApi::class)
class ChooseAccountsViewModelTest {

    @OptIn(ExperimentalCoroutinesApi::class)
    @get:Rule
    val coroutineRule = TestDispatcherRule()

    private val profileRepository = FakeProfileRepository()
    private val getProfileUseCase = GetProfileUseCase(profileRepository)

    private val incomingRequestRepository = IncomingRequestRepositoryImpl()

    private lateinit var viewModel: OneTimeChooseAccountsViewModel

//    @Before
//    fun setup() = runTest {
//        incomingRequestRepository.add(accountsRequestAtLeast)
//
//        viewModel = OneTimeChooseAccountsViewModel(
//            savedStateHandle = SavedStateHandle(
//                mapOf(
//                    ARG_NUMBER_OF_ACCOUNTS to accountsRequestAtLeast.oneTimeAccountsRequestItem!!.numberOfValues.quantity,
//                    ARG_EXACT_ACCOUNT_COUNT to true
//                )
//            ),
//            getProfileUseCase = getProfileUseCase
//        )
//    }
//
//    @Test
//    fun `given a profile with 2 accounts, when Choose Accounts screen is launched, then it shows two profiles to select`() =
//        runTest {
//            advanceUntilIdle()
//            val state = viewModel.state.first()
//            assertTrue(state.availableAccountItems.size == 2)
//        }
//
//    @Test
//    fun `given a request for at least 2 accounts, when Choose Accounts screen is launched, then continue button is disabled`() =
//        runTest {
//            advanceUntilIdle()
//            val state = viewModel.state.first()
//            assertFalse(state.isContinueButtonEnabled)
//        }
//
//    @Test
//    fun `given a request for at least 2 accounts, when user selects one, then continue button is disabled`() =
//        runTest {
//            advanceUntilIdle()
//            viewModel.onAccountSelect(0)
//            val state = viewModel.state.first()
//            assertFalse(state.isContinueButtonEnabled)
//        }
//
//    @Test
//    fun `given a request for at least 2 accounts, when user selects two, then continue button is enabled`() =
//        runTest {
//            advanceUntilIdle()
//            viewModel.onAccountSelect(0)
//            viewModel.onAccountSelect(1)
//            val state = viewModel.state.first()
//            assertTrue(state.isContinueButtonEnabled)
//        }
//
//    @Test
//    fun `given a request for at least 2 accounts, when user selects two and unselect the last selected, then continue button is disabled`() =
//        runTest {
//            advanceUntilIdle()
//            viewModel.onAccountSelect(0)
//            viewModel.onAccountSelect(1)
//            viewModel.onAccountSelect(1)
//            val state = viewModel.state.first()
//            assertFalse(state.isContinueButtonEnabled)
//        }
//
//    @Test
//    fun `given a request for exactly 1 account, when selected one, then continue button is enabled`() =
//        runTest {
//            // given
//            incomingRequestRepository.add(accountsRequestExact)
//
//            viewModel = OneTimeChooseAccountsViewModel(
//                savedStateHandle = SavedStateHandle(
//                    mapOf(
//                        ARG_NUMBER_OF_ACCOUNTS to accountsRequestExact.oneTimeAccountsRequestItem!!.numberOfValues.quantity,
//                        ARG_EXACT_ACCOUNT_COUNT to true
//                    )
//                ),
//                getProfileUseCase = getProfileUseCase
//            )
//
//            advanceUntilIdle()
//
//            // when
//            viewModel.onAccountSelect(0)
//
//            // then
//            val state = viewModel.state.first()
//            assertTrue(state.isContinueButtonEnabled)
//        }
//
//    @Test
//    fun `given a request for exactly 2 account, when selecting one, then continue button is disabled`() =
//        runTest {
//            // given
//            incomingRequestRepository.add(accountsTwoRequestExact)
//
//            viewModel = OneTimeChooseAccountsViewModel(
//                savedStateHandle = SavedStateHandle(
//                    mapOf(
//                        ARG_NUMBER_OF_ACCOUNTS to accountsTwoRequestExact.oneTimeAccountsRequestItem!!.numberOfValues.quantity,
//                        ARG_EXACT_ACCOUNT_COUNT to true
//                    )
//                ),
//                getProfileUseCase = getProfileUseCase
//            )
//
//            advanceUntilIdle()
//
//            // when
//            viewModel.onAccountSelect(0)
//
//            // then
//            val state = viewModel.state.first()
//            assertFalse(state.isContinueButtonEnabled)
//        }
//
//    @Test
//    fun `given a request for exactly 2 account, when selecting two, then continue button is enabled`() =
//        runTest {
//            // given
//            incomingRequestRepository.add(accountsTwoRequestExact)
//            viewModel = OneTimeChooseAccountsViewModel(
//                savedStateHandle = SavedStateHandle(
//                    mapOf(
//                        ARG_NUMBER_OF_ACCOUNTS to accountsTwoRequestExact.oneTimeAccountsRequestItem!!.numberOfValues.quantity,
//                        ARG_EXACT_ACCOUNT_COUNT to true
//                    )
//                ),
//                getProfileUseCase = getProfileUseCase
//            )
//            advanceUntilIdle()
//
//            // when
//            viewModel.onAccountSelect(0)
//            viewModel.onAccountSelect(1)
//
//            // then
//            val state = viewModel.state.first()
//            assertTrue(state.isContinueButtonEnabled)
//        }

}
