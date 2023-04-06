package com.babylon.wallet.android.presentation

import androidx.lifecycle.SavedStateHandle
import com.babylon.wallet.android.mockdata.account
import com.babylon.wallet.android.mockdata.profile
import com.babylon.wallet.android.presentation.createaccount.ARG_ACCOUNT_ID
import com.babylon.wallet.android.presentation.createaccount.ARG_REQUEST_SOURCE
import com.babylon.wallet.android.presentation.createaccount.CreateAccountConfirmationEvent
import com.babylon.wallet.android.presentation.createaccount.CreateAccountConfirmationViewModel
import com.babylon.wallet.android.presentation.createaccount.CreateAccountRequestSource
import com.babylon.wallet.android.presentation.navigation.Screen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever
import rdx.works.profile.domain.GetProfileUseCase

@ExperimentalCoroutinesApi
class CreateAccountConfirmationViewModelTest : BaseViewModelTest<CreateAccountConfirmationViewModel>() {

    private val savedStateHandle = mock(SavedStateHandle::class.java)
    private val getProfileUseCase = mock(GetProfileUseCase::class.java)
    private val accountId = "fj3489fj348f"
    private val accountName = "My main account"

    @Before
    override fun setUp() = runTest {
        super.setUp()
        whenever(savedStateHandle.get<String>(ARG_ACCOUNT_ID)).thenReturn(accountId)
        whenever(savedStateHandle.get<String>(Screen.ARG_ACCOUNT_NAME)).thenReturn(accountName)
        whenever(savedStateHandle.get<Boolean>(Screen.ARG_HAS_PROFILE)).thenReturn(false)
        whenever(getProfileUseCase()).thenReturn(
            flowOf(profile(accounts = listOf(account(address = accountId, name = accountName))))
        )
    }

    @Test
    fun `given profile did not exist, when view model init, verify correct account state and go next`() = runTest {
        // given
        whenever(savedStateHandle.get<CreateAccountRequestSource>(ARG_REQUEST_SOURCE)).thenReturn(
            CreateAccountRequestSource.FirstTime
        )
        val viewModel = vm.value
        val event = mutableListOf<CreateAccountConfirmationEvent>()

        // when
        viewModel.accountConfirmed()

        viewModel.oneOffEvent
            .onEach { event.add(it) }
            .launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        advanceUntilIdle()

        // then
        Assert.assertEquals(
            CreateAccountConfirmationViewModel.AccountConfirmationUiState(
                accountName = accountName,
                accountAddress = accountId,
                appearanceId = 1
            ),
            viewModel.state.value
        )

        Assert.assertEquals(event.first(), CreateAccountConfirmationEvent.NavigateToHome)
    }

    @Test
    fun `given profile did exist, when view model init, verify correct account state and dismiss`() = runTest {
        whenever(savedStateHandle.get<CreateAccountRequestSource>(ARG_REQUEST_SOURCE)).thenReturn(
            CreateAccountRequestSource.AccountsList
        )
        val event = mutableListOf<CreateAccountConfirmationEvent>()
        val viewModel = vm.value
        viewModel.accountConfirmed()

        viewModel.oneOffEvent
            .onEach { event.add(it) }
            .launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        advanceUntilIdle()

        // then
        Assert.assertEquals(
            CreateAccountConfirmationViewModel.AccountConfirmationUiState(
                accountName = accountName,
                accountAddress = accountId,
                appearanceId = 1
            ),
            viewModel.state.value
        )

        Assert.assertEquals(event.first(), CreateAccountConfirmationEvent.FinishAccountCreation)
    }

    override fun initVM(): CreateAccountConfirmationViewModel {
        return CreateAccountConfirmationViewModel(getProfileUseCase, savedStateHandle)
    }
}
