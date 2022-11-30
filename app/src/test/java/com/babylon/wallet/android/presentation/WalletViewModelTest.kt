package com.babylon.wallet.android.presentation

import android.content.ClipData
import android.content.ClipboardManager
import com.babylon.wallet.android.domain.MainViewRepository
import com.babylon.wallet.android.domain.Result
import com.babylon.wallet.android.domain.SampleDataProvider
import com.babylon.wallet.android.domain.usecase.wallet.RequestAccountResourcesUseCase
import com.babylon.wallet.android.presentation.wallet.WalletData
import com.babylon.wallet.android.presentation.wallet.WalletUiState
import com.babylon.wallet.android.presentation.wallet.WalletViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class WalletViewModelTest {

    @get:Rule
    val coroutineRule = TestDispatcherRule()

    private lateinit var vm: WalletViewModel
    private val mainViewRepository = mock(MainViewRepository::class.java)
    private val requestAccountsUseCase = mock(RequestAccountResourcesUseCase::class.java)
    private val clipboardManager = mock(ClipboardManager::class.java)

    private val walletData = WalletData(
        "$",
        "1000"
    )

    private val sampleData = SampleDataProvider().sampleAccountResource()

    @Before
    fun setUp() {
        vm = WalletViewModel(mainViewRepository, clipboardManager, requestAccountsUseCase)
    }

    @Test
    fun `when view model init, verify initial value of wallet UI state is Loading`() = runTest {
        // given
        val event = mutableListOf<WalletUiState>()

        // when
        vm.walletUiState
            .onEach { event.add(it) }
            .launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

        advanceUntilIdle()

        // then
        assertTrue(event.first().isLoading)
    }

    @Test
    fun `when view model init, verify wallet Ui state content is loaded at the end`() = runTest {
        // given
        val event = mutableListOf<WalletUiState>()
        whenever(mainViewRepository.getWallet()).thenReturn(walletData)
        whenever(requestAccountsUseCase.getAccountResources(any())).thenReturn(Result.Success(sampleData))
        // when
        vm.walletUiState
            .onEach { event.add(it) }
            .launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

        advanceUntilIdle()

        // then
        val lastEvent = event.last()
        assertEquals(lastEvent.wallet?.currency, walletData.currency)
        assertEquals(lastEvent.wallet?.amount, walletData.amount)
    }

    @Test
    fun `when view model init, verify initial value of account UI state is Loading`() = runTest {
        // given
        val event = mutableListOf<WalletUiState>()

        // when
        vm.walletUiState
            .onEach { event.add(it) }
            .launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

        advanceUntilIdle()

        // then
        assertTrue(event.first().isLoading)
    }

    @Test
    fun `when view model init, verify account Ui state content is loaded at the end`() = runTest {
        // given
        val event = mutableListOf<WalletUiState>()
        whenever(mainViewRepository.getWallet()).thenReturn(walletData)
        // when
        val viewModel = WalletViewModel(mainViewRepository, clipboardManager, requestAccountsUseCase)
        whenever(requestAccountsUseCase.getAccountResources(any())).thenReturn(Result.Success(sampleData))
        viewModel.walletUiState
            .onEach { event.add(it) }
            .launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

        advanceUntilIdle()

        // then
        assertTrue(!event.last().isLoading)
    }

    @Test
    fun `when onCopy called, verify content copied to clipboard manager`() {
        // given
        val hash = "somehash2123"
        val clipData = ClipData.newPlainText("accountHash", hash)
        vm.onCopyAccountAddress(hash)

        // then
        verify(clipboardManager).setPrimaryClip(clipData)
    }
}
