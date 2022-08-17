package com.babylon.wallet.android

import android.content.ClipboardManager
import androidx.lifecycle.SavedStateHandle
import com.babylon.wallet.android.domain.MainViewRepository
import com.babylon.wallet.android.mockdata.mockAccountUiList
import com.babylon.wallet.android.presentation.account.AccountUiState
import com.babylon.wallet.android.presentation.account.AccountViewModel
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
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class AccountViewModelTest {

    @get:Rule
    val coroutineRule = TestDispatcherRule()

    private val mainViewRepository = Mockito.mock(MainViewRepository::class.java)

    private val clipboardManager = Mockito.mock(ClipboardManager::class.java)

    private val savedStateHandle = Mockito.mock(SavedStateHandle::class.java)

    @Test
    fun `when viewmodel init, verify loading displayed before loading account ui`() = runTest {
        // given
        whenever(savedStateHandle.get<String>(Screen.ARG_ACCOUNT_ID)).thenReturn(accountId)
        whenever(mainViewRepository.getAccountBasedOnId(any())).thenReturn(mockAccountUiList.first())
        val event = mutableListOf<AccountUiState>()

        // when
        val viewModel = AccountViewModel(mainViewRepository, clipboardManager, savedStateHandle)
        viewModel.accountUiState
            .onEach { event.add(it) }
            .launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

        advanceUntilIdle()

        // then
        Assert.assertEquals(event.first(), AccountUiState.Loading)
    }

    @Test
    fun `when viewmodel init, verify accountUi loaded after loading`() = runTest {
        // given
        whenever(savedStateHandle.get<String>(Screen.ARG_ACCOUNT_ID)).thenReturn(accountId)
        whenever(mainViewRepository.getAccountBasedOnId(any())).thenReturn(mockAccountUiList.first())
        val event = mutableListOf<AccountUiState>()

        // when
        val viewModel = AccountViewModel(mainViewRepository, clipboardManager, savedStateHandle)
        viewModel.accountUiState
            .onEach { event.add(it) }
            .launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

        advanceUntilIdle()

        // then
        Assert.assertEquals(event.last(), AccountUiState.Loaded(
            mockAccountUiList.first()
        ))
    }

    companion object {
        private val accountId = "1212"
    }
}