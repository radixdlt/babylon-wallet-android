package com.babylon.wallet.android.presentation

import androidx.lifecycle.SavedStateHandle
import com.babylon.wallet.android.presentation.navigation.Screen
import com.babylon.wallet.android.presentation.createaccount.CreateAccountConfirmationViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
    fun `when view model init, verify correct account state is shown`() = runTest {
        // given
        val accountId = "12kje20k"
        val accountName = "My main account"
        whenever(savedStateHandle.get<String>(Screen.ARG_ACCOUNT_ID)).thenReturn(accountId)
        whenever(savedStateHandle.get<String>(Screen.ARG_ACCOUNT_NAME)).thenReturn(accountName)
        val viewModel = CreateAccountConfirmationViewModel(savedStateHandle)

        // when
        advanceUntilIdle()

        viewModel.goHomeClick()

        // then
        Assert.assertEquals(
            CreateAccountConfirmationViewModel.AccountConfirmationUiState(
                dismiss = false,
                goNext = true,
                accountName = accountName,
                accountId = accountId
            ),
            viewModel.accountUiState
        )
    }
}