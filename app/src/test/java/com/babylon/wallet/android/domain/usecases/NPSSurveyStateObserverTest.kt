package com.babylon.wallet.android.domain.usecases

import com.babylon.wallet.android.NPSSurveyState
import com.babylon.wallet.android.NPSSurveyStateObserver
import com.babylon.wallet.android.fakes.FakePreferenceManager
import com.babylon.wallet.android.presentation.TestDispatcherRule
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
import rdx.works.core.InstantGenerator
import java.time.Period

@ExperimentalCoroutinesApi
class NPSSurveyStateObserverTest {

    @get:Rule
    val coroutineRule = TestDispatcherRule()

    private val preferencesManager = FakePreferenceManager()
    private lateinit var useCase: NPSSurveyStateObserver

    @Before
    fun setUp() {
        useCase = NPSSurveyStateObserver(preferencesManager)
    }

    @Test
    fun `given survey has never been shown and less than 10 transactions performed, verify no survey shown`() = runTest {
        val event = mutableListOf<NPSSurveyState>()

        useCase.npsSurveyState.onEach {
            event.add(it)
        }
            .launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

        advanceUntilIdle()
        Assert.assertTrue(event.isEmpty())
    }

    @Test
    fun `given survey has never been shown and 10 transactions performed, verify survey shown`() = runTest {
        val useCase = NPSSurveyStateObserver(preferencesManager)
        val event = mutableListOf<NPSSurveyState>()
        useCase.npsSurveyState.onEach {
            event.add(it)
        }
            .launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        repeat(10) {
            preferencesManager.incrementTransactionCompleteCounter()
        }
        advanceUntilIdle()

        Assert.assertEquals(NPSSurveyState.Active, event.last())
    }

    @Test
    fun `given survey has already been shown and less than 90 days passed, verify survey not shown`() = runTest {
        val useCase = NPSSurveyStateObserver(preferencesManager)

        val instant = InstantGenerator().minus(Period.ofDays(12))
        preferencesManager.updateLastNPSSurveyInstant(instant)

        val event = mutableListOf<NPSSurveyState>()

        useCase.npsSurveyState.onEach {
            event.add(it)
        }
            .launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        repeat(10) {
            preferencesManager.incrementTransactionCompleteCounter()
        }

        advanceUntilIdle()

        Assert.assertEquals(event.first(), NPSSurveyState.InActive)
    }

    @Test
    fun `given survey has already been shown and 90 days passed, verify survey is shown`() = runTest {
        val useCase = NPSSurveyStateObserver(preferencesManager)

        val event = mutableListOf<NPSSurveyState>()
        useCase.npsSurveyState.onEach {
            event.add(it)
        }
            .launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        repeat(10) {
            preferencesManager.incrementTransactionCompleteCounter()
        }

        advanceUntilIdle()

        Assert.assertEquals(NPSSurveyState.Active, event.last())
        preferencesManager.updateLastNPSSurveyInstant(InstantGenerator().minus(Period.ofDays(89)))
        preferencesManager.incrementTransactionCompleteCounter()
        advanceUntilIdle()
        Assert.assertEquals(NPSSurveyState.InActive, event.last())
        preferencesManager.updateLastNPSSurveyInstant(InstantGenerator().minus(Period.ofDays(90)))
        preferencesManager.incrementTransactionCompleteCounter()
        advanceUntilIdle()
        Assert.assertEquals(NPSSurveyState.Active, event.last())
        Assert.assertEquals(2, event.filterIsInstance<NPSSurveyState.Active>().size)
    }
}
