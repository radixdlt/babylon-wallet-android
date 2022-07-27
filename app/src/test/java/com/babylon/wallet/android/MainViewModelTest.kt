package com.babylon.wallet.android

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.assertEquals
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class MainViewModelTest {

    @get:Rule
    val coroutineRule = TestDispatcherRule()

    private val mainViewRepository = mock(MainViewRepository::class.java)

    @Test
    fun `given that view model init, verify initial value of UI state is Loading`() = runTest {
        val event = mutableListOf<UiState>()
        val viewModel = MainViewModel(mainViewRepository)
        viewModel.uiState
            .onEach { event.add(it) }
            .launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

        advanceUntilIdle()

        assertEquals(event.first(), UiState.Loading)
    }

    @Test
    fun `given that view model init, verify Ui state content is loaded at the end`() = runTest {
        val event = mutableListOf<UiState>()
        whenever(mainViewRepository.getWalletData()).thenReturn(flow {
            emit(WalletData("$", "1000"))
        })
        val viewModel = MainViewModel(mainViewRepository)
        viewModel.uiState
            .onEach { event.add(it) }
            .launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

        advanceUntilIdle()

        val lastEvent = event.last() as UiState.Loaded
        assertEquals(lastEvent.walletData.currency, "$")
        assertEquals(lastEvent.walletData.amount, "1000")
    }
}