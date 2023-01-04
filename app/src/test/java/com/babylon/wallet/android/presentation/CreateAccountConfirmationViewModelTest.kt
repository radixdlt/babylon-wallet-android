package com.babylon.wallet.android.presentation

import androidx.lifecycle.SavedStateHandle
import com.babylon.wallet.android.presentation.createaccount.CreateAccountConfirmationEvent
import com.babylon.wallet.android.presentation.createaccount.CreateAccountConfirmationViewModel
import com.babylon.wallet.android.presentation.navigation.Screen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class CreateAccountConfirmationViewModelTest {

    @get:Rule
    val coroutineRule = TestDispatcherRule()

    private val savedStateHandle = Mockito.mock(SavedStateHandle::class.java)

    @Test
    fun `given profile did not exist, when view model init, verify correct account state and go next`() = runTest {
        // given
        val accountId = "12kje20k"
        val accountName = "My main account"
        val event = mutableListOf<CreateAccountConfirmationEvent>()
        whenever(savedStateHandle.get<String>(Screen.ARG_ACCOUNT_ID)).thenReturn(accountId)
        whenever(savedStateHandle.get<String>(Screen.ARG_ACCOUNT_NAME)).thenReturn(accountName)
        whenever(savedStateHandle.get<Boolean>(Screen.ARG_HAS_PROFILE)).thenReturn(false)
        val viewModel = CreateAccountConfirmationViewModel(savedStateHandle)

        // when
        viewModel.goHomeClick()

        viewModel.oneOffEvent
            .onEach { event.add(it) }
            .launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        advanceUntilIdle()

        // then
        Assert.assertEquals(
            CreateAccountConfirmationViewModel.AccountConfirmationUiState(
                accountName = accountName,
                accountId = accountId
            ),
            viewModel.accountUiState
        )

        Assert.assertEquals(event.first(), CreateAccountConfirmationEvent.NavigateToHome)
    }

    @Test
    fun `given profile did exist, when view model init, verify correct account state and dismiss`() = runTest {
        // given
        val accountId = "12kje20k"
        val accountName = "My main account"
        val event = mutableListOf<CreateAccountConfirmationEvent>()
        whenever(savedStateHandle.get<String>(Screen.ARG_ACCOUNT_ID)).thenReturn(accountId)
        whenever(savedStateHandle.get<String>(Screen.ARG_ACCOUNT_NAME)).thenReturn(accountName)
        whenever(savedStateHandle.get<Boolean>(Screen.ARG_HAS_PROFILE)).thenReturn(true)
        val viewModel = CreateAccountConfirmationViewModel(savedStateHandle)

        // when
        viewModel.goHomeClick()

        viewModel.oneOffEvent
            .onEach { event.add(it) }
            .launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        advanceUntilIdle()

        // then
        Assert.assertEquals(
            CreateAccountConfirmationViewModel.AccountConfirmationUiState(
                accountName = accountName,
                accountId = accountId
            ),
            viewModel.accountUiState
        )

        Assert.assertEquals(event.first(), CreateAccountConfirmationEvent.FinishAccountCreation)
    }
}