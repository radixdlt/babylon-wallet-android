package com.babylon.wallet.android.presentation

import com.babylon.wallet.android.data.DataStoreManager
import com.babylon.wallet.android.presentation.onboarding.OnboardingViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito
import org.mockito.kotlin.verify

@ExperimentalCoroutinesApi
class OnboardingViewModelTest {

    @get:Rule
    val coroutineRule = TestDispatcherRule()

    private val dataStoreManager = Mockito.mock(DataStoreManager::class.java)

    @Test
    fun `when set to show onboarding verify shown`() = runTest {
        // given
        val viewModel = OnboardingViewModel(UnconfinedTestDispatcher(testScheduler), dataStoreManager)

        // when
        viewModel.setShowOnboarding(true)

        // then
        verify(dataStoreManager).setShowOnboarding(true)
    }

    @Test
    fun `when set to not show onboarding, verify not shown`() = runTest {
        // given
        val viewModel = OnboardingViewModel(UnconfinedTestDispatcher(testScheduler), dataStoreManager)

        // when
        viewModel.setShowOnboarding(false)

        // then
        verify(dataStoreManager).setShowOnboarding(false)
    }
}