package com.babylon.wallet.android.presentation

import android.content.ClipboardManager
import androidx.lifecycle.SavedStateHandle
import com.babylon.wallet.android.domain.Result
import com.babylon.wallet.android.domain.SampleDataProvider
import com.babylon.wallet.android.domain.usecase.wallet.RequestAccountResourcesUseCase
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
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class AccountViewModelTest {

    @get:Rule
    val coroutineRule = TestDispatcherRule()

    private lateinit var vm: AccountViewModel

    private val requestAccountsUseCase = Mockito.mock(RequestAccountResourcesUseCase::class.java)

    private val clipboardManager = Mockito.mock(ClipboardManager::class.java)

    private val savedStateHandle = Mockito.mock(SavedStateHandle::class.java)

    private val sampleData = SampleDataProvider().sampleAccountResource()

    @Before
    fun setUp() = runTest {
        whenever(savedStateHandle.get<String>(Screen.ARG_ACCOUNT_ID)).thenReturn(accountId)
        whenever(requestAccountsUseCase.getAccountResources(any())).thenReturn(Result.Success(sampleData))
    }

    @Test
    fun `when viewmodel init, verify loading displayed before loading account ui`() = runTest {
        // given
        val event = mutableListOf<AccountUiState>()
        vm = AccountViewModel(requestAccountsUseCase, clipboardManager, savedStateHandle)
        vm.accountUiState
            .onEach { event.add(it) }
            .launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

        advanceUntilIdle()

        // then
        Assert.assertEquals(event.first().isLoading, true)
    }

    @Test
    fun `when viewmodel init, verify accountUi loaded after loading`() = runTest {
        // given
        val event = mutableListOf<AccountUiState>()
        vm = AccountViewModel(requestAccountsUseCase, clipboardManager, savedStateHandle)
        vm.accountUiState
            .onEach { event.add(it) }
            .launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

        advanceUntilIdle()

        // then
        with(event.last()) {
            assert(!this.isLoading)
            assert(accountAddress == sampleData.address)
            assert(xrdToken != null)
            assert(sampleData.fungibleTokens.size == 3)
        }
    }

    companion object {
        private val accountId = "1212"
    }
}
