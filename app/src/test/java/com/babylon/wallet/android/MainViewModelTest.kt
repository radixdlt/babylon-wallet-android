package com.babylon.wallet.android

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.assertEquals
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class MainViewModelTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    @Test
    fun `given that view model init, verify initial value of UI state is Loading`() = runTest {
        val event = mutableListOf<UiState>()
        val viewModel = MainViewModel()
        viewModel.uiState
            .onEach { event.add(it) }
            .launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

        advanceUntilIdle()

        assertEquals(event.first(), UiState.Loading)
    }

    @Test
    fun `given that view model init, verify Ui state content is loaded at the end`() = runTest {
        val event = mutableListOf<UiState>()
        val viewModel = MainViewModel()
        viewModel.uiState
            .onEach { event.add(it) }
            .launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

        advanceUntilIdle()

        val lastEvent = event.last() as UiState.Loaded
        assertEquals(lastEvent.walletData.currency, "$")
        assertEquals(lastEvent.walletData.amount, "1000")
    }
}