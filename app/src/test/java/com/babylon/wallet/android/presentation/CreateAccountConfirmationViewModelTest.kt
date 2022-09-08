package com.babylon.wallet.android.presentation

import androidx.lifecycle.SavedStateHandle
import com.babylon.wallet.android.presentation.navigation.Screen
import com.babylon.wallet.android.presentation.onboarding.CreateAccountConfirmationViewModel
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
    fun `when view model init, verify correct account state is shown`() = runTest {
        // given
        val accountId = "12kje20k"
        val accountName = "My main account"
        whenever(savedStateHandle.get<String>(Screen.ARG_ACCOUNT_ID)).thenReturn(accountId)
        whenever(savedStateHandle.get<String>(Screen.ARG_ACCOUNT_NAME)).thenReturn(accountName)
        val event = mutableListOf<Pair<String, String>>()
        val viewModel = CreateAccountConfirmationViewModel(savedStateHandle)

        // when
        viewModel.accountUiState
            .onEach { event.add(it) }
            .launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

        advanceUntilIdle()

        // then
        Assert.assertEquals(event.first(), Pair(accountName, accountId))
    }
}