package com.babylon.wallet.android.domain.usecases

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Test
import rdx.works.core.InstantGenerator
import rdx.works.core.preferences.PreferencesManager
import java.time.Period

@ExperimentalCoroutinesApi
class NPSSurveyUseCaseTest {

    @Test
    fun `given survey has never been shown and less than 10 transactions performed, verify no survey shown`() = runTest {
        val preferencesManager = mockk<PreferencesManager>()
        val addP2PLinkUseCase = NPSSurveyUseCase(preferencesManager)

        every { preferencesManager.lastNPSSurveyInstant } returns flowOf(null)
        every { preferencesManager.transactionCompleteCounter() } returns flowOf(0)

        addP2PLinkUseCase.npsSurveyState()

        val event = mutableListOf<NPSSurveyState>()

        addP2PLinkUseCase.npsSurveyState().onEach {
            event.add(it)
        }
            .launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

        advanceUntilIdle()

        Assert.assertEquals(event.first(), NPSSurveyState.InActive)
    }

    @Test
    fun `given survey has never been shown and 10 transactions performed, verify survey shown`() = runTest {
        val preferencesManager = mockk<PreferencesManager>()
        val addP2PLinkUseCase = NPSSurveyUseCase(preferencesManager)

        every { preferencesManager.lastNPSSurveyInstant } returns flowOf(null)
        every { preferencesManager.transactionCompleteCounter() } returns flowOf(10)

        addP2PLinkUseCase.npsSurveyState()

        val event = mutableListOf<NPSSurveyState>()

        addP2PLinkUseCase.npsSurveyState().onEach {
            event.add(it)
        }
            .launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

        advanceUntilIdle()

        Assert.assertEquals(event.first(), NPSSurveyState.Active)
    }

    @Test
    fun `given survey has already been shown and less than 90 days passed, verify survey not shown`() = runTest {
        val preferencesManager = mockk<PreferencesManager>()
        val addP2PLinkUseCase = NPSSurveyUseCase(preferencesManager)

        val instant = InstantGenerator().minus(Period.ofDays(12))
        every { preferencesManager.lastNPSSurveyInstant } returns flowOf(instant)
        every { preferencesManager.transactionCompleteCounter() } returns flowOf(10)

        addP2PLinkUseCase.npsSurveyState()

        val event = mutableListOf<NPSSurveyState>()

        addP2PLinkUseCase.npsSurveyState().onEach {
            event.add(it)
        }
            .launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

        advanceUntilIdle()

        Assert.assertEquals(event.first(), NPSSurveyState.InActive)
    }

    @Test
    fun `given survey has already been shown and 90 days passed, verify survey is shown`() = runTest {
        val preferencesManager = mockk<PreferencesManager>()
        val addP2PLinkUseCase = NPSSurveyUseCase(preferencesManager)

        val instant = InstantGenerator().minus(Period.ofDays(90))
        every { preferencesManager.lastNPSSurveyInstant } returns flowOf(instant)
        every { preferencesManager.transactionCompleteCounter() } returns flowOf(10)

        addP2PLinkUseCase.npsSurveyState()

        val event = mutableListOf<NPSSurveyState>()

        addP2PLinkUseCase.npsSurveyState().onEach {
            event.add(it)
        }
            .launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

        advanceUntilIdle()

        Assert.assertEquals(event.first(), NPSSurveyState.Active)
    }
}
