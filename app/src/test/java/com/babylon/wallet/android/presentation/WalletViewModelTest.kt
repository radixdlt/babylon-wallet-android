package com.babylon.wallet.android.presentation

import android.content.ClipData
import android.content.ClipboardManager
import com.babylon.wallet.android.data.AccountDto.Companion.toUiModel
import com.babylon.wallet.android.domain.MainViewRepository
import com.babylon.wallet.android.mockdata.mockAccountDtoList
import com.babylon.wallet.android.mockdata.mockAccountUiList
import com.babylon.wallet.android.presentation.wallet.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
        whenever(mainViewRepository.getWallet()).thenReturn(walletData)
        whenever(mainViewRepository.getAccounts()).thenReturn(mockAccountUiList)

        // when
        val viewModel = WalletViewModel(mainViewRepository, clipboardManager)
        viewModel.walletUiState
            .onEach { event.add(it) }
            .launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

        advanceUntilIdle()

        // then
        val lastEvent = event.last() as WalletUiState.Loaded
        assertEquals(lastEvent.wallet.currency, walletData.currency)
        assertEquals(lastEvent.wallet.amount, walletData.amount)
    }

    @Test
    fun `when view model init, verify initial value of account UI state is Loading`() = runTest {
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
    fun `when view model init, verify account Ui state content is loaded at the end`() = runTest {
        // given
        val event = mutableListOf<WalletUiState>()
        whenever(mainViewRepository.getWallet()).thenReturn(walletData)
        whenever(mainViewRepository.getAccounts()).thenReturn(
            mockAccountDtoList.map { accountDto ->
                accountDto.toUiModel()
            })

        // when
        val viewModel = WalletViewModel(mainViewRepository, clipboardManager)
        viewModel.walletUiState
            .onEach { event.add(it) }
            .launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

        advanceUntilIdle()

        // then
        val lastEvent = event.last() as WalletUiState.Loaded
        assertEquals(lastEvent.accounts[0].name, mockAccountUiList[0].name)
        assertEquals(lastEvent.accounts[0].hash, mockAccountUiList[0].hash)
        assertEquals(lastEvent.accounts[0].amount, mockAccountUiList[0].amount)
        assertEquals(lastEvent.accounts[0].currencySymbol, mockAccountUiList[0].currencySymbol)
    }

    @Test
    fun `when onCopy called, verify content copied to clipboard manager`() {
        // given
        val hash = "somehash2123"
        val clipData = ClipData.newPlainText("accountHash", hash)
        val viewModel = WalletViewModel(mainViewRepository, clipboardManager)

        // when
        viewModel.onCopyAccountAddress(hash)

        // then
        verify(clipboardManager).setPrimaryClip(clipData)
    }
}
