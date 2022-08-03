package com.babylon.wallet.android

import android.content.ClipData
import android.content.ClipboardManager
import com.babylon.wallet.android.domain.MainViewRepository
import com.babylon.wallet.android.presentation.wallet.WalletData
import com.babylon.wallet.android.presentation.wallet.AccountData
import com.babylon.wallet.android.presentation.wallet.WalletUiState
import com.babylon.wallet.android.presentation.wallet.WalletViewModel
import com.babylon.wallet.android.presentation.wallet.AccountUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class WalletViewModelTest {

    @get:Rule
    val coroutineRule = TestDispatcherRule()

    private val mainViewRepository = mock(MainViewRepository::class.java)

    private val clipboardManager = mock(ClipboardManager::class.java)

    private val walletData = WalletData(
        "$",
        "1000"
    )
    private val accountData = AccountData(
        "My main account",
        "10fewfewfwe00",
        "100",
        "$"
    )

    @Test
    fun `when view model init, verify initial value of wallet UI state is Loading`() = runTest {
        // given
        val event = mutableListOf<WalletUiState>()

        // when
        val viewModel = WalletViewModel(mainViewRepository, clipboardManager)
        viewModel.walletUiState
            .onEach { event.add(it) }
            .launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

        advanceUntilIdle()

        // then
        assertEquals(event.first(), WalletUiState.Loading)
    }

    @Test
    fun `when view model init, verify wallet Ui state content is loaded at the end`() = runTest {
        // given
        val event = mutableListOf<WalletUiState>()
        whenever(mainViewRepository.getWalletData()).thenReturn(flow {
            emit(walletData)
        })

        // when
        val viewModel = WalletViewModel(mainViewRepository, clipboardManager)
        viewModel.walletUiState
            .onEach { event.add(it) }
            .launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

        advanceUntilIdle()

        // then
        val lastEvent = event.last() as WalletUiState.Loaded
        assertEquals(lastEvent.walletData.currency, walletData.currency)
        assertEquals(lastEvent.walletData.amount, walletData.amount)
    }

    @Test
    fun `when view model init, verify initial value of account UI state is Loading`() = runTest {
        // given
        val event = mutableListOf<AccountUiState>()

        // when
        val viewModel = WalletViewModel(mainViewRepository, clipboardManager)
        viewModel.accountUiState
            .onEach { event.add(it) }
            .launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

        advanceUntilIdle()

        // then
        assertEquals(event.first(), AccountUiState.Loading)
    }

    @Test
    fun `when view model init, verify account Ui state content is loaded at the end`() = runTest {
        // given
        val event = mutableListOf<AccountUiState>()
        whenever(mainViewRepository.getAccountData()).thenReturn(flow {
            emit(accountData)
        })

        // when
        val viewModel = WalletViewModel(mainViewRepository, clipboardManager)
        viewModel.accountUiState
            .onEach { event.add(it) }
            .launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

        advanceUntilIdle()

        // then
        val lastEvent = event.last() as AccountUiState.Loaded
        assertEquals(lastEvent.accountData.accountName, accountData.accountName)
        assertEquals(lastEvent.accountData.accountHash, accountData.accountHash)
        assertEquals(lastEvent.accountData.accountValue, accountData.accountValue)
        assertEquals(lastEvent.accountData.accountCurrency, accountData.accountCurrency)
    }

    @Test
    fun `when onCopy called, verify content copied to clipboard manager`() {
        // given
        val hash = "somehash2123"
        val clipData = ClipData.newPlainText("accountHash", hash)
        val viewModel = WalletViewModel(mainViewRepository, clipboardManager)

        // when
        viewModel.onCopy(hash)

        // then
        verify(clipboardManager).setPrimaryClip(clipData)
    }
}
